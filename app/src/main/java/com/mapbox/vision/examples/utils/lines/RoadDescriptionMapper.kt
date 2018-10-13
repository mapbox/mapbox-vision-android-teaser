package com.mapbox.vision.examples.utils.lines

import com.mapbox.vision.examples.R
import com.mapbox.vision.visionevents.events.roaddescription.RoadDescription

interface RoadDescriptionMapper {

    fun getRoadElementListByRoadDescription(roadDescription: RoadDescription): List<RoadElement>

    enum class RoadElement(val drawableResourceId: Int) {
        SOLID_LINE(R.drawable.ic_separator_lane),
        INTERMITTENT_LINE(R.drawable.ic_half_lane),
        NOT_SURE(R.drawable.ic_unknown_lane),
        LINE_FORWARD(R.drawable.ic_arrow_forward),
        LINE_BACK(R.drawable.ic_arrow),
        YOUR_DIRECTION(R.drawable.ic_blue_arrow),
    }
}
