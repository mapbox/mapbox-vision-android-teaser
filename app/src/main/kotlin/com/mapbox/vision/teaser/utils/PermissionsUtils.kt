package com.mapbox.vision.teaser.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionsUtils {

    private const val PERMISSION_FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
    private const val PERMISSIONS_REQUEST_CODE = 123

    fun requestPermissionsIfNotGranted(activity: Activity): Boolean {
        if (!allPermissionsGranted(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(getRequiredPermissions(activity), PERMISSIONS_REQUEST_CODE)
            return true
        }
        return false
    }

    private fun allPermissionsGranted(activity: Activity): Boolean {
        for (permission in getRequiredPermissions(activity)) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                // PERMISSION_FOREGROUND_SERVICE was added for targetSdkVersion >= 28, it is normal and always granted, but should be added to the Manifest file
                // on devices with Android < P(9) checkSelfPermission(PERMISSION_FOREGROUND_SERVICE) can return PERMISSION_DENIED, but in fact it is GRANTED, so skip it
                // https://developer.android.com/guide/components/services#Foreground
                if (permission == PERMISSION_FOREGROUND_SERVICE) {
                    continue
                }
                return false
            }
        }
        return true
    }

    private fun getRequiredPermissions(activity: Activity): Array<String> {
        return try {
            val info = activity.packageManager?.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
            val permissions = info!!.requestedPermissions
            if (permissions != null && permissions.isNotEmpty()) {
                permissions
            } else {
                emptyArray()
            }
        } catch (e: Exception) {
            emptyArray()
        }
    }

    fun allPermissionsGrantedByRequest(activity: Activity, requestCode: Int)
            = allPermissionsGranted(activity) && requestCode == PERMISSIONS_REQUEST_CODE

}