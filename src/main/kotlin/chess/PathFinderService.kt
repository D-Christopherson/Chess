package chess

import java.util.Objects
import java.util.PriorityQueue
import kotlin.math.abs

// Moving blockading pieces to let a knight pass is actually a subset of the sliding tile puzzles except we have more
// blank tiles than usual and all but one piece is already on their target square. So we can just use A* with a heuristic
// of the sum of the distance of each piece to its target square. I've also designed the gantry to be able to move pieces
// along the two ends of the board as well as one side to reduce the number of moves required.

@OptIn(ExperimentalUnsignedTypes::class)
class PathFinderService(private val bitBoardService: BitBoardService) {

    fun findPath(boardState: BoardState, startingRow: Int, startingCol: Int, targetRow: Int, targetCol: Int): Path {
        var allPieces = this.bitBoardService.getAllPieces(boardState)
        val pieces = mutableSetOf<Piece>()
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = allPieces and 1UL
                if (piece != 0UL) {
                    // Only the piece moving has a new destination, everything else must return to its original square.
                    if (startingRow == row && startingCol == col) {
                        pieces.add(Piece(row, col, targetRow, targetCol))
                    } else {
                        pieces.add(Piece(row, col, row, col))
                    }
                }
                allPieces = allPieces shr 1
            }
        }
        return this.search(pieces)
    }

    private fun search(pieces: Set<Piece>): Path {
        val searchedPositions = mutableSetOf<String>()
        val pathsToSearch = PriorityQueue<Path> { path1, path2 ->
            (this.calculateTotalDistance(path1.pieces) + path1.distanceSoFar) - (this.calculateTotalDistance(path2.pieces) + path2.distanceSoFar)
        }

        var path = Path(pieces, listOf(), 0)
        while(true) {
            if (this.calculateTotalDistance(path.pieces) == 0) {
                return path
            }
            searchedPositions.add(this.toKey(path.pieces))
            val newSlides = this.generateSlides(path.pieces)
            for (entry in newSlides) {
                val piece = entry.key
                val slides = entry.value
                for (slide in slides) {
                    val clonedPieces = path.pieces.map { it.copy() }.toMutableSet()
                    clonedPieces.remove(piece)
                    val movedPiece = piece.copy(row = slide.endRow, col = slide.endCol)
                    clonedPieces.add(movedPiece)
                    if (searchedPositions.contains(this.toKey(clonedPieces))) {
                        continue
                    }
                    val clonedSlides = path.slides.map { it.copy() }.toMutableList()
                    clonedSlides.add(slide)
                    pathsToSearch.add(Path(clonedPieces, clonedSlides, path.distanceSoFar + 1))
                }
            }
            path = pathsToSearch.remove()
        }
    }

    private fun toKey(pieces: Set<Piece>): String {
        return pieces.sortedBy{it.row * 1000 + it.col*100 + it.targetRow*10 + it.targetCol}.joinToString(",") { "${it.row}${it.col}${it.targetRow}${it.targetCol}"}
    }

    private fun generateSlides(pieces: Set<Piece>): Map<Piece, Set<Slide>> {
        val result = mutableMapOf<Piece, Set<Slide>>()
        for (piece in pieces) {
            val slides = mutableSetOf<Slide>()
            // These bounds will shift outward with the additional rows/columns the gantry can reach when the board is finished.
            if (piece.row > 0) {
                val squareOccupied = pieces.contains(piece.copy(row = piece.row - 1))
                if (!squareOccupied) {
                    slides.add(Slide(piece.row, piece.col, piece.row - 1, piece.col))
                }
            }
            if (piece.row < 7) {
                val squareOccupied = pieces.contains(piece.copy(row = piece.row + 1))
                if (!squareOccupied) {
                    slides.add(Slide(piece.row, piece.col, piece.row + 1, piece.col))
                }
            }
            if (piece.col > 0) {
                val squareOccupied = pieces.contains(piece.copy(col = piece.col - 1))
                if (!squareOccupied) {
                    slides.add(Slide(piece.row, piece.col, piece.row, piece.col - 1))
                }
            }
            if (piece.col < 7) {
                val squareOccupied = pieces.contains(piece.copy(col = piece.col + 1))
                if (!squareOccupied) {
                    slides.add(Slide(piece.row, piece.col, piece.row, piece.col + 1))
                }
            }
            result[piece] = slides
        }
        return result
    }

    private fun calculateTotalDistance(pieces: Set<Piece>): Int {
        var totalDistance = 0
        pieces.forEach {
            totalDistance += abs(it.col - it.targetCol) + abs(it.row - it.targetRow)
        }
        return totalDistance
    }

    data class Path(val pieces: Set<Piece>, val slides: List<Slide>, val distanceSoFar: Int)
    data class Slide(val startingRow: Int, val startingCol: Int, val endRow: Int, val endCol: Int) {
        override fun toString(): String {
            return "$startingRow$startingCol$endRow$endCol"
        }
    }
    data class Piece(val row: Int, val col: Int, val targetRow: Int, val targetCol: Int) {
        override fun equals(other: Any?): Boolean {
            if (other !is Piece) {
                return false
            }
            return this.row == other.row && this.col == other.col
        }

        override fun hashCode(): Int {
            return Objects.hash(this.row, this.col)
        }
    }
}
