package chess

import org.springframework.stereotype.Service

@OptIn(ExperimentalUnsignedTypes::class)
@Service
class MoveGeneratorService {

    companion object {
        const val A_FILE = 0b10000000_10000000_10000000_10000000_10000000_10000000_10000000_10000000UL
        const val B_FILE = 0b01000000_01000000_01000000_01000000_01000000_01000000_01000000_01000000UL
        const val C_FILE = 0b00100000_00100000_00100000_00100000_00100000_00100000_00100000_00100000UL
        const val D_FILE = 0b00010000_00010000_00010000_00010000_00010000_00010000_00010000_00010000UL
        const val E_FILE = 0b00001000_00001000_00001000_00001000_00001000_00001000_00001000_00001000UL
        const val F_FILE = 0b00000100_00000100_00000100_00000100_00000100_00000100_00000100_00000100UL
        const val G_FILE = 0b00000010_00000010_00000010_00000010_00000010_00000010_00000010_00000010UL
        const val H_FILE = 0b00000001_00000001_00000001_00000001_00000001_00000001_00000001_00000001UL
        const val RANK_1 = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_11111111UL
        const val RANK_2 = 0b00000000_00000000_00000000_00000000_00000000_00000000_11111111_00000000UL
        const val RANK_3 = 0b00000000_00000000_00000000_00000000_00000000_11111111_00000000_00000000UL
        const val RANK_4 = 0b00000000_00000000_00000000_00000000_11111111_00000000_00000000_00000000UL
        const val RANK_5 = 0b00000000_00000000_00000000_11111111_00000000_00000000_00000000_00000000UL
        const val RANK_6 = 0b00000000_00000000_11111111_00000000_00000000_00000000_00000000_00000000UL
        const val RANK_7 = 0b00000000_11111111_00000000_00000000_00000000_00000000_00000000_00000000UL
        const val RANK_8 = 0b11111111_00000000_00000000_00000000_00000000_00000000_00000000_00000000UL

        // Knight moves are pre-calculated because they don't care about pieces in between
        val knightMoves = ArrayList<ArrayList<ULong>>()

        // King moves don't have pieces in between
        val kingMoves = ArrayList<ArrayList<ULong>>()

        // For sliding pieces it's easier to treat each direction separately, so bishops/rooks/queens will all build
        // from this table while checking for pieces that block the attack. Indexed by [direction][trailing zeroes]
        val slidingMoves = ArrayList<ArrayList<ULong>>()
        const val NORTH = 0
        const val NORTH_EAST = 1
        const val EAST = 2
        const val SOUTH_EAST = 3
        const val SOUTH = 4
        const val SOUTH_WEST = 5
        const val WEST = 6
        const val NORTH_WEST = 7
        val allDirections = listOf(NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, NORTH_WEST, SOUTH_WEST)

        // Testing if the king is in check for real (there are no blockers for a sliding piece) is slow. This list of
        // moves gives us a weaker "is in check" test where we can quickly say that it's impossible for any of the
        // opponents pieces to hit the king if there were no other pieces on the board.
        val superPieceMoves = ArrayList<ULong>()
    }

