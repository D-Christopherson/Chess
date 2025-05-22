package chess

import org.springframework.stereotype.Service

/**
 * Parses Forsyth-Edwards Notation (FEN) representation of boards into our class. Example:
 * rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
 */
@Service
@OptIn(ExperimentalUnsignedTypes::class)
class FenParsingService {
    fun fromString(fen: String): BoardState {
        val board = BoardState()
        val segments = fen.split(' ')
        val ranks = segments[0].split('/').reversed()

        this.parsePieces(board, ranks)

        when (segments[1].first()) {
            'w' -> {} // Do nothing intentionally to not perturb the zobrist hash for white
            'b' -> board.updateSideToPlay(false)
            else -> throw InvalidFenException("Unexpected player=${segments[1]}")
        }

        val castling = segments[2]
        board.castling[0][0] = castling.contains('K')
        board.castling[0][1] = castling.contains('Q')
        board.castling[1][0] = castling.contains('k')
        board.castling[1][1] = castling.contains('q')

        return board
    }

    private fun parsePieces(board: BoardState, ranks: List<String>) {
        var ranksToShift = 0
        ranks.forEach { rank ->
            var fileNumber = 1
            rank.forEach { piece ->
                var newPiece = 1UL
                // Given RNBQKBNR: when we get to the king our file is 5, so we want to shift left 3 times.
                newPiece = newPiece shl (8 - fileNumber)
                newPiece = newPiece shl (ranksToShift * 8)
                val side = this.getSide(piece)
                when (piece) {
                    'r', 'R' -> {
                       board.updateRooks(side, newPiece)
                    }

                    'k', 'K' -> {
                        board.updateKing(side, newPiece)
                    }

                    'p', 'P' -> {
                        board.updatePawns(side, newPiece)
                    }

                    'b', 'B' -> {
                        board.updateBishops(side, newPiece)
                    }

                    'q', 'Q' -> {
                        board.updateQueens(side, newPiece)
                    }

                    'n', 'N' -> {
                        board.updateKnights(side, newPiece)
                    }

                    '1', '2', '3', '4', '5', '6', '7', '8' -> {
                        fileNumber += piece.digitToInt()
                        return@forEach
                    }

                    else -> throw InvalidFenException("Unexpected value in board representation=$piece")
                }
                fileNumber++
            }
            ranksToShift++
        }
    }

    private fun getSide(piece: Char): Int {
        if (piece.isLowerCase()) return 1
        return 0
    }
}

class InvalidFenException(message: String) : RuntimeException(message)