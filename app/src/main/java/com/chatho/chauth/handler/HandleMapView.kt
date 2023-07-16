package com.chatho.chauth.handler

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.chatho.chauth.R
import com.chatho.chauth.api.HandleAPI
import com.chatho.chauth.api.IPLocationResponse
import com.chatho.chauth.databinding.ActivityMainBinding
import com.chatho.chauth.holder.OneSignalHolder
import com.chatho.chauth.service.IHandleOneSignal
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.gestures

class HandleMapView(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val handleAPI: HandleAPI
) : IHandleOneSignal {
    private val handleBiometric: HandleBiometric = HandleBiometric(activity, handleAPI)
    private lateinit var popOut: Animation
    private lateinit var popIn: Animation

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

    private fun setupMap() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.OUTDOORS) {
            handleMapGestures()

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
        binding.ipAddressText.text =
            "(${OneSignalHolder.clientIpAddress})".takeIf { OneSignalHolder.clientIpAddress != null }
                ?: "(Unknown IP Address)"
        if (response != null) {
            binding.ipLocationText.text =
                "${response.city}, ${response.regionName}, ${response.country}, ${response.continent}\n${response.isp}"
            binding.ipGeoLocationText.text = "Lon: ${response.lon} / Lat: ${response.lat}"
        } else {
            binding.ipLocationText.text = activity.resources.getString(R.string.unknown_ip_location)
            binding.ipGeoLocationText.text =
                activity.resources.getString(R.string.unknown_geo_location)
        }

        handleAnimations(cameraOptions)

        binding.allowActionButton.setOnClickListener {
            binding.popupView.startAnimation(popIn)
            OneSignalHolder.isAllowed = true
            handleBiometric.biometricSetup()
        }
        binding.denyActionButton.setOnClickListener {
            binding.popupView.startAnimation(popIn)
            OneSignalHolder.isAllowed = false
            handleAPI.handleNotify("test@test2.com", false)
        }

        binding.popupView.visibility =
            View.INVISIBLE // Add this to temporarily fix the 'popupView.startAnimation' work properly when the app comes from 'killed' status
        // Somehow, 'popupView.startAnimation' doesn't start the animation if 'popupView.visibility' is 'GONE' and app came from 'killed' status.
        binding.popupView.startAnimation(popOut)
    }

    private fun handleAnimations(cameraOptions: CameraOptions?) {
        popOut = AnimationUtils.loadAnimation(activity, R.anim.appear)
        popOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                binding.popupView.visibility = View.VISIBLE
                binding.enableQrScanButton.isEnabled = false

                binding.mapView.getMapboxMap()
                    .flyTo(cameraOptions!!, MapAnimationOptions.mapAnimationOptions {
                        duration(3_000)
                    })
            }

            override fun onAnimationEnd(p0: Animation?) {}

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        popIn = AnimationUtils.loadAnimation(activity, R.anim.disappear)
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

    override fun handleOneSignalCallback() {
        if (OneSignalHolder.isAllowed == null) {
            if (OneSignalHolder.clientIpAddress != null) {
                setupMap()
            } else {
                setupPopUp(null, null)
            }
        } else {
            handleBiometric.biometricSetup()
        }
    }
}