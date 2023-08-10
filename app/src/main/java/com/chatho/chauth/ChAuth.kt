package com.chatho.chauth

import android.app.Application
import com.mapbox.maps.ResourceOptionsManager
import com.onesignal.OneSignal

class ChAuth : Application() {
    override fun onCreate() {
        super.onCreate()

        // OneSignal
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)
        OneSignal.setExternalUserId(EXTERNAL_USER_ID)
        OneSignal.promptForPushNotifications()

        // MapBox
        ResourceOptionsManager.getDefault(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
    }

    companion object {
        private const val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID
        private const val EXTERNAL_USER_ID = "chauthtest"
    }
}