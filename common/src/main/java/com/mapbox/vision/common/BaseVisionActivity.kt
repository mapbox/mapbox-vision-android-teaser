package com.mapbox.vision.common

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.vision.mobile.core.utils.SystemInfoUtils
import com.mapbox.vision.mobile.core.utils.snapdragon.SupportedSnapdragonBoards
import com.mapbox.vision.utils.VisionLogger

abstract class BaseVisionActivity : AppCompatActivity() {

    companion object {
        public val BASE_SESSION_PATH = "${Environment.getExternalStorageDirectory().absolutePath}/MapboxVisionTelemetry"
        private const val PERMISSION_FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
        private const val PERMISSIONS_REQUEST_CODE = 123
    }

    protected abstract fun onPermissionsGranted()

    protected abstract fun setLayout()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)

        val board = SystemInfoUtils.getSnpeSupportedBoard()

        if (!SupportedSnapdragonBoards.isBoardSupported(board)) {
            val text =
                Html.fromHtml("The device is not supported, you need <b>Snapdragon-powered</b> device with <b>OpenCL</b> support, more details at <b>https://www.mapbox.com/android-docs/vision/overview/</b>")
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            VisionLogger.e(
                "NotSupportedBoard",
                "Current board is {\"$board\"}, Supported Boards: [${enumValues<SupportedSnapdragonBoards>().joinToString { it.name }}]; System Info: [${SystemInfoUtils.obtainSystemInfo()}]"
            )
            finish()
            return
        }

        setLayout()

        if (!allPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE)
        } else {
            onPermissionsGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allPermissionsGranted() && requestCode == PERMISSIONS_REQUEST_CODE) {
            onPermissionsGranted()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
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

    private fun getRequiredPermissions(): Array<String> {
        return try {
            val info = packageManager?.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val ps = info!!.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                emptyArray()
            }
        } catch (e: Exception) {
            emptyArray()
        }
    }
}
