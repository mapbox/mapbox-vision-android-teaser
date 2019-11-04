package com.mapbox.vision.common.utils

import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.vision.ar.core.models.ManeuverType

fun String?.mapToManeuverType(): ManeuverType = when(this) {
    "turn" -> ManeuverType.Turn
    "depart" -> ManeuverType.Depart
    "arrive" -> ManeuverType.Arrive
    "merge" -> ManeuverType.Merge
    "on ramp" -> ManeuverType.OnRamp
    "off ramp" -> ManeuverType.OffRamp
    "fork" -> ManeuverType.Fork
    "roundabout" -> ManeuverType.Roundabout
    "exit roundabout" -> ManeuverType.RoundaboutExit
    "end of road" -> ManeuverType.EndOfRoad
    "new name" -> ManeuverType.NewName
    "continue" -> ManeuverType.Continue
    "rotary" -> ManeuverType.Rotary
    "roundabout turn" -> ManeuverType.RoundaboutTurn
    "notification" -> ManeuverType.Notification
    "exit rotary" -> ManeuverType.RoundaboutExit
    else -> ManeuverType.None
}

fun String.buildStepPointsFromGeometry(): List<Point> {
    return PolylineUtils.decode(this, Constants.PRECISION_6)
}
