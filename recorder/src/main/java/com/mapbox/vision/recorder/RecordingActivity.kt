package com.mapbox.vision.recorder

import android.annotation.SuppressLint
import android.os.Environment
import android.view.View
import com.mapbox.vision.VisionManager
import com.mapbox.vision.common.BaseActivity
import com.mapbox.vision.common.view.hide
import com.mapbox.vision.common.view.show
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.Camera
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.android.synthetic.main.activity_recording.*

class RecordingActivity : BaseActivity() {

    companion object {
        private val BASE_SESSION_PATH = "${Environment.getExternalStorageDirectory().absolutePath}/Telemetry"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ssZ", Locale.US)
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

    override fun initViews() {
        setContentView(R.layout.activity_recording)
    }

    override fun onPermissionsGranted() {
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
}