    init {
        // Pre-calculated knight moves
        for (i in 0..63) {
            val movesFromSquare = ArrayList<ULong>()
            knightMoves.add(movesFromSquare)
            val knight = 1UL shl i
            // The guard around the files keep us from wrapping around the board laterally. We could also do bounds
            // detection for the first/second and seventh/eighth rank, but I've decided to just check for 0 instead.
            if (knight and A_FILE == 0UL) {
                this.addMoveIfNotZero(knight shl 17, movesFromSquare)
                this.addMoveIfNotZero(knight shr 15, movesFromSquare)
                if (knight and B_FILE == 0UL) {
                    this.addMoveIfNotZero(knight shl 10, movesFromSquare)
                    this.addMoveIfNotZero(knight shr 6, movesFromSquare)
                }
            }
            if (knight and H_FILE == 0UL) {
                this.addMoveIfNotZero(knight shl 15, movesFromSquare)
                this.addMoveIfNotZero(knight shr 17, movesFromSquare)
                if (knight and G_FILE == 0UL) {
                    this.addMoveIfNotZero(knight shl 6, movesFromSquare)
                    this.addMoveIfNotZero(knight shr 10, movesFromSquare)
                }
            }
        }

        // Pre-calculated king moves
        for (i in 0..63) {
            val movesFromSquare = ArrayList<ULong>()
            kingMoves.add(movesFromSquare)
            val king = 1UL shl i
            // Left moves
            if (king and A_FILE == 0UL) {
                this.addMoveIfNotZero(king shl 1, movesFromSquare)
                this.addMoveIfNotZero(king shr 7, movesFromSquare)
                this.addMoveIfNotZero(king shl 9, movesFromSquare)
            }

            // Right moves
            if (king and H_FILE == 0UL) {
                this.addMoveIfNotZero(king shl 7, movesFromSquare)
                this.addMoveIfNotZero(king shr 1, movesFromSquare)
                this.addMoveIfNotZero(king shr 9, movesFromSquare)
            }

            // Up and down
            this.addMoveIfNotZero(king shl 8, movesFromSquare)
            this.addMoveIfNotZero(king shr 8, movesFromSquare)
        }

        // Pre-calculated sliding moves
        for (i in 0..7) {
            slidingMoves.add(ArrayList())
        }

        // When we generate the actual attacks and there are no blockers, we'll end up with 64 trailing/leading 0s.
        // Instead of introducing a branch to handle that case we just extend our list here for negative slides
        // and at the end for positive slides.
        slidingMoves[EAST].add(0UL)
        slidingMoves[SOUTH_EAST].add(0UL)
        slidingMoves[SOUTH].add(0UL)
        slidingMoves[SOUTH_WEST].add(0UL)

        // The pattern for sliding pieces is to shift in that direction until we hit the A or H file. We don't have to
        // worry about falling off the top or bottom of the board since we'll just be bitwise OR-ing zero.
        for (i in 0..63) {
            val slidingPiece = 1UL shl i

            var northMoves = 0UL
            for (j in 1..7) {
                northMoves = northMoves or (slidingPiece shl j * 8)
            }
            slidingMoves[NORTH].add(northMoves)

            var northEastMoves = 0UL
            if (slidingPiece and H_FILE == 0UL) {
                for (j in 1..7) {
                    northEastMoves = northEastMoves or (slidingPiece shl j * 7)
                    if (northEastMoves and H_FILE != 0UL) {
                        break
                    }
                }
            }
            slidingMoves[NORTH_EAST].add(northEastMoves)

            var eastMoves = 0UL
            if (slidingPiece and H_FILE == 0UL) {
                for (j in 1..7) {
                    eastMoves = eastMoves or (slidingPiece shr j)
                    if (eastMoves and H_FILE != 0UL) {
                        break
                    }
                }
            }
            slidingMoves[EAST].add(eastMoves)

            var southEastMoves = 0UL
            if (slidingPiece and H_FILE == 0UL) {
                for (j in 1..7) {
                    southEastMoves = southEastMoves or (slidingPiece shr j * 9)
                    if (southEastMoves and H_FILE != 0UL) {
                        break
                    }
                }
            }
            slidingMoves[SOUTH_EAST].add(southEastMoves)

            var southMoves = 0UL
            for (j in 1..7) {
                southMoves = southMoves or (slidingPiece shr j * 8)
            }
            slidingMoves[SOUTH].add(southMoves)

            var southWestMoves = 0UL
            if (slidingPiece and A_FILE == 0UL) {
                for (j in 1..7) {
                    southWestMoves = southWestMoves or (slidingPiece shr j * 7)
                    if (southWestMoves and A_FILE != 0UL) {
                        break
                    }
                }
            }
            slidingMoves[SOUTH_WEST].add(southWestMoves)

            var westMoves = 0UL
            if (slidingPiece and A_FILE == 0UL) {
                for (j in 1..7) {
                    westMoves = westMoves or (slidingPiece shl j)
                    if (westMoves and A_FILE != 0UL) {
                        break
                    }
                }
            }
            slidingMoves[WEST].add(westMoves)

            var northWestMoves = 0UL
            if (slidingPiece and A_FILE == 0UL) {
                for (j in 1..7) {
                    northWestMoves = northWestMoves or (slidingPiece shl j * 9)
                    if (northWestMoves and A_FILE != 0UL) {
                        break
                    }
                }
            }
            slidingMoves[NORTH_WEST].add(northWestMoves)
        }

        slidingMoves[NORTH].add(0UL)
        slidingMoves[NORTH_EAST].add(0UL)
        slidingMoves[WEST].add(0UL)
        slidingMoves[NORTH_WEST].add(0UL)

        for (i in 0..63) {
            var joinedKnightMoves = 0UL
            knightMoves[i].forEach {
                joinedKnightMoves = joinedKnightMoves or it
            }
            superPieceMoves.add(
                slidingMoves[NORTH][i] or
                        slidingMoves[EAST][i + 1] or
                        slidingMoves[SOUTH][i + 1] or
                        slidingMoves[WEST][i] or
                        slidingMoves[NORTH_EAST][i] or
                        slidingMoves[NORTH_WEST][i] or
                        slidingMoves[SOUTH_EAST][i + 1] or
                        slidingMoves[SOUTH_WEST][i + 1] or
                        joinedKnightMoves
            )

        }
    }

