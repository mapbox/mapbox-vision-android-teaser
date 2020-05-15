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
    return null
}

fun Fragment.runOnUiThreadIfPossible(action: () -> Unit) {
    val activity = requireActivity()
    if (activity is MainActivity) {
        activity.runOnUiThreadIfPossible(action)
    } else {
        throw IllegalStateException("MainActivity should be parent")
    }
}

fun Fragment.requireMainActivity() = requireActivity() as? MainActivity