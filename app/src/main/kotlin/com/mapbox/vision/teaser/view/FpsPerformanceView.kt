package com.mapbox.vision.teaser.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.mapbox.vision.mobile.core.models.FrameStatistics
import com.mapbox.vision.teaser.R
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
    private var sumCoreUpdatesFps = 0f

    private var countSegmentationDetectionFps = 0L
    private var countCoreUpdatesFps = 0L

    init {
        LayoutInflater.from(context).inflate(R.layout.view_fps_performance, this, true)
    }

    @SuppressLint("SetTextI18n")
    fun showInfo(frameStatistics: FrameStatistics) {
        with(frameStatistics) {
            if (segmentationDetectionFps > 0) {
                sumSegmentationDetectionFps += segmentationDetectionFps
                merge_model_fps.text =
                    "MM: ${segmentationDetectionFps.round()}  AVG: ${(sumSegmentationDetectionFps / ++countSegmentationDetectionFps).round()}"
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
        sumCoreUpdatesFps = 0f

        countSegmentationDetectionFps = 0L
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
