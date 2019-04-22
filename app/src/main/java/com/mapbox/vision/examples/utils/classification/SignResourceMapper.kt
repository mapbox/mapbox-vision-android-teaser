package com.mapbox.vision.examples.utils.classification

import android.content.Context
import com.mapbox.vision.examples.models.UiSign

interface SignResourceMapper {

    fun getSignResource(uiSign: UiSign): Int
    fun getSignResourceForCurrentSpeed(uiSign: UiSign, speed: Float): Int

    class Impl(val context: Context) : SignResourceMapper {

        private val numbersTypesArray = arrayOf(
                UiSign.SignType.SpeedLimit,
                UiSign.SignType.SpeedLimitEnd,
                UiSign.SignType.SpeedLimitMin,
                UiSign.SignType.SpeedLimitNight,
                UiSign.SignType.SpeedLimitTrucks,
                UiSign.SignType.SpeedLimitComplementary,
                UiSign.SignType.SpeedLimitExit,
                UiSign.SignType.SpeedLimitRamp,
                UiSign.SignType.Mass
        )

        private val overSpeedArray = arrayOf(
                UiSign.SignType.SpeedLimit,
                UiSign.SignType.SpeedLimitNight,
                UiSign.SignType.SpeedLimitComplementary,
                UiSign.SignType.SpeedLimitTrucks
        )

        private fun getResourceNameForSign(uiSign: UiSign) = if (uiSign.signType in numbersTypesArray) {
            uiSign.signType.usResourceName + uiSign.signNum.value
        } else {
            uiSign.signType.usResourceName
        }

        private fun getResourceId(name: String) = context.resources.getIdentifier(
                name, "drawable", context.packageName
        ).also {
            if (it == 0) {

            }
        }

        override fun getSignResource(uiSign: UiSign) = getResourceId(
                name = getResourceNameForSign(uiSign)
        )

        override fun getSignResourceForCurrentSpeed(uiSign: UiSign, speed: Float): Int {
            val resourceName = getResourceNameForSign(uiSign)
            val fullResourceName =
                if (uiSign.signType in overSpeedArray && speed > uiSign.signNum.value) {
                    "over_$resourceName"
                } else {
                    resourceName
                }
            return getResourceId(name = fullResourceName)
        }
    }
}
