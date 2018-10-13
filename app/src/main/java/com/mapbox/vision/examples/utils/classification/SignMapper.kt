package com.mapbox.vision.examples.utils.classification

import com.mapbox.vision.examples.models.UiSignValueModel

interface SignMapper {

    fun getResourceByValue(uiSignValueModel: UiSignValueModel): Int
}
