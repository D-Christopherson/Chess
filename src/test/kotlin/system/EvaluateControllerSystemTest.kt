package system


import chess.EvaluateResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class EvaluateControllerSystemTest(@Value("\${auth.token}") private val authToken: String) : SystemTest() {

    @Test
    fun `evaluate -- no auth token -- returns 401`() {
        val headers = this.givenHeaders(auth = null)
        val body = this.givenABody()
        val request = HttpEntity(body, headers)

        val result = this.restTemplate!!.postForEntity("/evaluate", request, String::class.java)

        assertThat(result.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `evaluate -- incorrect token -- returns 401`() {
        val headers = this.givenHeaders(auth = "abc")
        val body = this.givenABody()
        val request = HttpEntity(body, headers)

        val result = this.restTemplate!!.postForEntity("/evaluate", request, String::class.java)

        assertThat(result.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `evaluate -- with invalid fen -- returns 400`() {
        val headers = this.givenHeaders()
        val body = this.givenABody(fen = "abc")
        val request = HttpEntity(body, headers)

        val result = this.restTemplate!!.postForEntity("/evaluate", request, String::class.java)

        assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `evaluate -- with depth of 5 -- successfully evaluates position`() {
        val headers = this.givenHeaders()
        val body = this.givenABody(depth = 5)
        val request = HttpEntity(body, headers)


        val result = this.restTemplate!!.postForEntity("/evaluate", request, EvaluateResult::class.java)

        // Most changes to the engine are going to change the exact move. Here we're just interested in making sure
        // that the plumbing is lined up.
        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
        // Hate to use something as slow as a regex in a test, but it feels better than ignoring the body entirely.
        assertThat(result.body).isNotNull()
        assertThat(result.body!!.move).isNotNull().matches("[a-h][1-8][a-h][1-8]")
        assertThat(result.body!!.depth).isGreaterThanOrEqualTo(5)
    }

    private fun givenABody(
        depth: Int = 0,
        fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    ): Map<String, String> {
        val body = mutableMapOf<String, String>()
        body["fen"] = fen
        body["depth"] = depth.toString()
        return body
    }

    private fun givenHeaders(auth: String? = this.authToken): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        if (auth != null) {
            headers["Authorization"] = auth
        }
        return headers
    }
}