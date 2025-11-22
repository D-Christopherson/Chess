package system


import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

class EvaluateControllerSystemTest : SystemTest() {

    @Test
    fun `evaluate -- with depth of 5 -- successfully evaluates position`() {
        val headers = HttpHeaders()
        headers["ContentType"] = MediaType.APPLICATION_JSON_VALUE
        val body = LinkedMultiValueMap<String, String>()
        body["fen"] = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        body["depth"] = "5"
        val request = HttpEntity<MultiValueMap<String, String>>(body, headers)


        val result = this.restTemplate!!.postForEntity("/evaluate", request, String::class.java)

        // Most changes to the engine are going to change the exact move. Here we're just interested in making sure
        // that the plumbing is lined up.
        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
        // Hate to use something as slow as a regex in a test, but it feels better than ignoring the body entirely.
        assertThat(result.body).isNotNull().matches("[a-h][1-8][a-h][1-8]")
    }
}