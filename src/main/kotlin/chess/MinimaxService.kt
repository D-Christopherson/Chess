package chess

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Service
@OptIn(ExperimentalUnsignedTypes::class)
class MinimaxService(
    private val bitBoardService: BitBoardService
) {
    companion object {
        val log = LoggerFactory.getLogger(MinimaxService::class.java)
        val movesInGame = mutableListOf<String>()
        var moveGenerationTime = 0L
        var makingMoveTime = 0L
        var wallClockTime = 0L
        var isInCheckTime = 0L
        var evaluationTime = 0L
        var moves = 0L
        var transpositionHits = 0L
        var transpositionBestMoves = 0L
        const val zobristTableSize = 0b1_11111111_11111111_11111111
    }

    fun evaluate(board: BoardState, depth: Int): String {

        val killerMoves = Array<Move?>(100) {null}
        val zobristHashesOfGame = HashMap<ULong, Int>()

        val transpositionTable = Array<TranspositionEntry?>(zobristTableSize + 1) { null }
        var result: Pair<Move?, Int> = Pair(Move('a', 1, 0UL, 0UL), 0)
        while (result.first != null) {
            val start = System.currentTimeMillis()
            var i = 0
            while (true) {
                result =
                    this.minimax(
                        board,
                        i,
                        Int.MIN_VALUE,
                        Int.MAX_VALUE,
                        board.sideToPlay,
                        killerMoves,
                        transpositionTable,
                        zobristHashesOfGame
                    )
                wallClockTime += System.currentTimeMillis() - start
                if (System.currentTimeMillis() - start < 100 && Math.abs(result.second) <= 200 && i < 20 || i < depth) {
                    i++
                } else {
                    break
                }
            }
            Arrays.setAll(transpositionTable) { null }
            if (result.first == null) {
                return "checkmate"
            }

            this.bitBoardService.makeMove(board, result.first!!)

            val repetition = zobristHashesOfGame[board.getZobrastHash()]
            if (repetition != null) {
                zobristHashesOfGame[board.getZobrastHash()] = zobristHashesOfGame[board.getZobrastHash()]!! + 1
            } else {
                zobristHashesOfGame[board.getZobrastHash()] = 1
            }
            this.bitBoardService.printBoard(board)
            println()
            movesInGame.add(
                this.bitBoardService.ulongToSquare(result.first!!.sourceSquare)
                        + this.bitBoardService.ulongToSquare(result.first!!.targetSquare)
                        + if (result.first!!.promotionPiece != null) "=" + result.first!!.promotionPiece else ""
            )
            movesInGame.forEach { print("$it ") }
            println()
            println(result.second)
            println("Statistics: depth: $i moveGeneration: $moveGenerationTime makingMove: $makingMoveTime wallClock: $wallClockTime isInCheck: $isInCheckTime evaluationTime: $evaluationTime moves: $moves transpositionHits: $transpositionHits transpositionBestMoves: $transpositionBestMoves")
        }
        return this.bitBoardService.ulongToSquare(result.first!!.sourceSquare) +
                this.bitBoardService.ulongToSquare(result.first!!.targetSquare)
    }

    fun minimax(
        board: BoardState,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingPlayer: Boolean,
        killerMoves: Array<Move?>,
        transpositionTable: Array<TranspositionEntry?>,
        zobristHashesOfGame: MutableMap<ULong, Int>
    ): Pair<Move?, Int> {
        val threeFoldCount = zobristHashesOfGame[board.getZobrastHash()]
        if (threeFoldCount != null && threeFoldCount >= 2) {
            return Pair(null, 0)
        }

        val entry = transpositionTable[(board.getZobrastHash() and zobristTableSize.toULong()).toInt()]
        if (entry != null && entry.zobristHash == board.getZobrastHash() && entry.depth >= depth && !entry.alphaCutoff && !entry.betaCutoff) {
            transpositionHits++
            return Pair(entry.bestMove, entry.evaluation)
        }

        if (depth <= 0) {
            return Pair(null, this.bitBoardService.evaluatePosition(board))
        }

        if (board.fiftyMoveRuleCounter >= 50) {
            return Pair(null, 0)
        }

        if (maximizingPlayer) {
            var value = Int.MIN_VALUE
            val moves = this.bitBoardService.generateMoves(board, killerMoves, depth, transpositionTable)
            var newAlpha = alpha
            var bestMove: Move? = null
            run search@{
                moves.forEach { move ->
                    val undo = this.bitBoardService.makeMove(board, move)
                    if (this.bitBoardService.isInCheck(board)) {
                        this.bitBoardService.undoMove(board, undo)
                        return@forEach
                    }
                    val repetitions = zobristHashesOfGame[board.getZobrastHash()]
                    var newEntry = false

                    if (repetitions != null) {
                        zobristHashesOfGame[board.getZobrastHash()] = repetitions + 1
                    } else {
                        newEntry = true
                        zobristHashesOfGame[board.getZobrastHash()] = 1
                    }

                    val result = this.minimax(
                        board,
                        depth - 1,
                        newAlpha,
                        beta,
                        false,
                        killerMoves,
                        transpositionTable,
                        zobristHashesOfGame
                    )

                    if (newEntry) {
                        zobristHashesOfGame.remove(board.getZobrastHash())
                    } else {
                        zobristHashesOfGame[board.getZobrastHash()] = repetitions!!
                    }

                    this.bitBoardService.undoMove(board, undo)
                    if (result.second > value || bestMove == null) {
                        value = result.second
                        bestMove = move
                    }
                    newAlpha = max(newAlpha, value)
                    if (value > beta) {
                        killerMoves[depth] = move
                        return@search
                    }
                }
            }
            if ((entry == null || entry.depth <= depth) && bestMove != null) {
                transpositionTable[(board.getZobrastHash() and zobristTableSize.toULong()).toInt()] =
                    TranspositionEntry(board.getZobrastHash(), value, depth, false, value > beta, bestMove)
            }
            board.updateSideToPlay(!board.sideToPlay)
            if (bestMove == null && !this.bitBoardService.isInCheck(board)) {
                board.updateSideToPlay(!board.sideToPlay)
                return Pair(bestMove, 0)
            }
            board.updateSideToPlay(!board.sideToPlay)
            return Pair(bestMove, value)
        } else {
            var value = Int.MAX_VALUE
            val moves = this.bitBoardService.generateMoves(board, killerMoves, depth, transpositionTable)
            var newBeta = beta
            var bestMove: Move? = null
            run search@{
                moves.forEach { move ->
                    val undo = this.bitBoardService.makeMove(board, move)
                    if (this.bitBoardService.isInCheck(board)) {
                        this.bitBoardService.undoMove(board, undo)
                        return@forEach
                    }

                    val repetitions = zobristHashesOfGame[board.getZobrastHash()]
                    var newEntry = false
                    if (repetitions != null) {
                        zobristHashesOfGame[board.getZobrastHash()] = repetitions + 1
                    } else {
                        newEntry = true
                        zobristHashesOfGame[board.getZobrastHash()] = 1
                    }

                    val result = this.minimax(
                        board,
                        depth - 1,
                        alpha,
                        newBeta,
                        true,
                        killerMoves,
                        transpositionTable,
                        zobristHashesOfGame
                    )

                    if (newEntry) {
                        zobristHashesOfGame.remove(board.getZobrastHash())
                    } else {
                        zobristHashesOfGame[board.getZobrastHash()] = repetitions!!
                    }

                    this.bitBoardService.undoMove(board, undo)
                    if (result.second < value || bestMove == null) {
                        value = result.second
                        bestMove = move
                    }
                    newBeta = min(newBeta, value)
                    if (value < alpha) {
                        killerMoves[depth] = move
                        return@search
                    }
                }
            }
            if ((entry == null || entry.depth <= depth) && bestMove != null) {
                transpositionTable[(board.getZobrastHash() and zobristTableSize.toULong()).toInt()] =
                    TranspositionEntry(board.getZobrastHash(), value, depth, value < alpha, false, bestMove)
            }
            board.updateSideToPlay(!board.sideToPlay)
            if (bestMove == null && !this.bitBoardService.isInCheck(board)) {
                board.updateSideToPlay(!board.sideToPlay)
                return Pair(bestMove, 0)
            }
            board.updateSideToPlay(!board.sideToPlay)
            return Pair(bestMove, value)
        }
    }
}

data class TranspositionEntry(
    val zobristHash: ULong,
    val evaluation: Int,
    val depth: Int,
    val alphaCutoff: Boolean,
    val betaCutoff: Boolean,
    val bestMove: Move?
)