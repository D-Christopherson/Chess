package chess

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
        // These stats won't be meaningful when deployed, but I use them regularly locally to make sure I don't
        // introduce a change that significantly slows the engine down.
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

    fun evaluate(board: BoardState, depth: Int): EvaluateResult {

        val killerMoves = Array<Move?>(100) { null }
        val zobristHashesOfGame = HashMap<ULong, Int>()

        val transpositionTable = Array<TranspositionEntry?>(zobristTableSize + 1) { null }
        var result: Pair<Move?, Int>
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


        if (result.first == null) {
            return EvaluateResult(if (result.second == 0) "stalemate" else "checkmate", i, result.second)
        }

        this.bitBoardService.makeMove(board, result.first!!)
        this.bitBoardService.printBoard(board)
        println()
        println(result.second)
        println("Statistics: depth: $i moveGeneration: $moveGenerationTime makingMove: $makingMoveTime wallClock: $wallClockTime isInCheck: $isInCheckTime evaluationTime: $evaluationTime moves: $moves transpositionHits: $transpositionHits transpositionBestMoves: $transpositionBestMoves")

        val move = this.bitBoardService.ulongToSquare(result.first!!.sourceSquare) +
                this.bitBoardService.ulongToSquare(result.first!!.targetSquare)
        return EvaluateResult(move, i, result.second)

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
        if (threeFoldCount != null && threeFoldCount >= 3) {
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
            var currentEvaluation = Int.MIN_VALUE
            val moves = this.bitBoardService.generateMoves(board, killerMoves, depth, transpositionTable)
            var newAlpha = alpha
            var bestMove: Move? = null
            run search@{
                moves.forEach { move ->
                    val result = this.makeAndUndoMove(board, move, zobristHashesOfGame) {
                        this.minimax(
                            board,
                            depth - 1,
                            newAlpha,
                            beta,
                            false,
                            killerMoves,
                            transpositionTable,
                            zobristHashesOfGame
                        )
                    }
                    if (result == null) {
                        return@forEach
                    }
                    if (result.second > currentEvaluation || bestMove == null) {
                        currentEvaluation = result.second
                        bestMove = move
                    }
                    newAlpha = max(newAlpha, currentEvaluation)
                    if (currentEvaluation >= beta) {
                        killerMoves[depth] = move
                        return@search
                    }
                }
            }

            this.updateTranspositionEntry(entry, depth, bestMove, transpositionTable, board, currentEvaluation, false, currentEvaluation >= beta)
            return this.checkForStalemateAndReturnBestMove(board, bestMove, currentEvaluation)
        } else {
            var currentEvaluation = Int.MAX_VALUE
            val moves = this.bitBoardService.generateMoves(board, killerMoves, depth, transpositionTable)
            var newBeta = beta
            var bestMove: Move? = null
            run search@{
                moves.forEach { move ->
                    val result = this.makeAndUndoMove(board, move, zobristHashesOfGame) {
                        this.minimax(
                            board,
                            depth - 1,
                            alpha,
                            newBeta,
                            true,
                            killerMoves,
                            transpositionTable,
                            zobristHashesOfGame
                        )
                    }
                    if (result == null) {
                        return@forEach
                    }
                    if (result.second < currentEvaluation || bestMove == null) {
                        currentEvaluation = result.second
                        bestMove = move
                    }
                    newBeta = min(newBeta, currentEvaluation)
                    if (currentEvaluation <= alpha) {
                        killerMoves[depth] = move
                        return@search
                    }
                }
            }
            this.updateTranspositionEntry(entry, depth, bestMove, transpositionTable, board, currentEvaluation, currentEvaluation <= alpha, false)
            return this.checkForStalemateAndReturnBestMove(board, bestMove, currentEvaluation)
        }
    }

    private fun incrementRepetitionCount(hash: ULong, repetitions: MutableMap<ULong, Int>) {
        val entry = repetitions[hash]
        repetitions[hash] = if (entry != null) entry + 1 else 1
    }

    private fun decrementRepetitionCount(hash: ULong, repetitions: MutableMap<ULong, Int>) {
        val entry = repetitions[hash]
        if (entry == 1) {
            repetitions.remove(hash)
        } else {
            if (entry != null) {
                repetitions[hash] = entry - 1
            }
        }
    }

    private fun <R> makeAndUndoMove(
        board: BoardState,
        move: Move,
        zobristHashesOfGame: MutableMap<ULong, Int>,
        body: () -> R
    ): R? {
        val undo = this.bitBoardService.makeMove(board, move)
        if (this.bitBoardService.isInCheck(board)) {
            this.bitBoardService.undoMove(board, undo)
            return null
        }
        this.incrementRepetitionCount(board.getZobrastHash(), zobristHashesOfGame)

        val result = body.invoke()

        this.decrementRepetitionCount(board.getZobrastHash(), zobristHashesOfGame)
        this.bitBoardService.undoMove(board, undo)
        return result
    }

    private fun updateTranspositionEntry(
        entry: TranspositionEntry?,
        depth: Int,
        bestMove: Move?,
        transpositionTable: Array<TranspositionEntry?>,
        board: BoardState,
        eval: Int,
        alphaCutoff: Boolean,
        betaCutoff: Boolean
    ) {
        if ((entry == null || entry.depth <= depth) && bestMove != null) {
            transpositionTable[(board.getZobrastHash() and zobristTableSize.toULong()).toInt()] =
                TranspositionEntry(board.getZobrastHash(), eval, depth, alphaCutoff, betaCutoff, bestMove)
        }
    }

    private fun checkForStalemateAndReturnBestMove(board: BoardState, bestMove: Move?, currentEvaluation: Int): Pair<Move?, Int> {
        // No moves but not in check is stalemate
        board.updateSideToPlay(!board.sideToPlay)
        val finalEval = if (bestMove == null && !this.bitBoardService.isInCheck(board)) 0 else currentEvaluation
        board.updateSideToPlay(!board.sideToPlay)
        return Pair(bestMove, finalEval)
    }
}

data class TranspositionEntry(
    val zobristHash: ULong,
    val evaluation: Int,
    val depth: Int,
    val alphaCutoff: Boolean,
    val betaCutoff: Boolean,
    val bestMove: Move
)