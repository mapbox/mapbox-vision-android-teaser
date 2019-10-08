package com.mapbox.vision.recorder

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.vision.VisionManager
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.Camera
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.mobile.core.utils.SystemInfoUtils
import com.mapbox.vision.mobile.core.utils.snapdragon.SupportedSnapdragonBoards
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.utils.VisionLogger
import kotlinx.android.synthetic.main.activity_recording.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
        private const val PERMISSION_FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
    }

    private var isPermissionsGranted = false
    private var visionManagerWasInit = false

    private var appModelPerformanceConfig: ModelPerformanceConfig = ModelPerformanceConfig.Merged(
        performance = ModelPerformance.Off
    )

    private val visionEventsListener = object : VisionEventsListener {

        override fun onCountryUpdated(country: Country) {
            println("Country $country")
        }

        override fun onCameraUpdated(camera: Camera) {
            runOnUiThread {
                fps_performance_view.setCalibrationProgress(camera.calibrationProgress)
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onUpdateCompleted() {
            val frameStatistics = VisionManager.getFrameStatistics()
            runOnUiThread {
                fps_performance_view.showInfo(frameStatistics, appModelPerformanceConfig)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

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

        if (!allPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE)
        } else {
            onPermissionsGranted()
        }
    }

    private fun onPermissionsGranted() {
        isPermissionsGranted = true

        root.setOnLongClickListener {
            if (fps_performance_view.visibility == View.GONE) {
                fps_performance_view.show()
            } else {
                fps_performance_view.hide()
            }
            return@setOnLongClickListener true

        }
        recording_view.setOnClickListener { toggleRecording() }
        fps_performance_view.hide()

        tryToInitVisionManager()
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

    private val BASE_SESSION_PATH = "${Environment.getExternalStorageDirectory().absolutePath}/Telemetry"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ssZ", Locale.US)

    private fun tryToInitVisionManager() {
        if (isPermissionsGranted && !visionManagerWasInit) {
            VisionManager.create()
            VisionManager.start()
            VisionManager.visionEventsListener = visionEventsListener
            VisionManager.setModelPerformanceConfig(appModelPerformanceConfig)

            vision_view.onResume()
            vision_view.setVisionManager(VisionManager)

            visionManagerWasInit = true
        }
    }

    override fun onStart() {
        super.onStart()
        tryToInitVisionManager()
    }

    private fun toggleRecording() {
        when (recording_view.state) {
            RecordingView.State.NotRecording -> {
                VisionManager.startRecording("$BASE_SESSION_PATH/${dateFormat.format(Date(System.currentTimeMillis()))}")
                recording_view.state = RecordingView.State.Recording
            }
            RecordingView.State.Recording -> {
                VisionManager.stopRecording()
                recording_view.state = RecordingView.State.NotRecording
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (isPermissionsGranted && visionManagerWasInit) {
            VisionManager.stopRecording()
            VisionManager.stop()
            VisionManager.destroy()
            vision_view.onPause()
            visionManagerWasInit = false
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

    private fun View.hide() {
        visibility = View.GONE
    }

    private fun View.show() {
        visibility = View.VISIBLE
    }
}
