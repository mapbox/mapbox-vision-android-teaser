package com.mapbox.vision.common.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.mabpox.vision.teaser.common.R
import com.mapbox.vision.mobile.core.models.FrameStatistics
import com.mapbox.vision.performance.ModelPerformanceConfig
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.view_fps_performance.view.*

class FpsPerformanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    companion object {
        private const val SESSION_TIME_FORMAT = "Duration: %02d:%02d:%02d"
    }

    private var sumSegmentationDetectionFps = 0f
    private var sumSegmentationFps = 0f
    private var sumDetectionFps = 0f
    private var sumCoreUpdatesFps = 0f

    private var countSegmentationDetectionFps = 0L
    private var countSegmentationFps = 0L
    private var countDetectionFps = 0L
    private var countCoreUpdatesFps = 0L

    init {
        LayoutInflater.from(context).inflate(R.layout.view_fps_performance, this, true)
    }

    @SuppressLint("SetTextI18n")
    fun showInfo(frameStatistics: FrameStatistics, appModelPerformanceConfig: ModelPerformanceConfig) {
        with(frameStatistics) {
            when (appModelPerformanceConfig) {
                is ModelPerformanceConfig.Merged -> {
                    segmentation_fps.hide()
                    detection_fps.hide()
                    merge_model_fps.show()

                    if (segmentationDetectionFps > 0) {
                        sumSegmentationDetectionFps += segmentationDetectionFps
                        merge_model_fps.text =
                            "MM: ${segmentationDetectionFps.round()}  AVG: ${(sumSegmentationDetectionFps / ++countSegmentationDetectionFps).round()}"
                    }
                }
                is ModelPerformanceConfig.Separate -> {
                    segmentation_fps.show()
                    detection_fps.show()
                    merge_model_fps.hide()

                    if (segmentationFps > 0) {
                        sumSegmentationFps += segmentationFps
                        segmentation_fps.text =
                            "S: ${segmentationFps.round()}  AVG: ${(sumSegmentationFps / ++countSegmentationFps).round()}"
                    }

                    if (detectionFps > 0) {
                        sumDetectionFps += detectionFps
                        detection_fps.text =
                            "D: ${detectionFps.round()}  AVG: ${(sumDetectionFps / ++countDetectionFps).round()}"
                    }
                }
            }

            if (coreUpdateFps > 0) {
                sumCoreUpdatesFps += coreUpdateFps
                core_update_fps.text =
                    "CU: ${coreUpdateFps.round()}  AVG: ${(sumCoreUpdatesFps / ++countCoreUpdatesFps).round()}"
            }
        }
    }

    fun resetAverageFps() {
        sumSegmentationDetectionFps = 0f
        sumSegmentationFps = 0f
        sumDetectionFps = 0f
        sumCoreUpdatesFps = 0f

        countSegmentationDetectionFps = 0L
        countSegmentationFps = 0L
        countDetectionFps = 0L
        countCoreUpdatesFps = 0L
    }

    fun setCalibrationProgress(calibrationProgress: Float) {
        calibration_progress.text = context.getString(
            R.string.calibration_progress,
            (calibrationProgress * 100).toInt()
        )
    }

    fun setTimestamp(timestamp: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(timestamp) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timestamp) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp) % 60
        session_time.text = String.format(SESSION_TIME_FORMAT, hours, minutes, seconds)
    }

    private fun Float.round() = String.format("%.2f", this)
}
