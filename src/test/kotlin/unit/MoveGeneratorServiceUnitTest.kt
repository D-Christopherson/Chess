package unit

import BitBoardUtil
import chess.BoardState
import chess.MoveGeneratorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ExperimentalUnsignedTypes::class)
class MoveGeneratorServiceUnitTest {

    @Test
    fun `generateKingMoves -- starting position -- no moves`() {
        val board = BitBoardUtil.STARTING_POSITION
        val subject = MoveGeneratorService()

        val result = subject.generateKingMoves(board)

        assertThat(result.captures).isEmpty()
        assertThat(result.other).isEmpty()
    }

    @Test
    fun `generateKingMoves -- king in middle of board -- 8 moves`() {
        val board = BoardState()
        board.king[0] = 0b00010000_00000000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generateKingMoves(board)

        assertThat(result.captures).isEmpty()
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00100000_00000000_00000000_00000000UL,
            0b00001000_00000000_00000000_00000000UL,
            0b00000000_00010000_00000000_00000000UL,
            0b00000000_00100000_00000000_00000000UL,
            0b00000000_00001000_00000000_00000000UL,
            0b00100000_00000000_00000000_00000000_00000000UL,
            0b00010000_00000000_00000000_00000000_00000000UL,
            0b00001000_00000000_00000000_00000000_00000000UL,
        ))
    }

    @Test
    fun `generateQueenMoves -- starting position -- 0 moves`() {
        val board = BitBoardUtil.STARTING_POSITION
        val subject = MoveGeneratorService()

        val result = subject.generateQueenMoves(board)

        assertThat(result.other).isEmpty()
        assertThat(result.captures).isEmpty()
    }

    @Test
    fun `generateQueenMoves -- queen in the middle of the board with no blockers -- 27 moves`() {
        val board = BoardState()
        board.queens[0] = 0b00010000_00000000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generateQueenMoves(board)

        assertThat(result.captures.isEmpty())

        assertThat(result.other.size).isEqualTo(27)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            // West
            0b00000000_00000000_00000000_00000000_00100000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_01000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_10000000_00000000_00000000_00000000UL,
            // North
            0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00010000_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            // East
            0b00000000_00000000_00000000_00000000_00001000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000100_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000010_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000UL,
            // South
            0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00010000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00010000UL,
            // North West
            0b00000000_00000000_00000000_00100000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_01000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_10000000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            // North East
            0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000100_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000010_00000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000001_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            // South West
            0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_01000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_10000000UL,
            // South East
            0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010UL,
        ))
    }

    @Test
    fun `generateQueenMoves -- queen in the middle of the board with 8 blockers -- includes captures`() {
        val board = BoardState()
        board.queens[0] = 0b00010000_00000000_00000000_00000000UL
        board.pawns[1] = 0b00000000_00000000_01010100_00000000_10000001_00000000_01010100_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generateQueenMoves(board)

        assertThat(result.captures.size).isEqualTo(8)
        assertThat(result.captures).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00000000_00000000_01000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000100_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_10000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_01000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00010000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000UL,
        ))

        assertThat(result.other.size).isEqualTo(11)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            // West
            0b00000000_00000000_00000000_00000000_00100000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_01000000_00000000_00000000_00000000UL,
            // North
            0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000UL,
            // East
            0b00000000_00000000_00000000_00000000_00001000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000100_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000010_00000000_00000000_00000000UL,
            // South
            0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000UL,
            // North West
            0b00000000_00000000_00000000_00100000_00000000_00000000_00000000_00000000UL,
            // North East
            0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000UL,
            // South West
            0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000000UL,
            // South East
            0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000UL,
        ))
    }

    @Test
    fun `generateRookMoves -- rook in the middle of the board with no blockers -- 14 moves`() {
        val board = BoardState()
        board.rooks[0] = 0b00010000_00000000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generateRookMoves(board)

        assertThat(result.captures.isEmpty())
        assertThat(result.other.size).isEqualTo(14)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            // West
            0b00000000_00000000_00000000_00000000_00100000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_01000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_10000000_00000000_00000000_00000000UL,
            // North
            0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00010000_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            // East
            0b00000000_00000000_00000000_00000000_00001000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000100_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000010_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000UL,
            // South
            0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00010000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00010000UL,
        ))
    }
    @Test
    fun `generateRookMoves -- rook in the middle of the board with 4 blockers -- includes captures`() {
        val board = BoardState()
        board.rooks[0] = 0b00010000_00000000_00000000_00000000UL
        board.pawns[1] = 0b00000000_00000000_00010000_00000000_10000001_00000000_00010000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generateRookMoves(board)

        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            // West
            0b00000000_00000000_00000000_00000000_00100000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_01000000_00000000_00000000_00000000UL,
            // North
            0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000UL,
            // East
            0b00000000_00000000_00000000_00000000_00001000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000100_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000010_00000000_00000000_00000000UL,
            // South
            0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000UL,
        ))

        assertThat(result.captures).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_10000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00010000_00000000UL
        ))
    }

    @Test
    fun `generateBishopMoves -- bishop in the middle of the board with no blockers -- 13 moves`() {
        val board = BoardState()
        board.bishops[0] = 0b00010000_00000000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generateBishopMoves(board)

        assertThat(result.captures.isEmpty())

        assertThat(result.other.size).isEqualTo(13)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            // North West
            0b00000000_00000000_00000000_00100000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_01000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_10000000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            // North East
            0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000100_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000010_00000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000001_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL,
            // South West
            0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_01000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_10000000UL,
            // South East
            0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010UL,
        ))
    }

    @Test
    fun `generateBishopMoves -- bishop in the middle of the board with 8 blockers -- includes captures`() {
        val board = BoardState()
        board.bishops[0] = 0b00010000_00000000_00000000_00000000UL
        board.pawns[1] = 0b00000000_00000000_01000100_00000000_00000000_00000000_01000100_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generateBishopMoves(board)

        assertThat(result.captures.size).isEqualTo(4)
        assertThat(result.captures).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00000000_00000000_01000000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000100_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_01000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000UL,
        ))

        assertThat(result.other.size).isEqualTo(4)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            // North West
            0b00000000_00000000_00000000_00100000_00000000_00000000_00000000_00000000UL,
            // North East
            0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000UL,
            // South West
            0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000000UL,
            // South East
            0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000UL,
        ))
    }

    @Test
    fun `generateKnightMoves -- starting position -- a3 c3 f3 h3`() {
        val board = BitBoardUtil.STARTING_POSITION
        val subject = MoveGeneratorService()

        val result = subject.generateKnightMoves(board)

        assertThat(result.captures).isEmpty()
        assertThat(result.other.size).isEqualTo(4)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            // A3
            0b10000000_00000000_00000000UL,
            // C3
            0b00100000_00000000_00000000UL,
            // F3
            0b00000100_00000000_00000000UL,
            // H3
            0b00000001_00000000_00000000UL
        ))
    }

    @Test
    fun `generatePawnMoves -- white pawn on second rank with captures available -- can move once, twice, or capture`() {
        val board = BoardState()
        board.pawns[0] = 0b00000000_00000000_00010000_00000000UL
        board.pawns[1] = 0b00000000_00000000_00000000_00000000_00000000_00101000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generatePawnMoves(board)

        assertThat(result.captures.size).isEqualTo(2)
        assertThat(result.captures).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000UL,
        ))

        assertThat(result.other.size).isEqualTo(2)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000UL,
            0b00000000_00000000_00000000_00000000_00010000_00000000_00000000_00000000UL,
        ))
    }

    @Test
    fun `generatePawnMoves -- black pawn on seventh rank with captures available -- can move once, twice, or capture`() {
        val board = BoardState()
        board.sideToPlay = false

        board.pawns[1] = 0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000UL
        board.pawns[0] = 0b00000000_00000000_00101000_00000000_00000000_00000000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.generatePawnMoves(board)

        assertThat(result.captures.size).isEqualTo(2)
        assertThat(result.captures).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00000000_00000000_00100000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00001000_00000000_00000000_00000000_00000000_00000000UL,
        ))

        assertThat(result.other.size).isEqualTo(2)
        assertThat(result.other).flatExtracting({it.targetSquare}).containsAll(listOf(
            0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000UL,
            0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000UL,
        ))
    }

    @Test
    fun `testIfInCheck -- pawn attacking black king -- returns true`() {
        val board = BoardState()
        board.sideToPlay = false
        board.king[1] = 0b00000000_00000000_00000000_00000010_00000000_00000000_00000000_00000000UL
        board.pawns[0] = 0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.testIfInCheck(0UL, 0UL, board.king[1], 0UL, 0UL, 0UL, 0UL, 0UL, board.pawns[0], true)

        assertThat(result).isTrue()
    }
    @Test
    fun `testIfInCheck -- pawn attacking white king -- returns true`() {
        val board = BoardState()
        board.king[0] = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000UL
        board.pawns[1] = 0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000UL
        val subject = MoveGeneratorService()

        val result = subject.testIfInCheck(0UL, 0UL, board.king[0], 0UL, 0UL, 0UL, 0UL, 0UL, board.pawns[1], false)

        assertThat(result).isTrue()
    }
}