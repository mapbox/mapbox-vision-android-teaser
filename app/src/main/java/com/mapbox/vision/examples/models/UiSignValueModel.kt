package com.mapbox.vision.examples.models

import com.mapbox.vision.visionevents.events.classification.SignClassification

data class UiSignValueModel(val signType: SignType, val signNum: SignNumber) {
    // Insert new value at the bottom
    enum class SignType(val resourceName: String = "") {
        Unknown("unknown"),
        SpeedLimit("speed_limit_us_"),
        SpeedLimitEnd("speed_limit_end_us_"),
        SpeedLimitMin("speed_limit_min_us_"),
        SpeedLimitNight("speed_limit_night_us_"),
        SpeedLimitTrucks("speed_limit_trucks_us_"),
        Mass,
        Pure,
        SpeedLimitExit("warning_exit_us_"),
        SpeedLimitRamp("warning_ramp_us_"),
        Left("warning_turn_left_us"),
        Right("warning_turn_right_us"),
        Back("warning_turn_back_us"),
        RoundAbout("warning_roundabout_us"),
        Bamp("warning_speedbump_us"),
        Winding("warning_winding_road_us"),
        InformationBikeRoute("information_bike_route_g1_us"),
        InformationParking("information_parking_g1_us"),
        RegulatoryAllDirectionsPermitted("regulatory_all_directions_permitted_us"),
        RegulatoryBicyclesOnly("regulatory_bicycles_only_g4_us"),
        RegulatoryDoNotpass("regulatory_do_not_pass_g1_us"),
        RegulatoryDoNotstopOnTracks,
        RegulatoryDualLanesAllDirectionsOnRight("regulatory_dual_lanes_all_directions_on_right_g1_us"),
        RegulatoryDualLanesGoLeftOrRight("regulatory_dual_lanes_go_left_or_right_g1_us"),
        RegulatoryDualLanesGoStraightOnLeft("regulatory_dual_lanes_go_straight_on_left_g1_us"),
        RegulatoryDualLanesGoStraightOnRight("regulatory_dual_lanes_go_left_or_right_g1_us"),
        RegulatoryDualLanesTurnLeft("regulatory_dual_lanes_turn_left_g1_us"),
        RegulatoryDualLanesTurnLeftOrStraight("regulatory_dual_lanes_turn_left_or_straight_g1_us"),
        RegulatoryDualLanesTurnRightOrStraight("regulatory_dual_lanes_turn_right_or_straight_g1_us"),
        RegulatoryEndOfSchoolZone("regulatory_end_of_school_zone_g1_us"),
        RegulatoryGoStraight("regulatory_go_straight_g3_us"),
        RegulatoryGoStraightOrTurnLeft("regulatory_go_straight_or_turn_left_g2_us"),
        RegulatoryGoStraightOrTurnRight("regulatory_go_straight_or_turn_right_g2_us"),
        RegulatoryHeightLimit("regulatory_height_limit_g1_us"),
        RegulatoryLeftTurnYieldOnGreen("regulatory_left_turn_yield_on_green_us"),
        RegulatoryNoBicycles("regulatory_no_bicycles_g2_us"),
        RegulatoryNoEntry("regulatory_no_entry_g1_us"),
        RegulatoryNoLeftOrUTurn("regulatory_no_left_or_u_turn_us"),
        RegulatoryNoLeftTurn("regulatory_no_left_turn_v1_us"),
        RegulatoryNoMotorVehicles("regulatory_no_motor_vehicles_us"),
        RegulatoryNoParking("regulatory_no_parking_us"),
        RegulatoryNoParkingOrNoStopping("regulatory_no_parking_or_no_stopping_v1_us"),
        RegulatoryNoPedestrians("regulatory_no_pedestrians_us"),
        RegulatoryNoRightTurn("regulatory_no_right_turn_us"),
        RegulatoryNoStopping("regulatory_no_stopping_us"),
        RegulatoryNoStraightThrough("regulatory_no_straight_through_us"),
        RegulatoryNoUTurn("regulatory_no_u_turn_v2_us"),
        RegulatoryOneWayStraight("regulatory_one_way_straight_g1_us"),
        RegulatoryReversibleLanes("regulatory_reversible_lanes_g1_us"),
        RegulatoryRoadClosedToVehicles("regulatory_road_closed_to_vehicles_g1_us"),
        RegulatoryStop("regulatory_stop_us"),
        RegulatoryTrafficSignalPhotoEnforced("regulatory_traffic_signal_photo_enforced_us"),
        RegulatoryTripleLanesGoStraightCenterLane("regulatory_tiple_lanes_go_straight_center_lane"),
        WarningBicyclesCrossing("warning_bicycles_crossing_us"),
        WarningHeightRestriction("warning_height_restriction_g2_us"),
        WarningPassLeftOrRight("warning_pass_left_or_right_us"),
        WarningPedestriansCrossing("warning_pedestrians_crossing_us"),
        WarningRoadNarrowsLeft("warning_road_narrows_left_us"),
        WarningRoadNarrowsRight("warning_road_narrows_right_us"),
        WarningSchoolZone("warning_school_zone_us"),
        WarningStopAhead("warning_stop_ahead_us"),
        WarningTrafficSignals("warning_traffic_signals_g3_us"),
        WarningTwoWayTraffic("warning_two_way_traffic_us"),
        WarningYieldAhead("warning_yield_ahead_g1_us"),
        InformationHighway("information_highway_exit_g1_us")
    }

    enum class SignNumber(val resourcePostfix: Double = 0.0) {
        SpeedLimit_5(5.0),
        SpeedLimit_15(15.0),
        SpeedLimit_25(25.0),
        SpeedLimit_35(35.0),
        SpeedLimit_45(45.0),
        SpeedLimit_55(55.0),
        SpeedLimit_65(65.0),
        SpeedLimit_75(75.0),
        SpeedLimit_85(85.0),
        SpeedLimit_10(10.0),
        SpeedLimit_20(20.0),
        SpeedLimit_30(30.0),
        SpeedLimit_40(40.0),
        SpeedLimit_50(50.0),
        SpeedLimit_60(60.0),
        SpeedLimit_70(70.0),
        SpeedLimit_80(80.0),
        SpeedLimit_90(90.0),
        SpeedLimit_100(100.0),
        SpeedLimit_110(110.0),
        SpeedLimit_120(120.0),
        Unknown
    }

    companion object {
        @JvmStatic
        fun getSignValueListBySignClassification(signClassification: SignClassification): List<UiSignValueModel> {

            val signValueModelsList = ArrayList<UiSignValueModel>()
            val signTypeValues = SignType.values()

            for (item in signClassification.items) {

                val signTypeIndex = item.type.ordinal
                val signType = if (signTypeIndex < signTypeValues.size) {
                    signTypeValues[signTypeIndex]
                } else {
                    continue
                }

                val signNumber = if (signTypeIndex > SignType.SpeedLimitRamp.ordinal) {
                    SignNumber.Unknown
                } else {
                    SignNumber.values().single() { it.resourcePostfix == item.number }
                }


                signValueModelsList.add(UiSignValueModel(signType, signNumber))
            }
            return signValueModelsList
        }
    }

}
