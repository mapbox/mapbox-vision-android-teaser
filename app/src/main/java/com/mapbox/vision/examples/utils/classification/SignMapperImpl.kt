package com.mapbox.vision.examples.utils.classification

import android.content.Context
import com.mapbox.vision.examples.models.UiSignValueModel

class SignMapperImpl(val context: Context) : SignMapper {

    private val numbersTypesArray = arrayOf(
            UiSignValueModel.SignType.SpeedLimit,
            UiSignValueModel.SignType.SpeedLimitEnd,
            UiSignValueModel.SignType.SpeedLimitMin,
            UiSignValueModel.SignType.SpeedLimitNight,
            UiSignValueModel.SignType.SpeedLimitTrucks,
            UiSignValueModel.SignType.SpeedLimitExit,
            UiSignValueModel.SignType.SpeedLimitRamp
    )

    override fun getResourceByValue(uiSignValueModel: UiSignValueModel): Int {
        val indicator: String = if (uiSignValueModel.signType in numbersTypesArray) {
            uiSignValueModel.signType.resourceName + uiSignValueModel.signNum.resourcePostfix
        } else {
            uiSignValueModel.signType.resourceName
        }
        return context.resources.getIdentifier(indicator, "drawable", context.packageName)
    }
}
