package com.mapbox.vision.examples.utils.lines

import com.mapbox.vision.visionevents.events.roaddescription.RoadDescription

class RoadDescriptionMapperImpl : RoadDescriptionMapper {

    override fun getRoadElementListByRoadDescription(roadDescription: RoadDescription): List<RoadDescriptionMapper.RoadElement> {
//        val elements: ArrayList<RoadDescriptionMapper.RoadElement> = ArrayList()
//        if(!roadDescription.isValid) {
//            return elements
//        }
//
//        elements.add(if (roadDescription.seeLeftBorder) {
//            RoadDescriptionMapper.RoadElement.SOLID_LINE
//        } else {
//            RoadDescriptionMapper.RoadElement.NOT_SURE
//        })
//
//
//        for (i in 0 until roadDescription.visibleRevLanes) {
//            elements.add(RoadDescriptionMapper.RoadElement.LINE_BACK)
//
//            if (i == roadDescription.visibleRevLanes - 1) {
//                elements.add(RoadDescriptionMapper.RoadElement.SOLID_LINE)
//            } else {
//                elements.add(RoadDescriptionMapper.RoadElement.INTERMITTENT_LINE)
//            }
//        }
//
//        for (j in 0 until roadDescription.visibleLeftLanes) {
//            elements.add(RoadDescriptionMapper.RoadElement.LINE_FORWARD)
//            elements.add(RoadDescriptionMapper.RoadElement.INTERMITTENT_LINE)
//        }
//
//        elements.add(RoadDescriptionMapper.RoadElement.YOUR_DIRECTION)
//
//        for (k in 0 until roadDescription.visibleRightLanes) {
//            elements.add(RoadDescriptionMapper.RoadElement.INTERMITTENT_LINE)
//            elements.add(RoadDescriptionMapper.RoadElement.LINE_FORWARD)
//        }
//
//        elements.add(if (roadDescription.seeRightBorder) {
//            RoadDescriptionMapper.RoadElement.SOLID_LINE
//        } else {
//            RoadDescriptionMapper.RoadElement.NOT_SURE
//        })
//
//        Log.e("JNI_Core_Wrapper", elements.joinToString { it.name + " " });

        return emptyList()
    }
}
