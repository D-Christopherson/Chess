package com.example.chess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min


@Composable
fun ChessBoard(fen: String, evaluateResult: EvaluateResult) {
    val config = LocalConfiguration.current
    val squareSize = min(config.screenHeightDp, config.screenWidthDp) / 8.0f
    val pieces = fen.split(' ')[0]
    val ranks = pieces.split('/')
    Column(
        modifier = Modifier
            .width(Dp(8 * squareSize))
            .padding(Dp(0F), Dp(squareSize))
    ) {
        for ((rankCount, rank) in ranks.withIndex()) {
            var fileCount = 0
            Row {
                for (piece in rank) {
                    if (piece.isDigit()) {
                        for (square in 0..piece.digitToInt()) {
                            Box(
                                modifier = Modifier
                                    .size(Dp(squareSize))
                                    .background(if ((fileCount + square + rankCount) % 2 == 1) Color.LightGray else Color.White)
                            )
                        }
                        fileCount += piece.digitToInt()
                        continue
                    }
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
                            .size(Dp(squareSize))
                            .background(if ((fileCount + rankCount) % 2 == 1) Color.LightGray else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painter = painterResource(pieceIcon), null)
                    }
                    fileCount++
                }
            }
        }
        Row(modifier = Modifier.height(Dp(squareSize / 2))) {}
        Row(modifier = Modifier.height(Dp(squareSize / 2))) {
            val clampedEval =
                if (evaluateResult.evaluation > 0) min(8, evaluateResult.evaluation)
                else max(-8, evaluateResult.evaluation)
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
        EvalText("Evaluation: ${evaluateResult.evaluation}", squareSize)
        EvalText("Depth: ${evaluateResult.depth}", squareSize)
        EvalText("Best move: ${evaluateResult.move}", squareSize)
    }
}

@Composable
fun EvalText(text: String, squareSize: Float) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = Dp(squareSize / 2)),
        fontSize = squareSize.sp / 2
    )
}