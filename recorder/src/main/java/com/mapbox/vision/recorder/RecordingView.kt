package com.mapbox.vision.recorder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_recording.view.*

class RecordingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    companion object {
    }

    enum class State(@DrawableRes val imageId: Int, @StringRes val textId : Int) {
        Recording(
            imageId = R.drawable.ic_stop_recording,
            textId = R.string.stop_recording
        ),
        NotRecording(
            imageId = R.drawable.ic_start_recording,
            textId = R.string.start_recording
        )
    }

    var state : State =
        State.NotRecording
        set(value) {
            field = value
            recording_text.setText(value.textId)
            recording_image.setImageResource(value.imageId)
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_recording, this, true)
        state = State.NotRecording
    }
}