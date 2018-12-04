package com.mapbox.vision.examples.utils.classification

import android.content.Context
import com.mapbox.vision.examples.models.UiSignValueModel

interface SignResourceMapper {

    fun getResourceByValue(uiSignValueModel: UiSignValueModel, speed: Double = 0.0): Int

    class Impl(val context: Context) : SignResourceMapper {

        private val numbersTypesArray = arrayOf(
                UiSignValueModel.SignType.SpeedLimit,
                UiSignValueModel.SignType.SpeedLimitEnd,
                UiSignValueModel.SignType.SpeedLimitMin,
                UiSignValueModel.SignType.SpeedLimitNight,
                UiSignValueModel.SignType.SpeedLimitTrucks,
                UiSignValueModel.SignType.SpeedLimitComplementary,
                UiSignValueModel.SignType.SpeedLimitExit,
                UiSignValueModel.SignType.SpeedLimitRamp
        )

        private val overSpeedArray = arrayOf(
                UiSignValueModel.SignType.SpeedLimit,
                UiSignValueModel.SignType.SpeedLimitNight,
                UiSignValueModel.SignType.SpeedLimitComplementary,
                UiSignValueModel.SignType.SpeedLimitTrucks
        )

        override fun getResourceByValue(uiSignValueModel: UiSignValueModel, speed: Double): Int {
            val resourceName: String = if (uiSignValueModel.signType in numbersTypesArray) {
                if (uiSignValueModel.signType in overSpeedArray && speed > uiSignValueModel.signNum.value) {
                    "over_" + uiSignValueModel.signType.resourceName + uiSignValueModel.signNum.value
                } else {
                    uiSignValueModel.signType.resourceName + uiSignValueModel.signNum.value
                }
            } else {
                uiSignValueModel.signType.resourceName
            }
            return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
    }
}
