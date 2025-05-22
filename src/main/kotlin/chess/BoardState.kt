package chess

import kotlin.random.Random
import kotlin.random.nextULong

@OptIn(ExperimentalUnsignedTypes::class)
data class BoardState(
    val pawns: ULongArray = ULongArray(2),
    val knights: ULongArray = ULongArray(2),
    val bishops: ULongArray = ULongArray(2),
    val rooks: ULongArray = ULongArray(2),
    val queens: ULongArray = ULongArray(2),
    val king: ULongArray = ULongArray(2),
    var sideToPlay: Boolean = true,
    var castling: Array<Array<Boolean>> = arrayOf(arrayOf(true, true), arrayOf(true, true)),
    var enPassant: ULong = 0UL,
    var fiftyMoveRuleCounter: Int = 0,
    var zobristHash: ULong = 0UL
) {
    // These values will be used to maintain our Zobrist hash of the position in order to lookup memoized results in our
    // transposition table.
    private val zobristHashingTable = Array(12) {
        Array(64) { 0UL }
    }

    private val zobristTurn = Random.nextULong()
    private val wKingCastle = Random.nextULong()
    private val bKingCastle = Random.nextULong()
    private val wQueenCastle = Random.nextULong()
    private val bQueenCastle = Random.nextULong()
    private val enPassantFileHash = Array(8) { 0UL }

    init {
        zobristHashingTable.forEach { pieceHashes ->
            for (i in pieceHashes.indices) {
                pieceHashes[i] = Random.nextULong()
            }
        }
        for (i in enPassantFileHash.indices) {
            enPassantFileHash[i] = Random.nextULong()
        }
    }

    fun getZobrastHash(): ULong {
       return zobristHash
    }

    fun updatePieces(piece: Char, index: Int, value: ULong) {
        when(piece) {
            'k' -> {
                zobristHash = zobristHash xor zobristHashingTable[10 + index][value.countTrailingZeroBits()]
                king[index] = king[index] xor value
            }
            'q' -> {
                zobristHash = zobristHash xor zobristHashingTable[8 + index][value.countTrailingZeroBits()]
                queens[index] = queens[index] xor value
            }
            'r' -> {
                zobristHash = zobristHash xor zobristHashingTable[6 + index][value.countTrailingZeroBits()]
                rooks[index] = rooks[index] xor value
            }
            'b' -> {
                zobristHash = zobristHash xor zobristHashingTable[2 + index][value.countTrailingZeroBits()]
                bishops[index] = bishops[index] xor value
            }
            'n' -> {
                zobristHash = zobristHash xor zobristHashingTable[4 + index][value.countTrailingZeroBits()]
                knights[index] = knights[index] xor value
            }
            'p' -> {
                zobristHash = zobristHash xor zobristHashingTable[index][value.countTrailingZeroBits()]
                pawns[index] = pawns[index] xor value
            }
        }
    }

    fun updatePawns(index: Int, value: ULong) {
        zobristHash = zobristHash xor zobristHashingTable[index][value.countTrailingZeroBits()]
        pawns[index] = pawns[index] xor value
    }

    fun updateBishops(index: Int, value: ULong) {
        zobristHash = zobristHash xor zobristHashingTable[2 + index][value.countTrailingZeroBits()]
        bishops[index] = bishops[index] xor value
    }

    fun updateKnights(index: Int, value: ULong) {
        zobristHash = zobristHash xor zobristHashingTable[4 + index][value.countTrailingZeroBits()]
        knights[index] = knights[index] xor value
    }

    fun updateRooks(index: Int, value: ULong) {
        zobristHash = zobristHash xor zobristHashingTable[6 + index][value.countTrailingZeroBits()]
        rooks[index] = rooks[index] xor value
    }

    fun updateQueens(index: Int, value: ULong) {
        zobristHash = zobristHash xor zobristHashingTable[8 + index][value.countTrailingZeroBits()]
        queens[index] = queens[index] xor value
    }

    fun updateKing(index: Int, value: ULong) {
        zobristHash = zobristHash xor zobristHashingTable[10 + index][value.countTrailingZeroBits()]
        king[index] = king[index] xor value
    }

    fun updateSideToPlay(side: Boolean) {
        sideToPlay = side
        zobristHash = zobristHash xor zobristTurn
    }
}