package unit
import BitBoardUtil.Companion.STARTING_POSITION
import BitBoardUtil.Companion.STARTING_POSITION_ULONG
import chess.BitBoardService
import chess.BoardState
import chess.PathFinderService
import chess.PathFinderService.Slide
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.isA
import org.mockito.kotlin.whenever

@OptIn(ExperimentalUnsignedTypes::class)
class PathFinderServiceUnitTest {

    @Test
    fun `findPath -- starting position -- knight b1 to c3`() {
        val subject = this.givenASubject(STARTING_POSITION_ULONG)

        val result = subject.findPath(STARTING_POSITION, 0, 1, 2, 2)

        assertThat(result.slides)
            .extracting(Slide::startingRow, Slide::startingCol, Slide::endRow, Slide::endCol)
            .containsExactly(
                tuple(1, 1, 2, 1), // pawn b2b3
                tuple(2, 1, 2, 0), // pawn b3a3
                tuple(0, 1, 1, 1), // knight b1b2
                tuple(1, 1, 2, 1), // knight b2b3
                tuple(2, 1, 2, 2), // knight b3c3
                tuple(2, 0, 2, 1), // pawn a3b3
                tuple(2, 1, 1, 1) // pawn b3b2
            )
    }

    // This appears to be about the limit of what the algorithm can do without running out of memory with 130k nodes
    // in the queue and 350MB of heap.
    @Test
    fun `findPath -- knight trapped in a 4x4 cube -- knight d4 to f5`() {
        val subject = this.givenASubject(
            0b00000000_00000000_00111100_00111010_00111100_00111100_00000000_00000000UL
        )

        val result = subject.findPath(STARTING_POSITION, 3, 4, 4, 2)

        assertThat(result.slides)
    }

    @Test
    fun `findPath -- knight trapped in a pyramid -- knight d4 to e6`() {
        val subject = this.givenASubject(
            0b00000000_00010000_00110000_01111100_01111100_00000000_00000000_00000000UL
        )

        val result = subject.findPath(STARTING_POSITION, 3, 4, 5, 3)

        assertThat(result.slides)
    }


    private fun givenASubject(board: ULong): PathFinderService {
        val bitBoardService = mock<BitBoardService>()
        whenever(bitBoardService.getAllPieces(isA<BoardState>())).thenReturn(board)
        return PathFinderService(bitBoardService)
    }
}