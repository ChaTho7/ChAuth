package com.chatho.chauth.service

import androidx.activity.ComponentActivity
import com.chatho.chauth.BuildConfig
import com.onesignal.OneSignal

class HandleOneSignal(
    private val activity: ComponentActivity,
    private val handleOneSignalCallback: () -> Unit,
) {
    var isAllowed : Boolean? = null
    var clientIpAddress : String? = null

    companion object {
        const val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID
        const val EXTERNAL_USER_ID = "chauthtest"
    }

    fun setup() {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)

        OneSignal.initWithContext(activity)
        OneSignal.setAppId(ONESIGNAL_APP_ID)
        OneSignal.setExternalUserId(EXTERNAL_USER_ID)

        OneSignal.setNotificationOpenedHandler {
            clientIpAddress = it.notification.additionalData.get("ip_address") as String?
            when (it.action.actionId) {
                "auth_allow" -> {
                    isAllowed = true
                    handleOneSignalCallback()
                }

                "auth_deny" -> {
                    isAllowed = false
                    handleOneSignalCallback()
                }

                else -> handleOneSignalCallback()
            }
        }

        OneSignal.promptForPushNotifications();
    }
}