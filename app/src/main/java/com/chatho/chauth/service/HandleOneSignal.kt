package com.chatho.chauth.service

import com.chatho.chauth.holder.OneSignalHolder
import com.onesignal.OneSignal

class HandleOneSignal(
    private val handleOneSignalCallback: () -> Unit,
) {
    fun setup() {
        OneSignal.setNotificationOpenedHandler {
            OneSignalHolder.clientIpAddress =
                it.notification.additionalData.get("ip_address") as String?
            OneSignalHolder.backendBuildType =
                it.notification.additionalData.get("build_type") as String?

            when (it.action.actionId) {
                "auth_allow" -> {
                    OneSignalHolder.isAllowed = true
                }

                "auth_deny" -> {
                    OneSignalHolder.isAllowed = false
                }
            }

            handleOneSignalCallback()
        }
    }
}