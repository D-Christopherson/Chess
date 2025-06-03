package chess

import chess.MoveGeneratorService.Companion.RANK_3
import chess.MoveGeneratorService.Companion.RANK_6
import org.springframework.stereotype.Service

@ExperimentalUnsignedTypes
@Service
class BitBoardService(private val moveService: MoveGeneratorService) {

    fun evaluatePosition(board: BoardState): Int {
        val start = System.currentTimeMillis()
        val result = board.queens[0].countOneBits() * 8 - board.queens[1].countOneBits() * 8 +
                board.rooks[0].countOneBits() * 5 - board.rooks[1].countOneBits() * 5 +
                board.bishops[0].countOneBits() * 3 - board.bishops[1].countOneBits() * 3 +
                board.knights[0].countOneBits() * 3 - board.knights[1].countOneBits() * 3 +
                board.pawns[0].countOneBits() * 1 - board.pawns[1].countOneBits() * 1
        MinimaxService.evaluationTime += System.currentTimeMillis() - start
        return result
    }

    fun generateMoves(
        board: BoardState,
        killerMoves: Array<Move?>,
        depth: Int,
        transpositionTable: Array<TranspositionEntry?>
    ): List<Move> {
        val start = System.currentTimeMillis()
        val captures = ArrayList<Move>(100)
        val nonCaptures = ArrayList<Move>(100)
        val result = ArrayList<Move>(100)
        val killerMove = killerMoves[depth]

        val pawnMoves = this.moveService.generatePawnMoves(board)
        captures.addAll(pawnMoves.captures)
        nonCaptures.addAll(pawnMoves.other)

        val knightMoves = this.moveService.generateKnightMoves(board)
        captures.addAll(knightMoves.captures)
        nonCaptures.addAll(knightMoves.other)

        val bishopMoves = this.moveService.generateBishopMoves(board)
        captures.addAll(bishopMoves.captures)
        nonCaptures.addAll(bishopMoves.other)

        val queenMoves = this.moveService.generateQueenMoves(board)
        captures.addAll(queenMoves.captures)
        nonCaptures.addAll(queenMoves.other)

        val rookMoves = this.moveService.generateRookMoves(board)
        captures.addAll(rookMoves.captures)
        nonCaptures.addAll(rookMoves.other)

        val kingMoves = this.moveService.generateKingMoves(board)
        captures.addAll(kingMoves.captures)
        nonCaptures.addAll(kingMoves.other)

        val transpositionEntry =
            transpositionTable[(board.getZobrastHash() and MinimaxService.zobristTableSize.toULong()).toInt()]
        if (transpositionEntry != null && transpositionEntry.zobristHash == board.zobristHash) {
            captures.remove(transpositionEntry.bestMove)
            nonCaptures.remove(transpositionEntry.bestMove)
            MinimaxService.transpositionBestMoves++
            result.add(transpositionEntry.bestMove)
        }

        if (captures.remove(killerMove)) {
            result.add(killerMove!!)
        }
        if (nonCaptures.remove(killerMove)) {
            result.add(killerMove!!)
        }

        // Sort captures first since they're more promising for alpha beta pruning
        result.addAll(captures)
        result.addAll(nonCaptures)
        MinimaxService.moveGenerationTime += System.currentTimeMillis() - start
        return result
    }

