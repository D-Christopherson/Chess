package com.example.chess

import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
fun ChessBoard(
    fen: String,
    evaluateResult: EvaluateResult,
    socket: BluetoothSocket? = null,
    authToken: String? = null
) {
    var boardState by remember { mutableStateOf(fen) }
    var evaluation by remember { mutableIntStateOf(evaluateResult.evaluation) }
    var bestMove by remember { mutableStateOf(evaluateResult.move) }
    var depth by remember { mutableIntStateOf(evaluateResult.depth) }
    var whiteToPlay by remember { mutableStateOf(true) }
    val client = OkHttpClient()
    val config = LocalConfiguration.current
    val squareSize = min(config.screenHeightDp, config.screenWidthDp) / 8.0f
    Column(
        modifier = Modifier
            .width(Dp(8 * squareSize))
            .padding(Dp(0F), Dp(squareSize))
    ) {
        Board(boardState, squareSize)
        Row(modifier = Modifier.height(Dp(squareSize / 2))) {}
        Row(modifier = Modifier.height(Dp(squareSize / 2))) {
            EvalBar(evaluation, squareSize)
        }
        // If I try to put this box in the row above it disappears unless the second part of the
        // eval bar stops just short of the edge of the screen. Instead I'll use this row which
        // I want for some padding anyway and just offset it upwards.
        Row(modifier = Modifier.height(Dp(squareSize / 2))) {
            Box(
                modifier = Modifier
                    .width(Dp(squareSize / 8))
                    .height(squareSize.dp)
                    .absoluteOffset(Dp(squareSize * 4 - squareSize / 16), Dp(-squareSize / 2))
                    .background(Color.DarkGray)
            )
        }
        EvalText("Evaluation:", evaluation, squareSize)
        EvalText("Depth:", depth, squareSize)
        EvalText("Best move:", bestMove, squareSize)

        if (authToken != null && socket != null) {
            Row(
                modifier = Modifier
                    .height(squareSize.dp)
                    .offset(squareSize.dp)
            ) {
                ScanBoardButton {
                    thread(block = {
                        if (!socket.isConnected) {
                            socket.connect()
                        }
                        socket.outputStream.write("SCAN#".toByteArray())
                        Log.d("BT", "Scan board")
                        while (socket.inputStream.available() == 0) {
                            Log.d("BT", "No data")
                            Thread.sleep(500)
                        }
                        Thread.sleep(500)

                        val fen = socket.inputStream.readNBytes(socket.inputStream.available())
                            .decodeToString()
                        Log.d("BT", fen)
                        boardState = fen
                    })
                }
                EvaluateButton(modifier = Modifier.offset(squareSize.dp)) {
                    val body = JSONObject()
                    if (whiteToPlay) {
                        body.put("fen", boardState.replace("b", "w"))
                    } else {
                        body.put("fen", boardState.replace("w", "b"))
                    }
                    body.put("depth", 8)
                    val headers = mutableMapOf<String, String>()
                    headers["Authorization"] = authToken
                    headers["Content-Type"] = "application/json"
                    val request = Request.Builder()
                        .url("https://chess.dakotachristopherson.com/evaluate")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .headers(headers.toHeaders())
                        .build()
                    thread(block = {
                        try {
                            Log.d("Engine", "Sending evaluate request")
                            val response = client.newCall(request).execute()
                            val body = response.body.string()
                            Log.d("Engine", body)
                            val evaluateResult =
                                Json.decodeFromString<EvaluateResult>(body)
                            evaluation = evaluateResult.evaluation
                            depth = evaluateResult.depth
                            bestMove = evaluateResult.move
                        } catch (e: Exception) {
                            Log.d("Engine", e.stackTraceToString())
                        }
                    })
                }
                Switch(checked = !whiteToPlay, modifier = Modifier.offset(squareSize.dp), onCheckedChange = {
                    whiteToPlay = !it
                })
            }
        }
    }
}

@Composable
fun Board(fen: String, squareSize: Float) {
    val pieces = fen.split(' ')[0]
    val ranks = pieces.split('/')
    for ((rankCount, rank) in ranks.withIndex()) {
        var fileCount = 0
        Row {
            for (piece in rank) {
                if (piece.isDigit()) {
                    EmptySquares(squareSize, fileCount, rankCount, piece.digitToInt())
                    fileCount += piece.digitToInt()
                    continue
                }
                Piece(piece, squareSize, fileCount, rankCount)
                fileCount++
            }
        }
    }
}

@Composable
fun EvalText(field: String, value: Any?, squareSize: Float) {
    Text(
        text = "$field $value",
        modifier = Modifier.padding(horizontal = Dp(squareSize / 2)),
        fontSize = squareSize.sp / 2
    )
}

@Composable
fun Piece(piece: Char, squareSize: Float, file: Int, rank: Int) {
    val pieceIcon = when (piece) {
        'k' -> R.drawable.chess_kdt45
        'q' -> R.drawable.chess_qdt45
        'r' -> R.drawable.chess_rdt45
        'n' -> R.drawable.chess_ndt45
        'b' -> R.drawable.chess_bdt45
        'p' -> R.drawable.chess_pdt45
        'K' -> R.drawable.chess_klt45
        'Q' -> R.drawable.chess_qlt45
        'R' -> R.drawable.chess_rlt45
        'N' -> R.drawable.chess_nlt45
        'B' -> R.drawable.chess_blt45
        'P' -> R.drawable.chess_plt45
        else -> throw Exception("Unable to parse FEN")
    }
    Box(
        modifier = Modifier
            .size(squareSize.dp)
            .background(if ((file + rank) % 2 == 1) Color.LightGray else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(pieceIcon), null)
    }
}

@Composable
fun EmptySquares(squareSize: Float, file: Int, rank: Int, emptySquares: Int) {
    for (square in 0..emptySquares - 1) {
        Box(
            modifier = Modifier
                .size(squareSize.dp)
                .background(if ((file + square + rank) % 2 == 1) Color.LightGray else Color.White)
        )
    }
}

@Composable
fun EvalBar(evaluateResult: Int, squareSize: Float) {
    val clampedEval =
        if (evaluateResult > 0) min(8, evaluateResult)
        else max(-8, evaluateResult)
    val evalBarOffset = clampedEval * squareSize / 2
    Box(
        modifier = Modifier
            .size(Dp(squareSize * 4 + evalBarOffset))
            .background(Color.LightGray)
    )
    Box(
        modifier = Modifier
            .size(Dp(squareSize * 4 - evalBarOffset))
            .background(Color.Black)
    )
}

@Composable
fun ScanBoardButton(onClick: () -> Unit) {
    OutlinedButton(onClick = { onClick() }) {
        Text("Scan Board")
    }
}

@Composable
fun EvaluateButton(modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = { onClick() }, modifier = modifier) {
        Text("Evaluate")
    }
}

@Serializable
data class EvaluateResult(val move: String?, val depth: Int, val evaluation: Int)
