package unit

import chess.FenParsingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ExperimentalUnsignedTypes::class)
class FenParsingServiceUnitTest {
    @Test
    fun `fromString -- with starting position -- parses successfully`() {
        val subject = FenParsingService()

        val result = subject.fromString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

        assertThat(result.rooks[0]).isEqualTo(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_10000001UL)
        assertThat(result.rooks[1]).isEqualTo(0b10000001_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL)
        assertThat(result.king[0]).isEqualTo(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00001000UL)
        assertThat(result.king[1]).isEqualTo(0b00001000_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL)
        assertThat(result.queens[0]).isEqualTo(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00010000UL)
        assertThat(result.queens[1]).isEqualTo(0b00010000_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL)
        assertThat(result.bishops[0]).isEqualTo(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00100100UL)
        assertThat(result.bishops[1]).isEqualTo(0b00100100_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL)
        assertThat(result.knights[0]).isEqualTo(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01000010UL)
        assertThat(result.knights[1]).isEqualTo(0b01000010_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL)
        assertThat(result.pawns[0]).isEqualTo(0b00000000_00000000_00000000_00000000_00000000_00000000_11111111_00000000UL)
        assertThat(result.pawns[1]).isEqualTo(0b00000000_11111111_00000000_00000000_00000000_00000000_00000000_00000000UL)

        assertThat(result.sideToPlay).isTrue()

        assertThat(result.castling[0][0]).isTrue()
        assertThat(result.castling[0][1]).isTrue()
        assertThat(result.castling[1][0]).isTrue()
        assertThat(result.castling[1][1]).isTrue()

        assertThat(result.enPassant).isEqualTo("-")
    }
}