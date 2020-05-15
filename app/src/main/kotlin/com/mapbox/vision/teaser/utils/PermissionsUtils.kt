package com.mapbox.vision.teaser.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mapbox.vision.teaser.MainActivity


private const val PERMISSION_FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
private const val PERMISSIONS_REQUEST_CODE = 123

fun MainActivity.requestPermissionsIfNotGranted(): Boolean {
    if (!allPermissionsGranted(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.requestPermissions(getRequiredPermissions(this), PERMISSIONS_REQUEST_CODE)
        return true
    }
    return false
}

fun MainActivity.allPermissionsGrantedByRequest(requestCode: Int) = allPermissionsGranted(this) && requestCode == PERMISSIONS_REQUEST_CODE

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
