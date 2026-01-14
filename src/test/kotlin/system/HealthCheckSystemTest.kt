package system

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.http.HttpStatus
import java.net.URI

class HealthCheckSystemTest : SystemTest(){
    @Test
    fun `actuator health -- with no auth token -- returns 200`() {
        val result = this.restTemplate!!.getForEntity<String>(URI.create("/actuator/health"))

        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    }
}