    fun generateKingMoves(board: BoardState): GeneratedMoves {
        // TODO castling
        val captures = HashSet<Move>()
        val other = HashSet<Move>()
        val index = this.sideToPlayToIndex(board.sideToPlay)
        val opponentPieces = this.getPiecesForColor(board, 1 - index)
        val ownPieces = this.getPiecesForColor(board, index)
        val king = board.king[index]
        val moves = kingMoves[king.countTrailingZeroBits()]
        moves.forEach { move ->
            if (move and ownPieces == 0UL) {
                if (move and opponentPieces == 0UL) {
                    other.add(Move('k', index, king, move))
                } else {
                    captures.add(Move('k', index, king, move))
                }
            }
        }
        return GeneratedMoves(captures, other)
    }

    fun generateQueenMoves(board: BoardState): GeneratedMoves {
        return this.generateSlidingMoves(
            board,
            board.queens,
            'q',
            listOf(NORTH, EAST, WEST, SOUTH, NORTH_EAST, SOUTH_EAST, NORTH_WEST, SOUTH_WEST)
        )
    }

    fun generateRookMoves(board: BoardState): GeneratedMoves {
        return this.generateSlidingMoves(
            board,
            board.rooks,
            'r',
            listOf(NORTH, SOUTH, EAST, WEST)
        )
    }

    fun generateKnightMoves(board: BoardState): GeneratedMoves {
        val captures = HashSet<Move>()
        val other = HashSet<Move>()
        val index = this.sideToPlayToIndex(board.sideToPlay)
        val opponentPieces = this.getPiecesForColor(board, 1 - index)
        val ownPieces = this.getPiecesForColor(board, index)
        val splitKnights = this.splitPieces(board.knights[index])

        splitKnights.forEach { knight ->
            val moves = knightMoves[knight.countTrailingZeroBits()]
            moves.forEach { move ->
                if (move and ownPieces == 0UL) {
                    if (move and opponentPieces == 0UL) {
                        other.add(Move('n', index, knight, move))
                    } else {
                        captures.add(Move('n', index, knight, move))
                    }
                }
            }
        }

        return GeneratedMoves(captures, other)
    }

    fun generateBishopMoves(board: BoardState): GeneratedMoves {
        return this.generateSlidingMoves(
            board,
            board.bishops,
            'b',
            listOf(NORTH_EAST, SOUTH_EAST, NORTH_WEST, SOUTH_WEST)
        )
    }

