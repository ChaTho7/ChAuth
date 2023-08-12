package com.chatho.chauth.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.chatho.chauth.R
import com.chatho.chauth.api.HandleAPI
import com.chatho.chauth.api.IPLocationResponse
import com.chatho.chauth.databinding.WindowFloatingBinding
import com.chatho.chauth.holder.OneSignalHolder
import com.chatho.chauth.util.runInCoroutineScope
import com.chatho.chauth.view.MainActivity
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
import kotlinx.coroutines.Dispatchers
import java.util.Timer
import java.util.TimerTask

class FloatingWindow : Service(), LifecycleOwner {
    private lateinit var binding: WindowFloatingBinding
    private lateinit var handleAPI: HandleAPI
    private var floatWindowLayoutParam: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null
    override val lifecycle: Lifecycle
        get() = LifecycleRegistry(this)

    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap
    private lateinit var popUpView: CardView
    private lateinit var closeButton: ImageView
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.context.setTheme(R.style.Theme_ChAuth)
        binding = WindowFloatingBinding.inflate(inflater)

        handleAPI = HandleAPI(applicationContext)

        LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        floatWindowLayoutParam = WindowManager.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT,
            LAYOUT_TYPE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        mapView = binding.mapViewFloatingWindow
        mapBoxMap = mapView.getMapboxMap()
        popUpView = binding.popupViewFloatingWindow
        closeButton = binding.closeFloatingWindowButton

        mapView.setViewTreeLifecycleOwner(object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() = this@FloatingWindow.lifecycle
        })
        handleAnimations()

        binding.root.setOnClickListener {
            if (closeButton.visibility != View.VISIBLE) {
                closeButton.visibility = View.INVISIBLE
                closeButton.startAnimation(fadeIn)
            }
        }

        if (OneSignalHolder.isAllowed == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager!!.addView(binding.root, floatWindowLayoutParam)

            if (OneSignalHolder.clientIpAddress != null) {
                setupMap()
            } else {
                setupPopUp(null, null)
            }
        } else if (OneSignalHolder.isAllowed == false) {
            handleAPI.handleNotify("test@test2.com", false)
        } else {
            maximazeBack(true)
        }
    }

    private fun handleMapGestures() {
        mapView.gestures.apply {
            scrollDecelerationEnabled = false
            scrollEnabled = false
            pinchScrollEnabled = false
            doubleTouchToZoomOutEnabled = false
            doubleTapToZoomInEnabled = false
            quickZoomEnabled = false
            pinchToZoomEnabled = false
            pitchEnabled = false
            simultaneousRotateAndPinchToZoomEnabled = false
            pinchToZoomDecelerationEnabled = false
            rotateDecelerationEnabled = false
            rotateEnabled = false
        }
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
        binding.ipAddressTextFloatingWindow.text =
            "(${OneSignalHolder.clientIpAddress})".takeIf { OneSignalHolder.clientIpAddress != null }
                ?: "(Unknown IP Address)"
        if (response != null) {
            binding.ipLocationTextFloatingWindow.text =
                "${response.city}, ${response.regionName}, ${response.country}, ${response.continent}\n${response.isp}"
            binding.ipGeoLocationTextFloatingWindow.text =
                "Lon: ${response.lon} / Lat: ${response.lat}"
        } else {
            binding.ipLocationTextFloatingWindow.text =
                resources.getString(R.string.unknown_ip_location)
            binding.ipGeoLocationTextFloatingWindow.text =
                resources.getString(R.string.unknown_geo_location)
        }

        binding.allowActionButtonFloatingWindow.setOnClickListener {
            OneSignalHolder.isAllowed = true
            maximazeBack(true)
        }
        binding.denyActionButtonFloatingWindow.setOnClickListener {
            OneSignalHolder.isAllowed = false
            handleAPI.handleNotify("test@test2.com", false)
            destroyService()
        }

        // Somehow, icon not render. Set visibility 'GONE' in layout and set here 'VISIBLE' and set image source again.
        binding.loginIconFloatingWindow.visibility = View.VISIBLE
        binding.loginIconFloatingWindow.setImageResource(R.drawable.login_icon)

        if (cameraOptions != null) {
            mapBoxMap.flyTo(cameraOptions, MapAnimationOptions.mapAnimationOptions {
                duration(3_000)
            })
        } else {
            val density = resources.displayMetrics.density
            val newTopMarginInDp = 20
            val newTopMarginInPixels = (newTopMarginInDp * density).toInt()

            val allowActionButtonLayoutParams =
                binding.allowActionButtonFloatingWindow.layoutParams as ConstraintLayout.LayoutParams
            val denyActionButtonLayoutParams =
                binding.denyActionButtonFloatingWindow.layoutParams as ConstraintLayout.LayoutParams
            allowActionButtonLayoutParams.topMargin = newTopMarginInPixels
            denyActionButtonLayoutParams.topMargin = newTopMarginInPixels

            binding.allowActionButtonFloatingWindow.layoutParams = allowActionButtonLayoutParams
            binding.denyActionButtonFloatingWindow.layoutParams = denyActionButtonLayoutParams

            mapView.visibility = View.GONE
        }
    }

    private fun handleAnimations() {
        fadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in_float_window)
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                closeButton.visibility = View.VISIBLE
                closeButton.setImageResource(R.drawable.close)
                closeButton.setOnClickListener {
                    OneSignalHolder.isAllowed = false
                    handleAPI.handleNotify("test@test2.com", false)
                    maximazeBack(false)
                }
            }

            override fun onAnimationEnd(p0: Animation?) {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        closeButton.startAnimation(fadeOut)
                    }

                }, 2000)
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })
        fadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out_float_window)
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                runInCoroutineScope(Dispatchers.Main) {
                    closeButton.visibility = View.GONE
                }
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })
    }

    private fun maximazeBack(needBiometric: Boolean) {
        destroyService()

        val backToHome = Intent(this, MainActivity::class.java)
        backToHome.putExtra("needBiometric", needBiometric)
        backToHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ContextCompat.startActivity(applicationContext, backToHome, null)
    }

    private fun destroyService() {
        stopSelf()
        windowManager?.removeView(binding.root)
    }

    // Use this to destroy service when app killed. (Otherwise, it doesn't destroy service being app killing.)
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        destroyService()
    }

    override fun onDestroy() {
        super.onDestroy()

        destroyService()
    }

    companion object {
        private var LAYOUT_TYPE = 0
        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager?

            for (service in manager!!.getRunningServices(Int.MAX_VALUE)) {
                if (FloatingWindow::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
}