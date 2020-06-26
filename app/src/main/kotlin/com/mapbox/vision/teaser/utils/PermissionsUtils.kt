package com.mapbox.vision.teaser.utils

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mapbox.vision.teaser.MainActivity

object PermissionsUtils {

    // PERMISSION_FOREGROUND_SERVICE was added for targetSdkVersion >= 28, it is normal and always granted, but should be added to the Manifest file
    // on devices with Android < P(9) checkSelfPermission(PERMISSION_FOREGROUND_SERVICE) can return PERMISSION_DENIED, but in fact it is GRANTED, so skip it
    // https://developer.android.com/guide/components/services#Foreground
    private const val PERMISSION_FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"

    // required by firebase dependencies, optional
    private const val PERMISSION_C2M = "com.google.android.c2dm.permission.RECEIVE"

    // required by firebase dependencies, optional
    private const val PERMISSION_REFERRER = "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE"

    private const val PERMISSIONS_REQUEST_CODE = 123

    fun requestPermissions(activity: MainActivity): Boolean {
        val notGranted = getNotGrantedPermissions(activity)
        if (notGranted.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(notGranted, PERMISSIONS_REQUEST_CODE)
            return true
        }
        return false
    }

    fun arePermissionsGranted(activity: MainActivity, requestCode: Int) =
        requestCode == PERMISSIONS_REQUEST_CODE &&
        getNotGrantedPermissions(activity).none { permission ->
            // these permissions are optional
            permission != PERMISSION_FOREGROUND_SERVICE &&
            permission != PERMISSION_C2M &&
            permission != PERMISSION_REFERRER
        }

    fun getNotGrantedPermissions(activity: MainActivity): Array<String> =
        getRequiredPermissions(activity)
            .filter { permission ->
                ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
            }
            .toTypedArray()

    private fun getRequiredPermissions(activity: MainActivity): Array<String> = try {
        val info = activity.packageManager?.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info?.requestedPermissions
        if (permissions != null && permissions.isNotEmpty()) {
            permissions
        } else {
            emptyArray()
        }
    } catch (e: Exception) {
        emptyArray()
    }
}
