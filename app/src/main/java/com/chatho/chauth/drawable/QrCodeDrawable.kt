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
class QrCodeDrawable(private val context: Context, qrCodeViewModel: QrCodeViewModel) : Drawable() {
    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.cyan)
        strokeWidth = 5F
        alpha = 200
    }

    private val barcodeTypeRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.dark_blue)
        alpha = 255
    }

    private val barcodeTypeTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        alpha = 255
        textSize = 50F
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.dark_blue)
        alpha = 255
    }

    private val contentTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        alpha = 255
        textSize = 36F
    }

    private val qrCodeViewModel = qrCodeViewModel
    private val contentPadding = 25
    private var barcodeTypeTextWidth =
        barcodeTypeTextPaint.measureText(qrCodeViewModel.qrType).toInt()
    private var textWidth = contentTextPaint.measureText(qrCodeViewModel.qrContent).toInt()

    override fun draw(canvas: Canvas) {
        canvas.drawRect(qrCodeViewModel.boundingRect, boundingRectPaint)
        // QR Type
        canvas.drawRect(
            Rect(
                qrCodeViewModel.boundingRect.left + (((qrCodeViewModel.boundingRect.right - qrCodeViewModel.boundingRect.left) - (barcodeTypeTextWidth + contentPadding * 2)) / 2),
                qrCodeViewModel.boundingRect.top - (barcodeTypeTextPaint.textSize.toInt() + contentPadding),
                qrCodeViewModel.boundingRect.left + (((qrCodeViewModel.boundingRect.right - qrCodeViewModel.boundingRect.left) - (barcodeTypeTextWidth + contentPadding * 2)) / 2) + barcodeTypeTextWidth + contentPadding * 2,
                qrCodeViewModel.boundingRect.top - contentPadding / 2
            ),
            barcodeTypeRectPaint
        )
        canvas.drawText(
            qrCodeViewModel.qrType,
            (qrCodeViewModel.boundingRect.left + ((qrCodeViewModel.boundingRect.right - qrCodeViewModel.boundingRect.left - barcodeTypeTextWidth) / 2)).toFloat(),
            (qrCodeViewModel.boundingRect.top - contentPadding).toFloat(),
            barcodeTypeTextPaint
        )
        // QR Content
        canvas.drawRect(
            Rect(
                qrCodeViewModel.boundingRect.left,
                qrCodeViewModel.boundingRect.bottom + contentPadding / 2,
                qrCodeViewModel.boundingRect.left + textWidth + contentPadding * 2,
                qrCodeViewModel.boundingRect.bottom + contentTextPaint.textSize.toInt() + contentPadding
            ),
            contentRectPaint
        )
        canvas.drawText(
            qrCodeViewModel.qrContent,
            (qrCodeViewModel.boundingRect.left + contentPadding).toFloat(),
            (qrCodeViewModel.boundingRect.bottom + contentPadding * 2).toFloat(),
            contentTextPaint
        )
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