    fun generatePawnMoves(board: BoardState): GeneratedMoves {
        val captures = HashSet<Move>()
        val other = HashSet<Move>()
        val index = this.sideToPlayToIndex(board.sideToPlay)
        val opponentPieces = this.getPiecesForColor(board, 1 - index)
        val ownPieces = this.getPiecesForColor(board, index)
        val allPieces = opponentPieces or ownPieces
        val splitPawns = this.splitPieces(board.pawns[index])

        splitPawns.forEach { pawn ->
            if (board.sideToPlay) {
                val move = pawn shl 8
                val occupied = move and allPieces
                if (occupied == 0UL) {
                    // Sort the double moves first so the engine plays more aggressively
                    if (pawn and RANK_2 != 0UL) {
                        val doubleMove = move shl 8
                        if (doubleMove and allPieces == 0UL) {
                            other.add(Move('p', index, pawn, doubleMove, null, move))
                        }
                    }
                    if (move and RANK_8 == 0UL) {
                        other.add(Move('p', index, pawn, move))
                    } else {
                        this.addPromotionMove(index, pawn, move, other)
                    }
                }
                if (pawn and A_FILE == 0UL) {
                    val attackLeft = pawn shl 9
                    this.addPawnCaptures(board, attackLeft, pawn, opponentPieces,RANK_8, captures, index)
                }
                if (pawn and H_FILE == 0UL) {
                    val attackRight = pawn shl 7
                    this.addPawnCaptures(board, attackRight, pawn, opponentPieces,RANK_8, captures, index)
                }
            } else {
                val move = pawn shr 8
                val occupied = move and allPieces
                if (occupied == 0UL) {
                    if (pawn and RANK_7 != 0UL) {
                        val doubleMove = move shr 8
                        if (doubleMove and allPieces == 0UL) {
                            other.add(Move('p', index, pawn, doubleMove, null, move))
                        }
                    }
                    if (move and RANK_1 == 0UL) {
                        other.add(Move('p', index, pawn, move))
                    } else {
                        this.addPromotionMove(index, pawn, move, other)
                    }
                }
                if (pawn and H_FILE == 0UL) {
                    val attackRight = pawn shr 9
                    this.addPawnCaptures(board, attackRight, pawn, opponentPieces,RANK_1, captures, index)
                }
                if (pawn and A_FILE == 0UL) {
                    val attackLeft = pawn shr 7
                    this.addPawnCaptures(board, attackLeft, pawn, opponentPieces,RANK_1, captures, index)
                }
            }
        }

        return GeneratedMoves(captures, other)
    }

    private fun addPawnCaptures(
        board: BoardState,
        attackedSquare: ULong,
        pawn: ULong,
        opponentPieces: ULong,
        promotionRank: ULong,
        captures: MutableSet<Move>,
        index: Int
    ) {
        if (attackedSquare and opponentPieces != 0UL) {
            if (attackedSquare and promotionRank == 0UL) {
                captures.add(Move('p', index, pawn, attackedSquare))
            } else {
                this.addPromotionMove(index, pawn, attackedSquare, captures)
            }
        }
        if (attackedSquare and board.enPassant != 0UL) {
            captures.add(Move('p', index, pawn, attackedSquare, enPassantCapture = true))
        }
    }

