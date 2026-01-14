package chess

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluateController @Autowired constructor(
    private val fenParsingService: FenParsingService,
    private val minimaxService: MinimaxService
) {

    @PostMapping("/evaluate")
    fun evaluate(@RequestParam fen: String, @RequestParam depth: Int = 5): EvaluateResult {
        val board = this.fenParsingService.fromString(fen)
        return this.minimaxService.evaluate(board, depth)
    }

}
data class EvaluateResult(val move: String?, val depth: Int, val evaluation: Int)