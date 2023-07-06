package com.chatho.chauth.service

import androidx.activity.ComponentActivity
import com.chatho.chauth.BuildConfig
import com.chatho.chauth.api.HandleAPI
import com.chatho.chauth.holder.OneSignalHolder
import com.onesignal.OneSignal

class HandleOneSignal(
    private val activity: ComponentActivity,
    private val handleOneSignalCallback: () -> Unit,
) {
    private val handleAPI = HandleAPI(activity)

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
            OneSignalHolder.clientIpAddress =
                it.notification.additionalData.get("ip_address") as String?
            OneSignalHolder.backendBuildType =
                it.notification.additionalData.get("build_type") as String?

            when (it.action.actionId) {
                "auth_allow" -> {
                    OneSignalHolder.isAllowed = true
                    handleOneSignalCallback()
                }

                "auth_deny" -> {
                    OneSignalHolder.isAllowed = false
                    handleAPI.handleNotify("test@test2.com", false) {
                        OneSignalHolder.isAllowed = null
                        OneSignalHolder.clientIpAddress = null
                    }
                }

                else -> handleOneSignalCallback()
            }
        }

        OneSignal.promptForPushNotifications();
    }
}