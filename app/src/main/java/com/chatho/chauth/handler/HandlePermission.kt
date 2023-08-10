package com.chatho.chauth.handler

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList

class HandlePermission(private val activity: ComponentActivity) {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUESTS = 1

        private val REQUIRED_RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(activity, it)) {
                    return false
                }
            }
        }
        return true
    }

    fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(activity, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity, permissionsToRequest.toTypedArray(), PERMISSION_REQUESTS
            )
        }

        getOverlayPermission()
    }

    fun getOverlayPermission(): Boolean {
        return if (!Settings.canDrawOverlays(activity)) {
            requestOverlayDisplayPermission()

            false
        } else {
            true
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    private fun requestOverlayDisplayPermission() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)

        builder.setCancelable(true)
        builder.setTitle("Screen Overlay Permission Needed")
        builder.setMessage("Enable 'Display over other apps' from System Settings.")
        builder.setPositiveButton(
            "Open Settings"
        ) { dialog, which ->
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )

            activity.startActivityForResult(intent, AppCompatActivity.RESULT_OK)
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}