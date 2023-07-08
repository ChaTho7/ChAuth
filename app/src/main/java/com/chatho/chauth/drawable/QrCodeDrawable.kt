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
import androidx.core.content.ContextCompat
import com.chatho.chauth.R
import com.chatho.chauth.viewmodel.QrCodeViewModel

/**
 * A Drawable that handles displaying a QR Code's data and a bounding box around the QR code.
 */
class QrCodeDrawable(private val context: Context, private val qrCodeViewModel: QrCodeViewModel) :
    Drawable() {
    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.clear_green)
        strokeWidth = 5F
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

    override fun draw(canvas: Canvas) {
        // QR Rect
        val boundingRectWidth =
            qrCodeViewModel.boundingRect.right - qrCodeViewModel.boundingRect.left

        canvas.drawRect(qrCodeViewModel.boundingRect, boundingRectPaint)

        // QR Type
        val qrTypeRectLeft =
            qrCodeViewModel.boundingRect.left + ((boundingRectWidth - (barcodeTypeTextWidth + contentPadding * 2)) / 2)
        val qrTypeRectRight = qrTypeRectLeft + barcodeTypeTextWidth + contentPadding * 2
        val qrTypeRectTop =
            qrCodeViewModel.boundingRect.top - (barcodeTypeTextPaint.textSize.toInt() + contentPadding)
        val qrTypeRectBottom = qrCodeViewModel.boundingRect.top - contentPadding / 2

        canvas.drawRect(
            Rect(
                qrTypeRectLeft, qrTypeRectTop, qrTypeRectRight, qrTypeRectBottom
            ), barcodeTypeRectPaint
        )
        canvas.drawText(
            qrCodeViewModel.qrType,
            (qrTypeRectLeft + contentPadding).toFloat(),
            (qrTypeRectBottom - contentPadding / 2).toFloat(),
            barcodeTypeTextPaint
        )

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
            qrCodeViewModel.boundingRect.left + ((boundingRectWidth - maxTextWidth) / 2) - contentPadding
        val qrContentRectRight =
            qrCodeViewModel.boundingRect.right - ((boundingRectWidth - maxTextWidth) / 2) + contentPadding
        val qrContentRectTop = qrCodeViewModel.boundingRect.bottom + (contentPadding / 2)
        val qrContentRectBottom =
            qrCodeViewModel.boundingRect.bottom + (contentTextPaint.textSize.toInt() * stringLines.size) + (contentPadding * (stringLines.size + (0.5))).toInt()

        canvas.drawRect(
            Rect(
                qrContentRectLeft, qrContentRectTop, qrContentRectRight, qrContentRectBottom
            ), contentRectPaint
        )

        var currentY =
            (qrContentRectTop + contentTextPaint.textSize.toInt() + (contentPadding / 2)).toFloat()
        for (line in stringLines) {
            canvas.drawText(
                line, (qrContentRectLeft + contentPadding).toFloat(), currentY, contentTextPaint
            )
            currentY += contentTextPaint.textSize + contentPadding / 2
        }
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

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}