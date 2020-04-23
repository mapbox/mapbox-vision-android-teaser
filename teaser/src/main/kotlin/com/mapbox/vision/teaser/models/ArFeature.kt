package com.mapbox.vision.teaser.models

import com.mapbox.vision.teaser.R

sealed class ArFeature(
    val drawableId: Int,
    val isLaneVisible: Boolean,
    val isFenceVisible: Boolean
) {

    abstract fun getNextFeature(): ArFeature

    object Lane : ArFeature(
        drawableId = R.drawable.ar_mode_lane,
        isLaneVisible = true,
        isFenceVisible = false
    ) {
        override fun getNextFeature() = Fence
    }

    object Fence : ArFeature(
        drawableId = R.drawable.ar_mode_fence,
        isLaneVisible = false,
        isFenceVisible = true
    ) {
        override fun getNextFeature() = LaneAndFence
    }

    object LaneAndFence : ArFeature(
        drawableId = R.drawable.ar_mode_lane_fence,
        isLaneVisible = true,
        isFenceVisible = true
    ) {
        override fun getNextFeature() = Lane
    }
}
