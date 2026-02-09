package chess

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException

@RestController
public class EvaluateController @Autowired constructor(
    private val fenParsingService: FenParsingService,
    private val minimaxService: MinimaxService
) {

    companion object {
        private val log = LoggerFactory.getLogger(EvaluateController::class.java)
    }

    @PostMapping("/evaluate")
    fun evaluate(@RequestBody request: EvaluateRequest): EvaluateResult {
        try {
            val board = this.fenParsingService.fromString(request.fen)
            return this.minimaxService.evaluate(board, request.depth)
        } catch (e: InvalidFenException) {
            log.info("Unable to parse FEN", e)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }

}
data class EvaluateRequest(val fen: String, val depth: Int = 5)
data class EvaluateResult(val move: String?, val depth: Int, val evaluation: Int)