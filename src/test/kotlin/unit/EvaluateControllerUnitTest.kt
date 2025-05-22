package unit

import chess.BoardState
import chess.EvaluateController
import chess.FenParsingService
import chess.MinimaxService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class EvaluateControllerUnitTest {

    @Test
    fun `evaluate -- with no duration or depth -- uses defaults`() {
        val mockMinimaxService: MinimaxService = mock()
        val subject = this.givenASubject(minimaxService = mockMinimaxService)

        subject.evaluate("")

        verify(mockMinimaxService).evaluate(any<BoardState>(), eq(5))
    }

    private fun givenASubject(
        fenParsingService: FenParsingService = mock(),
        minimaxService: MinimaxService = mock()
    ): EvaluateController {
        whenever(fenParsingService.fromString(anyString())).thenReturn(BoardState())
        return EvaluateController(fenParsingService, minimaxService)
    }
}