    /**
     * Update the existing board with a given move
     */
    fun makeMove(board: BoardState, move: Move): UndoMove {
        MinimaxService.moves++
        val piecesToUndo = mutableListOf<UndoPiece>()
        val moveCountToUndo = board.fiftyMoveRuleCounter
        val enPassantSquareToUndo = board.enPassant
        val start = System.currentTimeMillis()
        // TODO castling
        val opponentIndex = if (board.sideToPlay) 1 else 0
        val index = 1 - opponentIndex
        val opponentPieces = this.getPiecesForColor(board, opponentIndex)
        if (move.targetSquare and opponentPieces != 0UL) {
            if (board.queens[opponentIndex] and move.targetSquare != 0UL) {
                board.updateQueens(opponentIndex, move.targetSquare)
                piecesToUndo.add(UndoPiece('q', opponentIndex, move.targetSquare))
            } else if (board.rooks[opponentIndex] and move.targetSquare != 0UL) {
                board.updateRooks(opponentIndex, move.targetSquare)
                piecesToUndo.add(UndoPiece('r', opponentIndex, move.targetSquare))
            } else if (board.bishops[opponentIndex] and move.targetSquare != 0UL) {
                board.updateBishops(opponentIndex, move.targetSquare)
                piecesToUndo.add(UndoPiece('b', opponentIndex, move.targetSquare))
            } else if (board.knights[opponentIndex] and move.targetSquare != 0UL) {
                board.updateKnights(opponentIndex, move.targetSquare)
                piecesToUndo.add(UndoPiece('n', opponentIndex, move.targetSquare))
            } else if (board.pawns[opponentIndex] and move.targetSquare != 0UL) {
                board.updatePawns(opponentIndex, move.targetSquare)
                piecesToUndo.add(UndoPiece('p', opponentIndex, move.targetSquare))
            }
            board.fiftyMoveRuleCounter = 0
        } else {
            board.fiftyMoveRuleCounter++
        }
        if (move.enPassantCapture) {
            if (board.enPassant and RANK_3 != 0UL) {
                board.updatePieces(move.piece, opponentIndex, board.enPassant shl 8)
                piecesToUndo.add(UndoPiece(move.piece, opponentIndex, board.enPassant shl 8))
            } else if (board.enPassant and RANK_6 != 0UL) {
                board.updatePieces(move.piece, opponentIndex, board.enPassant shr 8)
                piecesToUndo.add(UndoPiece(move.piece, opponentIndex, board.enPassant shr 8))
            }
        }

        board.updatePieces(move.piece, index, move.sourceSquare)
        piecesToUndo.add(UndoPiece(move.piece, index, move.sourceSquare))
        if (move.piece == 'p') {
            board.fiftyMoveRuleCounter = 0
        }

        if (move.promotionPiece == null) {
            board.updatePieces(move.piece, index, move.targetSquare)
            piecesToUndo.add(UndoPiece(move.piece, index, move.targetSquare))
        } else {
            board.updatePieces(move.promotionPiece, index, move.targetSquare)
            piecesToUndo.add(UndoPiece(move.promotionPiece, index, move.targetSquare))
        }

        if (move.enPassantSquare != null) {
            board.enPassant = move.enPassantSquare
        } else {
            board.enPassant = 0UL
        }

        board.updateSideToPlay(!board.sideToPlay)
        MinimaxService.makingMoveTime += System.currentTimeMillis() - start
        return UndoMove(piecesToUndo, enPassantSquareToUndo, moveCountToUndo)
    }

    fun undoMove(board: BoardState, undoMove: UndoMove) {
        undoMove.pieceUpdates.forEach { update ->
            board.updatePieces(update.piece, update.index, update.sourceOrTargetSquare)
        }
        board.fiftyMoveRuleCounter = undoMove.moves
        board.enPassant = undoMove.enPassantSquare
        board.updateSideToPlay(!board.sideToPlay)
    }

    data class UndoMove(val pieceUpdates: List<UndoPiece>, val enPassantSquare: ULong, val moves: Int)
    data class UndoPiece(val piece: Char, val index: Int, val sourceOrTargetSquare: ULong)

    fun isInCheck(board: BoardState): Boolean {
        // We're testing for if the player that just made a move is in check in order to ignore that move in our search.
        // So if it's white to move now, we really care about black's king.
        val index = if (board.sideToPlay) 1 else 0
        val opponentIndex = 1 - index
        return this.moveService.testIfInCheck(
            this.getPiecesForColor(board, index),
            this.getPiecesForColor(board, opponentIndex),
            board.king[index],
            board.king[opponentIndex],
            board.queens[opponentIndex],
            board.rooks[opponentIndex],
            board.knights[opponentIndex],
            board.bishops[opponentIndex],
            board.pawns[opponentIndex],
            board.sideToPlay
        )
    }

