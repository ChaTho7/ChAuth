/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chatho.chauth.drawable

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.chatho.chauth.R
import com.chatho.chauth.viewmodel.QrCodeViewModel

/**
 * A Drawable that handles displaying a QR Code's data and a bounding box around the QR code.
 */
class QrCodeDrawable(private val context: Context, private val qrCodeViewModel: QrCodeViewModel) :
    Drawable() {
    private val qrCodeIcon: Drawable? =
        AppCompatResources.getDrawable(context, R.drawable.qr_code_icon)

    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.clear_green)
        strokeWidth = 5f
        alpha = 200
    }

    private val barcodeTypeRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.dark_purple_grey_color_2)
        alpha = 255
    }
    private val barcodeTypeTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        alpha = 255
        textSize = 50F
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.dark_purple_grey_color_2)
        alpha = 255
    }
    private val contentTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        alpha = 255
        textSize = 30F
    }

    private val contentPadding = 30
    private var barcodeTypeTextWidth =
        barcodeTypeTextPaint.measureText(qrCodeViewModel.qrType).toInt()

    private val path = Path()

    override fun draw(canvas: Canvas) {
        // QR Rect
        val boundingRectWidth = qrCodeViewModel.boundingRect.width()
        drawCurvedArea(canvas, qrCodeViewModel.boundingRect)

        // QR Type
        val qrTypeRectTop =
            qrCodeViewModel.boundingRect.top - (barcodeTypeTextPaint.textSize.toInt() + contentPadding) - (CURVE_RADIUS * 2).toInt()
        val qrTypeRectBottom =
            (qrCodeViewModel.boundingRect.top - contentPadding / 2) - (CURVE_RADIUS * 2).toInt()
        val qrTypeRectHeight = qrTypeRectBottom - qrTypeRectTop
        val qrTypeRectLeft =
            qrCodeViewModel.boundingRect.left + ((boundingRectWidth - (barcodeTypeTextWidth + contentPadding * 2)) / 2) - (contentPadding / 2) - qrTypeRectHeight
        val qrTypeRectRight =
            qrTypeRectLeft + barcodeTypeTextWidth + (contentPadding * 3) + qrTypeRectHeight

        val qrTypeRect = RectF(
            qrTypeRectLeft.toFloat(),
            qrTypeRectTop.toFloat(),
            qrTypeRectRight.toFloat(),
            qrTypeRectBottom.toFloat()
        )
        val cornerRadius = 30f

        canvas.drawRoundRect(qrTypeRect, cornerRadius, cornerRadius, barcodeTypeRectPaint)

        canvas.drawText(
            qrCodeViewModel.qrType,
            (qrTypeRectLeft + (contentPadding * 1.5) + qrTypeRectHeight).toFloat(),
            (qrTypeRectBottom - contentPadding / 2).toFloat(),
            barcodeTypeTextPaint
        )

        // QR Icon
        val imageBounds = Rect(
            (qrTypeRectLeft + (contentPadding * 1.25)).toInt(),
            qrTypeRectTop + 10,
            (qrTypeRectLeft + (contentPadding * 1.25) + (qrTypeRectHeight - 20)).toInt(),
            qrTypeRectBottom - 10
        )
        qrCodeIcon?.bounds = imageBounds
        qrCodeIcon?.draw(canvas)

        // QR Content
        val maxContentWidth = (boundingRectWidth - (contentPadding * 2)).toFloat()
        var stringLines = splitStringByWidth(
            qrCodeViewModel.qrContent, maxContentWidth
        )
        var maxTextWidth =
            contentTextPaint.measureText(stringLines.maxByOrNull { it.length }).toInt()

        if (maxTextWidth > maxContentWidth) {
            stringLines = splitStringByWidth(
                qrCodeViewModel.qrContent, maxTextWidth.toFloat()
            )
            maxTextWidth =
                contentTextPaint.measureText(stringLines.maxByOrNull { it.length }).toInt()
        }

        val qrContentRectLeft =
            qrCodeViewModel.boundingRect.left + ((boundingRectWidth - maxTextWidth) / 2) - (contentPadding * 1.25)
        val qrContentRectRight =
            qrCodeViewModel.boundingRect.right - ((boundingRectWidth - maxTextWidth) / 2) + (contentPadding * 1.25)
        val qrContentRectTop =
            qrCodeViewModel.boundingRect.bottom + ((contentPadding * 1.25) / 2) + (CURVE_RADIUS * 2).toInt()
        val qrContentRectBottom =
            qrCodeViewModel.boundingRect.bottom + (contentTextPaint.textSize.toInt() * stringLines.size) + ((contentPadding * 1.25) * (stringLines.size + (0.5))).toInt() + (CURVE_RADIUS * 2).toInt()

        val qrContentRect = RectF(
            qrContentRectLeft.toFloat(),
            qrContentRectTop.toFloat(),
            qrContentRectRight.toFloat(),
            qrContentRectBottom.toFloat()
        )
        val cornerRadius2 = 20f

        canvas.drawRoundRect(qrContentRect, cornerRadius2, cornerRadius2, contentRectPaint)

        var currentY =
            (qrContentRectTop + contentTextPaint.textSize.toInt() + ((contentPadding * 1.25) / 2)).toFloat()
        for (line in stringLines) {
            val lineWidth = contentTextPaint.measureText(line).toInt()
            canvas.drawText(
                line,
//                (qrContentRectLeft + (contentPadding * 1.25)).toFloat(),
                (qrContentRectLeft + (qrContentRect.width() - lineWidth) / 2).toFloat(),
                currentY,
                contentTextPaint
            )
            currentY += contentTextPaint.textSize + (contentPadding * 1.25).toFloat() / 2
        }
    }

    private fun drawCurvedArea(canvas: Canvas, boundingRect: Rect) {
        val straigthLineWidth = minOf(boundingRect.height(), boundingRect.width()).toFloat() / 5

        drawTopLeftCurve(canvas, boundingRect, straigthLineWidth)
        drawTopRightCurve(canvas, boundingRect, straigthLineWidth)
        drawBottomLeftCurve(canvas, boundingRect, straigthLineWidth)
        drawBottomRightCurve(canvas, boundingRect, straigthLineWidth)
    }

    private fun drawTopLeftCurve(
        canvas: Canvas, boundingRect: Rect, straigthLineWidth: Float
    ) {
        val startX = boundingRect.left.toFloat() - DIFFERENCE_SIZE
        val startY = boundingRect.top.toFloat() + CURVE_RADIUS + straigthLineWidth - DIFFERENCE_SIZE
        val endX = boundingRect.left.toFloat() + CURVE_RADIUS + straigthLineWidth - DIFFERENCE_SIZE
        val endY = boundingRect.top.toFloat() - DIFFERENCE_SIZE

        path.moveTo(startX, startY)

        val controlX = startX
        val controlY = startY - straigthLineWidth

        val endControlX = endX - straigthLineWidth
        val endControlY = endY

        path.cubicTo(controlX, controlY, endControlX, endControlY, endX, endY)

        canvas.drawPath(path, boundingRectPaint)
    }

    private fun drawTopRightCurve(
        canvas: Canvas, boundingRect: Rect, straigthLineWidth: Float
    ) {
        val startX = boundingRect.right.toFloat() + DIFFERENCE_SIZE
        val startY = boundingRect.top.toFloat() + CURVE_RADIUS + straigthLineWidth - DIFFERENCE_SIZE
        val endX =
            boundingRect.right.toFloat() - (CURVE_RADIUS + straigthLineWidth) + DIFFERENCE_SIZE
        val endY = boundingRect.top.toFloat() - DIFFERENCE_SIZE

        path.moveTo(startX, startY)

        val controlX = startX
        val controlY = startY - straigthLineWidth

        val endControlX = endX + straigthLineWidth
        val endControlY = endY

        path.cubicTo(controlX, controlY, endControlX, endControlY, endX, endY)

        canvas.drawPath(path, boundingRectPaint)
    }

    private fun drawBottomLeftCurve(
        canvas: Canvas, boundingRect: Rect, straigthLineWidth: Float
    ) {
        val startX = boundingRect.left.toFloat() - DIFFERENCE_SIZE
        val startY =
            boundingRect.bottom.toFloat() - (CURVE_RADIUS + straigthLineWidth) + DIFFERENCE_SIZE
        val endX = boundingRect.left.toFloat() + CURVE_RADIUS + straigthLineWidth - DIFFERENCE_SIZE
        val endY = boundingRect.bottom.toFloat() + DIFFERENCE_SIZE

        path.moveTo(startX, startY)

        val controlX = startX
        val controlY = startY + straigthLineWidth

        val endControlX = endX - straigthLineWidth
        val endControlY = endY

        path.cubicTo(controlX, controlY, endControlX, endControlY, endX, endY)

        canvas.drawPath(path, boundingRectPaint)
    }

    private fun drawBottomRightCurve(
        canvas: Canvas, boundingRect: Rect, straigthLineWidth: Float
    ) {
        val startX = boundingRect.right.toFloat() + DIFFERENCE_SIZE
        val startY =
            boundingRect.bottom.toFloat() - (CURVE_RADIUS + straigthLineWidth) + DIFFERENCE_SIZE
        val endX =
            boundingRect.right.toFloat() - (CURVE_RADIUS + straigthLineWidth) + DIFFERENCE_SIZE
        val endY = boundingRect.bottom.toFloat() + DIFFERENCE_SIZE

        path.moveTo(startX, startY)

        val controlX = startX
        val controlY = startY + straigthLineWidth

        val endControlX = endX + straigthLineWidth
        val endControlY = endY

        path.cubicTo(controlX, controlY, endControlX, endControlY, endX, endY)

        canvas.drawPath(path, boundingRectPaint)
    }

    override fun setAlpha(alpha: Int) {
        boundingRectPaint.alpha = alpha
        contentRectPaint.alpha = alpha
        contentTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFiter: ColorFilter?) {
        boundingRectPaint.colorFilter = colorFilter
        contentRectPaint.colorFilter = colorFilter
        contentTextPaint.colorFilter = colorFilter
    }

    private fun splitStringByWidth(input: String, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val delimiters = arrayOf(" ", ":", ";", ",")
        val regexPattern = delimiters.joinToString("|") { Regex.escape(it) }
        val words =
            input.splitToSequence(Regex("(?<=[$regexPattern])|(?=[$regexPattern])")).toList()
        var currentLine = ""

        for (word in words) {
            val width = contentTextPaint.measureText("$currentLine $word")

            if (width > maxWidth) {
                if (delimiters.contains(word)) {
                    currentLine += word
                } else {
                    result.add(currentLine.trim())
                    currentLine = word
                }
            } else {
                if (currentLine.isNotEmpty()) {
                    currentLine += " "
                }
                currentLine += word
            }
        }

        if (currentLine.isNotEmpty()) {
            result.add(currentLine.trim())
        }

        return result
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private const val CURVE_RADIUS = 5F
        private const val DIFFERENCE_SIZE = CURVE_RADIUS * 2
    }
}