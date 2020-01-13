package com.mapbox.vision.common.models

sealed class ArFeature {

    open val caption: String = ""
    open val isLaneVisible = true
    open val isFenceVisible = false

    abstract fun getNextFeature(): ArFeature

    object Lane : ArFeature() {
        override val caption = "Lane: On\nFence: Off"
        override fun getNextFeature() = Fence
        override val isLaneVisible = true
        override val isFenceVisible = false
    }

    object Fence : ArFeature() {
        override val caption = "Lane: Off\nFence: On"
        override fun getNextFeature() = LaneAndFence
        override val isLaneVisible = false
        override val isFenceVisible = true
    }

    object LaneAndFence : ArFeature() {
        override val caption = "Lane: On\nFence: On"
        override fun getNextFeature() = Lane
        override val isLaneVisible = true
        override val isFenceVisible = true
    }
}
