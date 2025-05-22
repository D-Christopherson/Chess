package system

import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

class EvaluateControllerSystemTest: SystemTest() {

    @Test
    fun `evaluate -- with depth of 5 -- successfully evaluates position`() {
        val headers = HttpHeaders()
        headers["ContentType"] = MediaType.APPLICATION_JSON_VALUE
        val body = LinkedMultiValueMap<String, String>()
        body["fen"] = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        body["depth"] = "6"
        val request = HttpEntity<MultiValueMap<String, String>>(body, headers)


        val result = this.restTemplate!!.postForEntity("/evaluate", request, String::class.java)

        println(result)
    }

    @Test
    fun `evaluate -- test -- successfully evaluates position`() {
        // TODO leaves bishop??
        val headers = HttpHeaders()
        headers["ContentType"] = MediaType.APPLICATION_JSON_VALUE
        val body = LinkedMultiValueMap<String, String>()
        body["fen"] = "r1bqkb1r/ppppp1pp/B4p2/8/8/4P3/PPPP1PPP/R1BQK1NR b KQkq - 0 7"
        val request = HttpEntity<MultiValueMap<String, String>>(body, headers)


        val result = this.restTemplate!!.postForEntity("/evaluate", request, String::class.java)

        println(result)
    }
    @Test
    fun `evaluate -- test 2 -- successfully evaluates position`() {

        val headers = HttpHeaders()
        headers["ContentType"] = MediaType.APPLICATION_JSON_VALUE
        val body = LinkedMultiValueMap<String, String>()
        body["fen"] = "8/7Q/k7/8/8/8/2K5/8 b - - 34 67"
        body["depth"] = "12"
        val request = HttpEntity<MultiValueMap<String, String>>(body, headers)


        val result = this.restTemplate!!.postForEntity("/evaluate", request, String::class.java)

        println(result)
    }
}