package com.example.chess

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.caverock.androidsvg.PreserveAspectRatio
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG

class ChessBoard(context: Context, private val fen: String?, private val evaluateResult: EvaluateResult? = null): View(context) {
    private val paint = Paint()
    private val renderOptions = RenderOptions()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDraw(canvas: Canvas) {
        val squareSize = Math.min(height, width) / 8.0f
        for (file in 0..7) {
            for (rank in 0..7) {
                paint.setColor(if ((file + rank) % 2 == 0) Color.LightGray.toArgb() else Color.White.toArgb())
                paint.style = Paint.Style.FILL
                // Top and bottom have an extra square size just to move the board away from the top bar.
                canvas.drawRect(
                    squareSize * file,
                    squareSize * rank + squareSize,
                    squareSize * (file + 1),
                    squareSize * (rank + 1) + squareSize,
                    paint
                )
            }
        }
        if (fen == null) {
            return
        }
        val pieces = fen.split(' ')[0]
        val ranks = pieces.split('/')
        for ((rankCount, rank) in ranks.withIndex()) {
            var fileCount = 0
            for (piece in rank) {
                if (piece.isDigit()) {
                    fileCount += piece.digitToInt()
                    continue
                }
                val pieceIcon = when(piece) {
                    'k' -> SVG.getFromAsset(context.assets, "pieces/Chess_kdt45.svg")
                    'q' -> SVG.getFromAsset(context.assets, "pieces/Chess_qdt45.svg")
                    'r' -> SVG.getFromAsset(context.assets, "pieces/Chess_rdt45.svg")
                    'n' -> SVG.getFromAsset(context.assets, "pieces/Chess_ndt45.svg")
                    'b' -> SVG.getFromAsset(context.assets, "pieces/Chess_bdt45.svg")
                    'p' -> SVG.getFromAsset(context.assets, "pieces/Chess_pdt45.svg")
                    'K' -> SVG.getFromAsset(context.assets, "pieces/Chess_klt45.svg")
                    'Q' -> SVG.getFromAsset(context.assets, "pieces/Chess_qlt45.svg")
                    'R' -> SVG.getFromAsset(context.assets, "pieces/Chess_rlt45.svg")
                    'N' -> SVG.getFromAsset(context.assets, "pieces/Chess_nlt45.svg")
                    'B' -> SVG.getFromAsset(context.assets, "pieces/Chess_blt45.svg")
                    'P' -> SVG.getFromAsset(context.assets, "pieces/Chess_plt45.svg")
                    else -> throw Exception("Unable to parse FEN")
                }
                renderOptions.viewPort(
                    squareSize * fileCount,
                    squareSize * rankCount + squareSize,
                    squareSize * (fileCount + 1),
                    squareSize * (rankCount + 1) + squareSize)
                renderOptions.viewBox(0f,0f,15f,15f)
                renderOptions.preserveAspectRatio(PreserveAspectRatio.LETTERBOX)
                pieceIcon.renderToCanvas(canvas, renderOptions)
                fileCount++
            }
        }
        if (evaluateResult == null) {
            return
        }
        paint.color = Color.Black.toArgb()
        paint.textSize = squareSize / 3
        val firstLineY = squareSize * 9.5f
        val evaluation = if (evaluateResult.evaluation == Int.MAX_VALUE || evaluateResult.evaluation == Int.MIN_VALUE) "Checkmate in ${evaluateResult.depth}" else evaluateResult.evaluation
        canvas.drawText("Evaluation: $evaluation", squareSize, firstLineY, paint)
        canvas.drawText("Depth: ${evaluateResult.depth}", squareSize, firstLineY + paint.textSize, paint)
        canvas.drawText("Best Move: ${evaluateResult.move}", squareSize, firstLineY + 2 * paint.textSize, paint)

        val middleOfBoard = squareSize * 4
        val clampedEval = if (evaluateResult.evaluation > 0) Math.min(8, evaluateResult.evaluation) else Math.max(-8, evaluateResult.evaluation)
        val evalBarOffset = clampedEval * squareSize / 2
        paint.color = Color.LightGray.toArgb()
        val evalBarTop = squareSize * 9.5f + paint.textSize * 3
        canvas.drawRect(0f, evalBarTop, middleOfBoard + evalBarOffset, evalBarTop + squareSize / 2, paint)
        paint.color = Color.DarkGray.toArgb()
        canvas.drawRect(middleOfBoard + evalBarOffset, evalBarTop, squareSize * 8, evalBarTop + squareSize / 2, paint)
        paint.color = Color.Black.toArgb()
        canvas.drawRect(middleOfBoard - squareSize / 20, evalBarTop, middleOfBoard + squareSize / 20, evalBarTop + squareSize / 2, paint)
    }
}
