package chess

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class FindPathController(
    private val fenParsingService: FenParsingService,
    private val pathFinderService: PathFinderService
) {
    companion object {
        private val log = LoggerFactory.getLogger(FindPathController::class.java)
    }

    @PostMapping("/findPath")
    fun findPath(request: FindPathRequest): FindPathResponse {
        try {
            val board = fenParsingService.fromString(request.fen)
            val path = pathFinderService.findPath(
                board,
                request.startingRow,
                request.startingCol,
                request.targetRow,
                request.targetCol
            )
            val result = path.slides.joinToString(",") { it.toString() }
            return FindPathResponse(result)
        } catch (e: InvalidFenException) {
            log.info("Unable to parse FEN", e)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}

data class FindPathRequest(
    val fen: String,
    val startingRow: Int,
    val startingCol: Int,
    val targetRow: Int,
    val targetCol: Int
)

data class FindPathResponse(val path: String)