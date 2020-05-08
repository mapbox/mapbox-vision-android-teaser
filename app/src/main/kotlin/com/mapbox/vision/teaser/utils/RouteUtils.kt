package com.mapbox.vision.teaser.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.vision.ar.core.models.ManeuverType
import com.mapbox.vision.ar.core.models.RoutePoint
import com.mapbox.vision.mobile.core.models.position.GeoCoordinate

fun String?.mapToManeuverType(): ManeuverType = when (this) {
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
    "exit rotary" -> ManeuverType.RotaryExit
    else -> ManeuverType.None
}

fun String.buildStepPointsFromGeometry(): List<Point> {
    return PolylineUtils.decode(this, Constants.PRECISION_6)
}

fun DirectionsRoute.getRoutePoints(): Array<RoutePoint> {
    val routePoints = arrayListOf<RoutePoint>()
    legs()?.forEach { leg ->
        leg.steps()?.forEach { step ->
            val maneuverPoint = RoutePoint(
                    GeoCoordinate(
                        latitude = step.maneuver().location().latitude(),
                        longitude = step.maneuver().location().longitude()
                    ),
                    step.maneuver().type().mapToManeuverType()
            )
            routePoints.add(maneuverPoint)

            step.geometry()
                    ?.buildStepPointsFromGeometry()
                    ?.map { geometryStep ->
                        RoutePoint(
                                GeoCoordinate(
                                    latitude = geometryStep.latitude(),
                                    longitude = geometryStep.longitude()
                                )
                        )
                    }
                    ?.let { stepPoints ->
                        routePoints.addAll(stepPoints)
                    }
        }
    }

    return routePoints.toTypedArray()
}
