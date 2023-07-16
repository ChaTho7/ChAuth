package com.chatho.chauth.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chatho.chauth.BuildConfig
import com.chatho.chauth.api.HandleAPI
import com.chatho.chauth.databinding.ActivityMainBinding
import com.chatho.chauth.handler.HandleEncoding
import com.chatho.chauth.handler.HandleMapView
import com.chatho.chauth.handler.HandlePermission
import com.chatho.chauth.service.HandleOneSignal
import com.mapbox.maps.ResourceOptionsManager
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var handleMapView: HandleMapView
    private lateinit var handleAPI: HandleAPI
    private lateinit var oneSignal: HandleOneSignal
    private val handlePermission: HandlePermission = HandlePermission(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ResourceOptionsManager.getDefault(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleAPI = HandleAPI(this)

        handleMapView = HandleMapView(this, binding, handleAPI)
        oneSignal = HandleOneSignal(this, handleMapView::handleOneSignalCallback)
        oneSignal.setup()
        if (!handlePermission.allRuntimePermissionsGranted()) {
            handlePermission.getRuntimePermissions()
        }

        binding.enableQrScanButton.setOnClickListener {
            if (handlePermission.allRuntimePermissionsGranted()) {
                val intent = Intent(this, ScanActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permissions have not granted !", Toast.LENGTH_LONG).show()
                handlePermission.getRuntimePermissions()
            }
        }

        intent.getStringExtra("qrContent").let { qrContent ->
            if (!qrContent.isNullOrBlank()) {
                val handleAPI = HandleAPI(this)

                val qrCode = JSONObject(qrContent).get("code") as String
                val buildType = JSONObject(qrContent).get("build_type") as String
                HandleEncoding.encodeQRCode(qrCode).let { encodedContent ->
                    handleAPI.handleQR("test@admin.com", encodedContent, buildType)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }
}