package com.chatho.chauth.view

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chatho.chauth.R
import com.chatho.chauth.api.HandleAPI
import com.chatho.chauth.api.IPLocationResponse
import com.chatho.chauth.databinding.ActivityFloatingBinding
import com.chatho.chauth.handler.HandleBiometric
import com.chatho.chauth.holder.OneSignalHolder
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar

class FloatingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFloatingBinding
    private lateinit var handleBiometric: HandleBiometric
    private lateinit var handleAPI: HandleAPI

    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap
    private lateinit var popUpView: CardView
    private lateinit var popOut: Animation
    private lateinit var popIn: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFloatingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleAPI = HandleAPI(this)
        handleBiometric = HandleBiometric(this, handleAPI)

        mapView = binding.mapViewFloatingActivity
        mapBoxMap = mapView.getMapboxMap()
        popUpView = binding.popupViewFloatingActivity

        if (OneSignalHolder.isAllowed == null) {
            if (OneSignalHolder.clientIpAddress != null) {
                setupMap()
            } else {
                setupPopUp(null, null)
            }
        } else if (OneSignalHolder.isAllowed == false) {
            handleAPI.handleNotify("test@test2.com", false)
        } else {
            handleBiometric.biometricSetup(true)
        }
    }

    override fun onResume() {
        super.onResume()

        hideSystemBars()
    }

    private fun handleMapGestures() {
        mapView.gestures.scrollDecelerationEnabled = false
        mapView.gestures.scrollEnabled = false
        mapView.gestures.pinchScrollEnabled = false
        mapView.gestures.doubleTouchToZoomOutEnabled = false
        mapView.gestures.doubleTapToZoomInEnabled = false
        mapView.gestures.quickZoomEnabled = false
        mapView.gestures.pinchToZoomEnabled = false
        mapView.gestures.pitchEnabled = false
        mapView.gestures.simultaneousRotateAndPinchToZoomEnabled = false
        mapView.gestures.pinchToZoomDecelerationEnabled = false
        mapView.gestures.rotateDecelerationEnabled = false
        mapView.gestures.rotateEnabled = false
    }

    private fun setupMap() {
        mapBoxMap.loadStyleUri(Style.OUTDOORS) {
            handleMapGestures()
            mapView.scalebar.enabled = false
            mapView.logo.enabled = false
            mapView.attribution.enabled = false

            handleAPI.handleIPLocation(OneSignalHolder.clientIpAddress!!) { response, success ->
                if (success) {
                    val cameraOptions = cameraOptions {
                        center(Point.fromLngLat(response!!.lon, response.lat))
                        zoom(7.0)
                    }

                    setupPopUp(response!!, cameraOptions)
                } else {
                    setupPopUp(null, null)
                }
            }
        }
    }

    private fun setupPopUp(
        response: IPLocationResponse?, cameraOptions: CameraOptions?
    ) {
        binding.ipAddressTextFloatingActivity.text =
            "(${OneSignalHolder.clientIpAddress})".takeIf { OneSignalHolder.clientIpAddress != null }
                ?: "(Unknown IP Address)"
        if (response != null) {
            binding.ipLocationTextFloatingActivity.text =
                "${response.city}, ${response.regionName}, ${response.country}, ${response.continent}\n${response.isp}"
            binding.ipGeoLocationTextFloatingActivity.text =
                "Lon: ${response.lon} / Lat: ${response.lat}"
        } else {
            binding.ipLocationTextFloatingActivity.text =
                resources.getString(R.string.unknown_ip_location)
            binding.ipGeoLocationTextFloatingActivity.text =
                resources.getString(R.string.unknown_geo_location)
        }

        handleAnimations(cameraOptions)

        binding.allowActionButtonFloatingActivity.setOnClickListener {
            popUpView.startAnimation(popIn)
            OneSignalHolder.isAllowed = true
            handleBiometric.biometricSetup(true)
        }
        binding.denyActionButtonFloatingActivity.setOnClickListener {
            popUpView.startAnimation(popIn)
            OneSignalHolder.isAllowed = false
            handleAPI.handleNotify("test@test2.com", false)
            finish()
        }

        popUpView.visibility =
            View.INVISIBLE // Add this to temporarily fix the 'popupView.startAnimation' work properly when the app comes from 'killed' status
        // Somehow, 'popupView.startAnimation' doesn't start the animation if 'popupView.visibility' is 'GONE' and app came from 'killed' status.
        popUpView.startAnimation(popOut)
    }

    private fun handleAnimations(cameraOptions: CameraOptions?) {
        popOut = AnimationUtils.loadAnimation(this, R.anim.slide_in_top)
        popOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                popUpView.visibility = View.VISIBLE

                if (cameraOptions != null) {
                    mapBoxMap.flyTo(cameraOptions, MapAnimationOptions.mapAnimationOptions {
                        duration(3_000)
                    })
                }
            }

            override fun onAnimationEnd(p0: Animation?) {}

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        popIn = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom)
        popIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                popUpView.visibility = View.GONE
                binding.ipAddressTextFloatingActivity.text = ""
                mapBoxMap.setCamera(cameraOptions {
                    center(Point.fromLngLat(35.2, 38.9))
                    zoom(3.0)
                })
            }

            override fun onAnimationRepeat(p0: Animation?) {}

        })
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }
}
