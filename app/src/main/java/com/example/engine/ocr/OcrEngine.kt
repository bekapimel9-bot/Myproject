package com.example.engine.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedTextBlock(
    val text: String,
    val bounds: Rect,
    val normalizedBounds: RectF,
    val confidence: Float = 0.85f,
    val lines: List<String> = emptyList()
)

data class OcrScanResult(
    val fullText: String,
    val blocks: List<DetectedTextBlock>,
    val executionTimeMs: Long,
    val frameWidth: Int,
    val frameHeight: Int
)

class OcrEngine {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(
        frameBitmap: Bitmap,
        regionRatio: RectF? = null
    ): OcrScanResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val originalWidth = frameBitmap.width
        val originalHeight = frameBitmap.height

        // Calculate crop bounds if region is specified
        val cropRect = if (regionRatio != null) {
            val left = (regionRatio.left * originalWidth).toInt().coerceIn(0, originalWidth - 1)
            val top = (regionRatio.top * originalHeight).toInt().coerceIn(0, originalHeight - 1)
            val right = (regionRatio.right * originalWidth).toInt().coerceIn(left + 1, originalWidth)
            val bottom = (regionRatio.bottom * originalHeight).toInt().coerceIn(top + 1, originalHeight)
            Rect(left, top, right, bottom)
        } else {
            Rect(0, 0, originalWidth, originalHeight)
        }

        val targetBitmap = if (cropRect.width() < originalWidth || cropRect.height() < originalHeight) {
            Bitmap.createBitmap(
                frameBitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )
        } else {
            frameBitmap
        }

        val inputImage = InputImage.fromBitmap(targetBitmap, 0)
        val visionText: Text = try {
            Tasks.await(recognizer.process(inputImage))
        } catch (e: Exception) {
            if (targetBitmap != frameBitmap && !targetBitmap.isRecycled) {
                targetBitmap.recycle()
            }
            return@withContext OcrScanResult(
                fullText = "",
                blocks = emptyList(),
                executionTimeMs = System.currentTimeMillis() - startTime,
                frameWidth = originalWidth,
                frameHeight = originalHeight
            )
        }

        if (targetBitmap != frameBitmap && !targetBitmap.isRecycled) {
            targetBitmap.recycle()
        }

        val detectedBlocks = mutableListOf<DetectedTextBlock>()
        val combinedTextBuilder = StringBuilder()

        // Sort blocks by reading order (top-to-bottom, left-to-right)
        val sortedBlocks = visionText.textBlocks.sortedWith(
            compareBy<Text.TextBlock> { it.boundingBox?.top ?: 0 }
                .thenBy { it.boundingBox?.left ?: 0 }
        )

        for (block in sortedBlocks) {
            val rawText = block.text.trim()
            if (rawText.isBlank()) continue

            val box = block.boundingBox ?: Rect(0, 0, 0, 0)
            // Offset coordinates back to original full frame
            val adjustedBox = Rect(
                box.left + cropRect.left,
                box.top + cropRect.top,
                box.right + cropRect.left,
                box.bottom + cropRect.top
            )

            val normBounds = RectF(
                adjustedBox.left.toFloat() / originalWidth,
                adjustedBox.top.toFloat() / originalHeight,
                adjustedBox.right.toFloat() / originalWidth,
                adjustedBox.bottom.toFloat() / originalHeight
            )

            val linesList = block.lines.mapNotNull { line ->
                val lineText = line.text.trim()
                if (lineText.isNotEmpty()) lineText else null
            }

            detectedBlocks.add(
                DetectedTextBlock(
                    text = rawText,
                    bounds = adjustedBox,
                    normalizedBounds = normBounds,
                    confidence = 0.90f,
                    lines = linesList
                )
            )

            if (combinedTextBuilder.isNotEmpty()) {
                combinedTextBuilder.append("\n")
            }
            combinedTextBuilder.append(rawText)
        }

        val duration = System.currentTimeMillis() - startTime
        OcrScanResult(
            fullText = combinedTextBuilder.toString(),
            blocks = detectedBlocks,
            executionTimeMs = duration,
            frameWidth = originalWidth,
            frameHeight = originalHeight
        )
    }

    fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
