package com.mapbox.vision.examples.utils.classification

import android.content.Context
import com.mapbox.vision.examples.models.UiSign
import com.mapbox.vision.examples.utils.ResourceType
import com.mapbox.vision.examples.utils.getResId
import com.mapbox.vision.mobile.core.models.Country

interface SignResources {

    fun getSignResource(uiSign: UiSign, country: Country): Int
    fun getSpeedSignResource(uiSign: UiSign, speed: Float, country: Country): Int

    class Impl(val context: Context) : SignResources {

        private val signsWithNumber = arrayOf(
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

        private val speedLimitSigns = arrayOf(
            UiSign.SignType.SpeedLimit,
            UiSign.SignType.SpeedLimitNight,
            UiSign.SignType.SpeedLimitComplementary,
            UiSign.SignType.SpeedLimitTrucks
        )

        private fun getResourceNameForSign(
            uiSign: UiSign,
            country: Country
        ): String {
            val resourceName = when (country) {
                Country.Unknown, Country.USA -> uiSign.signType.usResourceName
                Country.UK, Country.Other-> uiSign.signType.ukResourceName
                Country.China -> uiSign.signType.chinaResourceName
            }

            return if (uiSign.signType in signsWithNumber) {
                resourceName + uiSign.signNum.value
            } else {
                resourceName
            }
        }

        private fun getSignResId(name: String, country: Country): Int {
            val resId = context.getResId(
                name = name,
                type = ResourceType.Drawable
            )

            return if (resId == 0) {
                context.getResId(
                    name = getResourceNameForSign(UiSign(UiSign.SignType.Unknown, UiSign.SignNumber.Unknown), country),
                    type = ResourceType.Drawable
                )
            } else {
                resId
            }
        }

        override fun getSignResource(uiSign: UiSign, country: Country) = getSignResId(
            name = getResourceNameForSign(uiSign, country),
            country = country
        )

        override fun getSpeedSignResource(uiSign: UiSign, speed: Float, country: Country): Int {
            val resourceName = getResourceNameForSign(uiSign, country)
            val fullResourceName = if (uiSign.signType in speedLimitSigns && speed > uiSign.signNum.value) {
                "over_$resourceName"
            } else {
                resourceName
            }
            return getSignResId(
                name = fullResourceName,
                country = country
            )
        }
    }
}
