package com.mapbox.vision.examples.utils.classification

import android.content.Context
import com.mapbox.vision.examples.models.UiSignValueModel

interface SignResourceMapper {

    fun getSignResource(uiSignValueModel: UiSignValueModel): Int
    fun getSignResourceForCurrentSpeed(uiSignValueModel: UiSignValueModel, speed: Double): Int

    class Impl(val context: Context) : SignResourceMapper {

        private val numbersTypesArray = arrayOf(
                UiSignValueModel.SignType.SpeedLimit,
                UiSignValueModel.SignType.SpeedLimitEnd,
                UiSignValueModel.SignType.SpeedLimitMin,
                UiSignValueModel.SignType.SpeedLimitNight,
                UiSignValueModel.SignType.SpeedLimitTrucks,
                UiSignValueModel.SignType.SpeedLimitComplementary,
                UiSignValueModel.SignType.SpeedLimitExit,
                UiSignValueModel.SignType.SpeedLimitRamp,
                UiSignValueModel.SignType.Mass
        )

        private val overSpeedArray = arrayOf(
                UiSignValueModel.SignType.SpeedLimit,
                UiSignValueModel.SignType.SpeedLimitNight,
                UiSignValueModel.SignType.SpeedLimitComplementary,
                UiSignValueModel.SignType.SpeedLimitTrucks
        )

        private fun getResourceNameForSign(uiSignValueModel: UiSignValueModel) = if (uiSignValueModel.signType in numbersTypesArray) {
            uiSignValueModel.signType.usResourceName + uiSignValueModel.signNum.value
        } else {
            uiSignValueModel.signType.usResourceName
        }

        private fun getResourceId(name: String) = context.resources.getIdentifier(
                name, "drawable", context.packageName
        ).also {
            if (it == 0) {

            }
        }

        override fun getSignResource(uiSignValueModel: UiSignValueModel) = getResourceId(
                name = getResourceNameForSign(uiSignValueModel)
        )

        override fun getSignResourceForCurrentSpeed(uiSignValueModel: UiSignValueModel, speed: Double): Int {
            val resourceName = getResourceNameForSign(uiSignValueModel)
            val fullResourceName =
                if (uiSignValueModel.signType in overSpeedArray && speed > uiSignValueModel.signNum.value) {
                    "over_$resourceName"
                } else {
                    resourceName
                }
            return getResourceId(name = fullResourceName)
        }
    }
}
