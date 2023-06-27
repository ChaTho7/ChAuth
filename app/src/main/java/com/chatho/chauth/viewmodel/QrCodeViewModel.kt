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

package com.chatho.chauth.viewmodel

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import com.chatho.chauth.util.findConstantFieldName
import com.chatho.chauth.view.MainActivity
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * A ViewModel for encapsulating the data for a QR Code, including the encoded data, the bounding
 * box, and the touch behavior on the QR Code.
 */
class QrCodeViewModel(private val activity: ComponentActivity, barcode: Barcode) {
    var boundingRect: Rect = barcode.boundingBox!!
    var qrContent: String = ""
    var qrType = findConstantFieldName(Barcode::class.java, "TYPE_", barcode.valueType)
    var qrCodeTouchCallback = { v: View, e: MotionEvent -> false }

    init {
        when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                qrContent = barcode.url!!.url!!
                qrCodeTouchCallback = { v: View, e: MotionEvent ->
                    if (e.action == MotionEvent.ACTION_DOWN && boundingRect.contains(
                            e.getX().toInt(), e.getY().toInt()
                        )
                    ) {
                        val openBrowserIntent = Intent(Intent.ACTION_VIEW)
                        openBrowserIntent.data = Uri.parse(qrContent)
                        v.context.startActivity(openBrowserIntent)
                    }
                    true // return true from the callback to signify the event was handled
                }
            }

            Barcode.TYPE_TEXT -> {
                qrContent = barcode.rawValue.toString()
                qrCodeTouchCallback = { v: View, e: MotionEvent ->
                    if (e.action == MotionEvent.ACTION_DOWN && boundingRect.contains(
                            e.getX().toInt(), e.getY().toInt()
                        )
                    ) {
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra("qrContent", qrContent)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        activity.startActivity(intent)
                        activity.finish()
                    }
                    true // return true from the callback to signify the event was handled
                }
            }

            Barcode.FORMAT_UNKNOWN -> {
                qrContent = "Unknown format: ${barcode.rawValue.toString()}"
            }

            Barcode.TYPE_UNKNOWN -> {
                qrContent = "Unknown type: ${barcode.rawValue.toString()}"
            }
            // Add other QR Code types here to handle other types of data,
            else -> {
                qrContent = "Unsupported data type: ${barcode.rawValue.toString()}"
            }
        }
    }
}