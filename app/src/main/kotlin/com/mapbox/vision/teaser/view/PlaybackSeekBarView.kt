package com.mapbox.vision.teaser.view

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
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

    fun setTimePosition(seconds: Float) {
        video_position_time_text.text = DateUtils.formatElapsedTime(seconds.toLong())
    }

    fun setDuration(seconds: Float) {
        video_duration_time_text.text = DateUtils.formatElapsedTime(seconds.toLong())
    }
}
