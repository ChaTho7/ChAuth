package com.chatho.chauth.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chatho.chauth.databinding.ActivityScanBinding
import com.chatho.chauth.drawable.QrCodeDrawable
import com.chatho.chauth.handler.HandlePermission
import com.chatho.chauth.viewmodel.QrCodeViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
//    private lateinit var recognizer: TextRecognizer
    private val handlePermission: HandlePermission = HandlePermission(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (handlePermission.allRuntimePermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions have not been granted !", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        hideSystemBars()
    }

    private fun startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraController = LifecycleCameraController(baseContext)
        val previewView: PreviewView = viewBinding.viewFinder

        val options =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        barcodeScanner = BarcodeScanning.getClient(options)
//        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

//        cameraController.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this),
//            MlKitAnalyzer(
//                listOf(recognizer),
//                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
//                ContextCompat.getMainExecutor(this)
//            ) { result: MlKitAnalyzer.Result? ->
//                val barcodeResults = result?.getValue(recognizer)
//                if ((barcodeResults == null) || (barcodeResults.text.isEmpty())) {
//                    previewView.overlay.clear()
//                    previewView.setOnTouchListener { _, _ -> false }
//                    return@MlKitAnalyzer
//                }
//
//                println("HERE IS THE TEXT: ")
////                val listOfWords = barcodeResults.textBlocks.map { it.text }
//                println(barcodeResults.text)
//
//                previewView.overlay.clear()
//            })

        cameraController.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this)
            ) { result: MlKitAnalyzer.Result? ->
                val barcodeResults = result?.getValue(barcodeScanner)
                if ((barcodeResults == null) || (barcodeResults.size == 0) || (barcodeResults.first() == null)) {
                    previewView.overlay.clear()
                    previewView.setOnTouchListener { _, _ -> false }
                    return@MlKitAnalyzer
                }

                val qrCodeViewModel = QrCodeViewModel(this, barcodeResults[0])
                val qrCodeDrawable = QrCodeDrawable(this, qrCodeViewModel)

                previewView.setOnTouchListener(qrCodeViewModel.qrCodeTouchCallback)
                previewView.overlay.clear()
                previewView.overlay.add(qrCodeDrawable)
            })

        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
//        recognizer.close()
        barcodeScanner.close()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}