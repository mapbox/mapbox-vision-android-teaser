package com.mapbox.vision.teaser.utils

import androidx.fragment.app.Fragment
import com.mapbox.vision.VisionManager
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.manager.BaseVisionManager
import com.mapbox.vision.teaser.MainActivity
import java.lang.IllegalStateException

fun Fragment.requireVisionManager(): BaseVisionManager? {
    val activity = requireActivity()
    if (activity is MainActivity) {
        return if (activity.isCameraMode()) {
            VisionManager
        } else {
            VisionReplayManager
        }
    }
    throw IllegalStateException("Fragment should be created inside MainActivity")
}

fun Fragment.runOnUiThreadIfResumed(action: () -> Unit) {
    requireActivity().runOnUiThread {
        if (isResumed) {
            action.invoke()
        }
    }
}

fun Fragment.requireMainActivity() = requireActivity() as? MainActivity