    // Given the state of the board, test if the king is in check. I chose to break this out from the move generation
    // since detecting something like a discovered check from an en-passant capture is tough to do without actually
    // making the move.
    fun testIfInCheck(
        ownPieces: ULong,
        opponentPieces: ULong,
        king: ULong,
        opponentKing: ULong,
        opponentQueens: ULong,
        opponentRooks: ULong,
        opponentKnights: ULong,
        opponentBishops: ULong,
        opponentPawns: ULong,
        sideToPlay: Boolean
    ): Boolean {
        val start = System.currentTimeMillis()
        val kingSquare = king.countTrailingZeroBits()
        if (!sideToPlay) {
            val pawnAttacks = ((opponentPawns and H_FILE.inv()) shr 9) or ((opponentPawns and A_FILE.inv()) shr 7)
            if (pawnAttacks and king != 0UL) {
                MinimaxService.isInCheckTime += System.currentTimeMillis() - start
                return true
            }
        } else {
            val pawnAttacks = ((opponentPawns and A_FILE.inv()) shl 9) or ((opponentPawns and H_FILE.inv()) shl 7)
            if (pawnAttacks and king != 0UL) {
                MinimaxService.isInCheckTime += System.currentTimeMillis() - start
                return true
            }
        }

        // Short circuit if the opponent has no pieces even pointed at the king. So far this seems to make no improvement
        if (superPieceMoves[kingSquare] and opponentPieces == 0UL) {
            MinimaxService.isInCheckTime += System.currentTimeMillis() - start
            return false
        }

        // We're generating the sliding moves from the king's square and checking if any of the opponent's pieces are
        // on that path.
        val allPieces = ownPieces or opponentPieces
        allDirections.forEach { direction ->
            val attacks: Pair<ULong, ULong>
            when (direction) {
                NORTH, NORTH_EAST, NORTH_WEST, WEST -> {
                    attacks = this.getPositiveSlidingAttacks(
                        allPieces,
                        opponentPieces,
                        direction,
                        kingSquare
                    )
                }

                SOUTH, SOUTH_EAST, SOUTH_WEST, EAST -> {
                    attacks = this.getNegativeSlidingAttacks(
                        allPieces,
                        opponentPieces,
                        direction,
                        kingSquare
                    )
                }

                else -> throw RuntimeException("Unexpected direction")
            }
            when (direction) {
                NORTH, SOUTH, EAST, WEST -> {
                    if (attacks.second and (opponentQueens or opponentRooks) != 0UL) {
                        MinimaxService.isInCheckTime += System.currentTimeMillis() - start
                        return true
                    }
                }

                else -> {
                    if (attacks.second and (opponentQueens or opponentBishops) != 0UL) {
                        MinimaxService.isInCheckTime += System.currentTimeMillis() - start
                        return true
                    }
                }
            }
        }

        val knightAttacks = knightMoves[kingSquare]
        var attacks = 0UL
        knightAttacks.forEach { attack ->
            attacks = attacks or attack
        }
        if (attacks and opponentKnights != 0UL) {
            MinimaxService.isInCheckTime += System.currentTimeMillis() - start
            return true
        }

        val kingAttacks = kingMoves[kingSquare]
        attacks = 0UL
        kingAttacks.forEach { attack ->
            attacks = attacks or attack
        }
        if (attacks and opponentKing != 0UL) {
            MinimaxService.isInCheckTime += System.currentTimeMillis() - start
            return true
        }

        MinimaxService.isInCheckTime += System.currentTimeMillis() - start
        return false
    }

    /**
     * Breaks up a ULong like 0b00000011 into [0b00000010, 0b00000001]
     */
    fun splitPieces(pieces: ULong): List<ULong> {
        val splitPieces = ArrayList<ULong>()
        var remainingPieces = pieces
        while (remainingPieces != 0UL) {
            val trailingZeroes = remainingPieces.countTrailingZeroBits()
            val piece = 1UL shl trailingZeroes
            splitPieces.add(piece)
            remainingPieces = remainingPieces xor piece
        }
        return splitPieces
    }

    private fun sideToPlayToIndex(sideToPlay: Boolean): Int {
        return if (sideToPlay) 0 else 1
    }

    private fun getPiecesForColor(board: BoardState, color: Int): ULong {
        return board.pawns[color] or
                board.knights[color] or
                board.bishops[color] or
                board.rooks[color] or
                board.queens[color] or
                board.king[color]
    }

    // Util to ignore moves that fall off the edge of the board (like a king on the 8th rank trying to move up 1 square)
    private fun addMoveIfNotZero(square: ULong, moves: ArrayList<ULong>) {
        square.takeIf { it != 0UL }?.let { moves.add(it) }
    }

