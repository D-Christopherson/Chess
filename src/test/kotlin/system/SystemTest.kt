package system

import chess.Application
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = arrayOf(Application::class))

open class SystemTest {
    @LocalServerPort
    protected val port = 0

    @Autowired
    protected val restTemplate: TestRestTemplate? = null
}