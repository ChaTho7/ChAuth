package com.chatho.chauth.view

import android.animation.TimeInterpolator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import com.chatho.chauth.BuildConfig
import com.chatho.chauth.R
import com.chatho.chauth.api.HandleAPI
import com.chatho.chauth.api.IPLocationResponse
import com.chatho.chauth.databinding.ActivityMainBinding
import com.chatho.chauth.handler.HandleBiometric
import com.chatho.chauth.handler.HandleEncoding
import com.chatho.chauth.handler.HandlePermission
import com.chatho.chauth.handler.IHandleBiometric
import com.chatho.chauth.service.HandleOneSignal
import com.chatho.chauth.service.IHandleOneSignal
import com.chatho.chauth.util.findConstantFieldName
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.gestures
import javax.crypto.Cipher

class MainActivity : AppCompatActivity(), IHandleBiometric, IHandleOneSignal {
    private lateinit var binding: ActivityMainBinding
    private val handlePermission: HandlePermission = HandlePermission(this)
    private val oneSignal: HandleOneSignal = HandleOneSignal(this, this::handleOneSignalCallback)
    private val handleAPI = HandleAPI(this)
    private lateinit var popOut: Animation
    private lateinit var popIn: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ResourceOptionsManager.getDefault(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

                HandleEncoding.encodeQRCode(qrContent).let { encodedContent ->
                    handleAPI.handleQR("test@admin.com", encodedContent)
                }
            }
        }
    }

    private fun handleMap() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.OUTDOORS) {
            handleMapGestures()

            handleAPI.handleIPLocation(oneSignal.clientIpAddress!!) { response, success ->
                if (success) {
                    val cameraOptions = cameraOptions {
                        center(Point.fromLngLat(response!!.lon, response.lat))
                        zoom(7.0)
                    }

                    handlePopUp(response!!, cameraOptions)
                } else {
                    handlePopUp(null, null)
                }
            }
        }
    }

    private fun handleMapGestures() {
        binding.mapView.gestures.scrollDecelerationEnabled = false
        binding.mapView.gestures.scrollEnabled = false
        binding.mapView.gestures.pinchScrollEnabled = false
        binding.mapView.gestures.doubleTouchToZoomOutEnabled = false
        binding.mapView.gestures.doubleTapToZoomInEnabled = false
        binding.mapView.gestures.quickZoomEnabled = false
        binding.mapView.gestures.pinchToZoomEnabled = false
        binding.mapView.gestures.pitchEnabled = false
        binding.mapView.gestures.simultaneousRotateAndPinchToZoomEnabled = false
        binding.mapView.gestures.pinchToZoomDecelerationEnabled = false
        binding.mapView.gestures.rotateDecelerationEnabled = false
        binding.mapView.gestures.rotateEnabled = false
    }

    private fun handlePopUp(
        response: IPLocationResponse?, cameraOptions: CameraOptions?
    ) {
        binding.ipAddressText.text =
            "(${oneSignal.clientIpAddress})".takeIf { oneSignal.clientIpAddress != null }
                ?: "(Unknown IP Address)"
        if (response != null) {
            binding.ipLocationText.text =
                "${response.city}, ${response.regionName}, ${response.country}, ${response.continent}\n${response.isp}"
            binding.ipGeoLocationText.text = "Lon: ${response.lon} / Lat: ${response.lat}"
        } else {
            binding.ipLocationText.text = "(Unknown IP Location)"
            binding.ipGeoLocationText.text = "(Unknown Geo Location)"
        }

        handleAnimations(cameraOptions)

        binding.allowActionButton.setOnClickListener {
            binding.popupView.startAnimation(popIn)
            oneSignal.isAllowed = true
            biometricSetup()
        }
        binding.denyActionButton.setOnClickListener {
            binding.popupView.startAnimation(popIn)
            oneSignal.isAllowed = false
            biometricSetup()
        }

        binding.popupView.visibility =
            View.INVISIBLE // Add this to temporarily fix the 'popupView.startAnimation' work properly when the app comes from 'killed' status
        // Somehow, 'popupView.startAnimation' doesn't start the animation if 'popupView.visibility' is 'GONE' and app came from 'killed' status.
        binding.popupView.startAnimation(popOut)
    }

    private fun handleAnimations(cameraOptions: CameraOptions?) {
        popOut = AnimationUtils.loadAnimation(this, R.anim.appear)
        popOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                binding.popupView.visibility = View.VISIBLE
                binding.enableQrScanButton.isEnabled = false

                binding.mapView.getMapboxMap().flyTo(cameraOptions!!, mapAnimationOptions {
                    duration(3_000)
                })
            }

            override fun onAnimationEnd(p0: Animation?) {}

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        popIn = AnimationUtils.loadAnimation(this, R.anim.disappear)
        popIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                binding.popupView.visibility = View.GONE
                binding.ipAddressText.text = ""
                binding.enableQrScanButton.isEnabled = true
                binding.mapView.getMapboxMap().setCamera(cameraOptions {
                    center(Point.fromLngLat(35.2, 38.9))
                    zoom(3.0)
                })
            }

            override fun onAnimationRepeat(p0: Animation?) {}

        })
    }

    override fun biometricSetup() {
        if (BiometricManager.from(this)
                .canAuthenticate(Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            val handleBiometric = HandleBiometric(
                this,
                this::biometricOnSucceededCallback,
                this::biometricOnErrorCallback,
                this::biometricOnFailedCallback
            )

            val defaultCipher: Cipher = handleBiometric.setupCiphers()

            handleBiometric.createKey(DEFAULT_KEY_NAME)
            handleBiometric.authenticate(defaultCipher, DEFAULT_KEY_NAME)
        } else {
            Toast.makeText(this, "Biometric is not available...", Toast.LENGTH_LONG).show()
        }
    }

    override fun biometricOnSucceededCallback(result: BiometricPrompt.AuthenticationResult) {
        Log.d("MainActivity", "Authentication is successful")
        val authType = findConstantFieldName(
            BiometricPrompt::class.java, "AUTHENTICATION_RESULT_TYPE_", result.authenticationType
        )
        Log.d("MainActivity", "Authentication type: $authType")
        handleAPI.handleNotify("test@test2.com", oneSignal.isAllowed!!) {
            oneSignal.isAllowed = null
            oneSignal.clientIpAddress = null
        }
    }

    override fun biometricOnErrorCallback(errorCode: Int, errString: CharSequence) {
        Log.d("MainActivity", "$errorCode :: $errString")
        oneSignal.isAllowed = null
        oneSignal.clientIpAddress = null
    }

    override fun biometricOnFailedCallback() {
        Log.d("MainActivity", "Authentication failed for an unknown reason")
        oneSignal.isAllowed = null
        oneSignal.clientIpAddress = null
    }

    override fun handleOneSignalCallback() {
        if (oneSignal.isAllowed == null) {
            if (oneSignal.clientIpAddress != null) {
                handleMap()
            } else {
                handlePopUp(null, null)
            }
        } else {
            biometricSetup()
        }
    }

    companion object {
        private const val DEFAULT_KEY_NAME = "chauth_key"
    }
}