    private fun generateSlidingMoves(
        board: BoardState,
        pieceList: ULongArray,
        pieceType: Char,
        directions: List<Int>
    ): GeneratedMoves {
        val captures = HashSet<Move>()
        val other = HashSet<Move>()
        val index = this.sideToPlayToIndex(board.sideToPlay)
        val opponentPieces = this.getPiecesForColor(board, 1 - index)
        val ownPieces = this.getPiecesForColor(board, index)
        val allPieces = opponentPieces or ownPieces
        val splitPieces = this.splitPieces(pieceList[index])
        splitPieces.forEach { piece ->
            directions.forEach { direction ->
                val attacks: Pair<ULong, ULong>
                when (direction) {
                    NORTH, NORTH_EAST, NORTH_WEST, WEST ->
                        attacks = this.getPositiveSlidingAttacks(
                            allPieces,
                            opponentPieces,
                            direction,
                            piece.countTrailingZeroBits()
                        )

                    SOUTH, SOUTH_EAST, SOUTH_WEST, EAST ->
                        attacks = this.getNegativeSlidingAttacks(
                            allPieces,
                            opponentPieces,
                            direction,
                            piece.countTrailingZeroBits()
                        )

                    else -> throw RuntimeException("Unexpected direction")
                }
                if (attacks.second != 0UL) {
                    captures.add(Move(pieceType, index, piece, attacks.second))
                }
                var quietMoves = attacks.first
                while (quietMoves != 0UL) {
                    val trailingZeroes = quietMoves.countTrailingZeroBits()
                    val quietMove = 1UL shl trailingZeroes
                    other.add(Move(pieceType, index, piece, quietMove))
                    quietMoves = quietMoves xor quietMove
                }
            }
        }
        return GeneratedMoves(captures, other)
    }

    // Sliding attack calculation adapted from https://www.chessprogramming.org/Classical_Approach#Zero_Count
    private fun getPositiveSlidingAttacks(
        allPieces: ULong,
        opponentPieces: ULong,
        direction: Int,
        square: Int
    ): Pair<ULong, ULong> {
        val attacks = slidingMoves[direction][square]
        val blocker = attacks and allPieces
        val blockingSquare = blocker.countTrailingZeroBits()
        // Notably different than the wiki's approach, I want to return the blocking square separately and only if
        // it's the opponent's piece. This will let us easily sort captures to the front during our search.
        val attacksWithoutBlockingPiece = attacks xor (slidingMoves[direction][blockingSquare] or blocker)
        return Pair(attacksWithoutBlockingPiece, opponentPieces and (1UL shl blockingSquare) and attacks)
    }

    // Sliding attack calculation adapted from https://www.chessprogramming.org/Classical_Approach#Zero_Count
    private fun getNegativeSlidingAttacks(
        allPieces: ULong,
        opponentPieces: ULong,
        direction: Int,
        square: Int
    ): Pair<ULong, ULong> {
        // Negative sliding attacks have the extra entry at the start, so shift everything back
        val attacks = slidingMoves[direction][square + 1]
        val blocker = attacks and allPieces
        val blockingSquare = blocker.countLeadingZeroBits()
        val attacksWithoutBlockingPiece = attacks xor (slidingMoves[direction][64 - blockingSquare] or blocker)
        return Pair(attacksWithoutBlockingPiece, opponentPieces and (1UL shl (63 - blockingSquare)) and attacks)
    }

    private fun addPromotionMove(index: Int, piece: ULong, targetSquare: ULong, moves: MutableSet<Move>) {
        moves.add(Move('p', index, piece, targetSquare, 'q'))
        moves.add(Move('p', index, piece, targetSquare, 'r'))
        moves.add(Move('p', index, piece, targetSquare, 'b'))
        moves.add(Move('p', index, piece, targetSquare, 'n'))
    }
}

data class GeneratedMoves(val captures: Set<Move>, val other: Set<Move>)

data class Move(
    val piece: Char,
    val index: Int,
    val sourceSquare: ULong,
    val targetSquare: ULong,
    val promotionPiece: Char? = null,
    val enPassantSquare: ULong? = null,
    val enPassantCapture: Boolean = false
)
