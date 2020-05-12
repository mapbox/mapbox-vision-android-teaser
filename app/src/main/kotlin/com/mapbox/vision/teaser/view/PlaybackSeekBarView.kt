package com.mapbox.vision.teaser.view

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import com.mapbox.vision.teaser.R
import kotlinx.android.synthetic.main.view_playback_seek_bar.view.*

class PlaybackSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_playback_seek_bar, this, true)
    }

    var onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener? = null
        get() = field
        set(value) {
            seek_bar.setOnSeekBarChangeListener(value)
        }

    fun setProgress(seconds: Float) {
        playback_position_time_text.text = DateUtils.formatElapsedTime(seconds.toLong())
        seek_bar.progress = seconds.toInt()
    }

    fun setDuration(seconds: Float) {
        playback_duration_time_text.text = DateUtils.formatElapsedTime(seconds.toLong())
        seek_bar.max = seconds.toInt()
    }
}