    fun ulongToSquare(square: ULong): String {
        val file = square.countTrailingZeroBits() % 8
        val rank = square.countTrailingZeroBits() / 8
        val fileName = when (file) {
            0 -> 'h'
            1 -> 'g'
            2 -> 'f'
            3 -> 'e'
            4 -> 'd'
            5 -> 'c'
            6 -> 'b'
            7 -> 'a'
            else -> throw RuntimeException("Math is broken")
        }
        val rankName = when (rank) {
            0 -> '1'
            1 -> '2'
            2 -> '3'
            3 -> '4'
            4 -> '5'
            5 -> '6'
            6 -> '7'
            7 -> '8'
            else -> throw RuntimeException("Math is broken")
        }
        return fileName.toString() + rankName
    }

    fun printBoard(board: BoardState) {
        val grid = Array(8) { Array(8) { '-' } }
        val whiteKing = board.king[0]
        val blackKing = board.king[1]
        val whiteQueens = this.moveService.splitPieces(board.queens[0])
        val blackQueens = this.moveService.splitPieces(board.queens[1])
        val whiteRooks = this.moveService.splitPieces(board.rooks[0])
        val blackRooks = this.moveService.splitPieces(board.rooks[1])
        val whiteBishops = this.moveService.splitPieces(board.bishops[0])
        val blackBishops = this.moveService.splitPieces(board.bishops[1])
        val whiteKnights = this.moveService.splitPieces(board.knights[0])
        val blackKnights = this.moveService.splitPieces(board.knights[1])
        val whitePawns = this.moveService.splitPieces(board.pawns[0])
        val blackPawns = this.moveService.splitPieces(board.pawns[1])

        grid[whiteKing.countTrailingZeroBits() / 8][whiteKing.countTrailingZeroBits() % 8] = 'K'
        grid[blackKing.countTrailingZeroBits() / 8][blackKing.countTrailingZeroBits() % 8] = 'k'
        whiteQueens.forEach { queen ->
            grid[queen.countTrailingZeroBits() / 8][queen.countTrailingZeroBits() % 8] = 'Q'
        }
        blackQueens.forEach { queen ->
            grid[queen.countTrailingZeroBits() / 8][queen.countTrailingZeroBits() % 8] = 'q'
        }
        whiteRooks.forEach { rook ->
            grid[rook.countTrailingZeroBits() / 8][rook.countTrailingZeroBits() % 8] = 'R'
        }
        blackRooks.forEach { rook ->
            grid[rook.countTrailingZeroBits() / 8][rook.countTrailingZeroBits() % 8] = 'r'
        }
        whiteBishops.forEach { bishop ->
            grid[bishop.countTrailingZeroBits() / 8][bishop.countTrailingZeroBits() % 8] = 'B'
        }
        blackBishops.forEach { bishop ->
            grid[bishop.countTrailingZeroBits() / 8][bishop.countTrailingZeroBits() % 8] = 'b'
        }
        whiteKnights.forEach { knight ->
            grid[knight.countTrailingZeroBits() / 8][knight.countTrailingZeroBits() % 8] = 'N'
        }
        blackKnights.forEach { knight ->
            grid[knight.countTrailingZeroBits() / 8][knight.countTrailingZeroBits() % 8] = 'n'
        }
        whitePawns.forEach { pawn ->
            grid[pawn.countTrailingZeroBits() / 8][pawn.countTrailingZeroBits() % 8] = 'P'
        }
        blackPawns.forEach { pawn ->
            grid[pawn.countTrailingZeroBits() / 8][pawn.countTrailingZeroBits() % 8] = 'p'
        }
        for (i in 0..7) {
            for (j in 0..7) {
                print(grid[7 - i][7 - j])
            }
            println()
        }
    }

    private fun getPiecesForColor(board: BoardState, color: Int): ULong {
        return board.pawns[color] or
                board.knights[color] or
                board.bishops[color] or
                board.rooks[color] or
                board.queens[color] or
                board.king[color]
    }

    private fun getAllPieces(board: BoardState): ULong {
        return board.pawns[0] or board.pawns[1] or
                board.knights[0] or board.knights[1] or
                board.bishops[0] or board.bishops[1] or
                board.rooks[0] or board.rooks[1] or
                board.queens[0] or board.queens[1] or
                board.king[0] or board.king[1]
    }
}