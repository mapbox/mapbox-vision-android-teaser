package com.mapbox.vision.examples.models

import com.mapbox.vision.visionevents.events.classification.SignClassification

data class UiSignValueModel(val signType: SignType, val signNum: SignNumber) {

    companion object {
        fun getSignValueListBySignClassification(signClassification: SignClassification): List<UiSignValueModel> {
            val signValueModelsList = ArrayList<UiSignValueModel>()
            val signTypeValues = SignType.values()

            for (item in signClassification.items) {
                val signTypeIndex = item.type.ordinal
                if (signTypeIndex >= signTypeValues.size) {
                    continue
                }

                val signType = signTypeValues[signTypeIndex]

                val signNumber = if (signTypeIndex > SignType.SpeedLimitRamp.ordinal) {
                    SignNumber.Unknown
                } else {
                    SignNumber.fromNumber(item.number)
                }

                signValueModelsList.add(UiSignValueModel(signType, signNumber))
            }
            return signValueModelsList
        }
    }

    enum class SignType(val resourceName: String = "") {
        Unknown("unknown"),
        SpeedLimit("speed_limit_us_"),
        SpeedLimitEnd("speed_limit_end_us_"),
        SpeedLimitMin("speed_limit_min_us_"),
        SpeedLimitNight("speed_limit_night_us_"),
        SpeedLimitTrucks("speed_limit_trucks_us_"),
        Mass,
        SpeedLimitComplementary("speed_limit_comp_us_"),
        SpeedLimitExit("warning_exit_us_"),
        SpeedLimitRamp("warning_ramp_us_"),
        WarningTurnLeft("warning_turn_left_us"),
        WarningTurnRight("warning_turn_right_us"),
        WarningHairpinCurveLeft("warning_hairpin_curve_left_us"),
        WarningRoundabout("warning_roundabout_us"),
        WarningSpeedBump("warning_speed_bump_us"),
        WarningWindingRoad("warning_winding_road_us"),
        InformationBikeRoute("information_bike_route_us"),
        InformationParking("information_parking_us"),
        RegulatoryAllDirectionsPermitted("regulatory_all_directions_permitted_us"),
        RegulatoryBicyclesOnly("regulatory_bicycles_only_us"),
        RegulatoryDoNotPass("regulatory_do_not_pass_us"),
        RegulatoryDoNotDriveOnShoulder("regulatory_do_not_drive_on_shoulder_us"),
        RegulatoryDualLanesAllDirectionsOnRight("regulatory_dual_lanes_all_directions_on_right_us"),
        RegulatoryDualLanesGoLeftOrRight("regulatory_dual_lanes_go_left_or_right_us"),
        RegulatoryDualLanesGoStraightOnLeft("regulatory_dual_lanes_go_straight_on_left_us"),
        RegulatoryDualLanesGoStraightOnRight("regulatory_dual_lanes_go_left_or_right_us"),
        RegulatoryDualLanesTurnLeft("regulatory_dual_lanes_turn_left_us"),
        RegulatoryDualLanesTurnLeftOrStraight("regulatory_dual_lanes_turn_left_or_straight_us"),
        RegulatoryDualLanesTurnRightOrStraight("regulatory_dual_lanes_turn_right_or_straight_us"),
        RegulatoryEndOfSchoolZone("regulatory_end_of_school_zone_us"),
        RegulatoryGoStraight("regulatory_go_straight_us"),
        RegulatoryGoStraightOrTurnLeft("regulatory_go_straight_or_turn_left_us"),
        RegulatoryGoStraightOrTurnRight("regulatory_go_straight_or_turn_right_us"),
        RegulatoryHeightLimit("regulatory_height_limit_us"),
        RegulatoryLeftTurnYieldOnGreen("regulatory_left_turn_yield_on_green_us"),
        RegulatoryNoBicycles("regulatory_no_bicycles_us"),
        RegulatoryNoEntry("regulatory_no_entry_us"),
        RegulatoryNoLeftOrUTurn("regulatory_no_left_or_u_turn_us"),
        RegulatoryNoLeftTurn("regulatory_no_left_turn_us"),
        RegulatoryNoMotorVehicles("regulatory_no_motor_vehicles_us"),
        RegulatoryNoParking("regulatory_no_parking_us"),
        RegulatoryNoParkingOrNoStopping("regulatory_no_parking_or_no_stopping_us"),
        RegulatoryNoPedestrians("regulatory_no_pedestrians_us"),
        RegulatoryNoRightTurn("regulatory_no_right_turn_us"),
        RegulatoryNoStopping("regulatory_no_stopping_us"),
        RegulatoryNoStraightThrough("regulatory_no_straight_through_us"),
        RegulatoryNoUTurn("regulatory_no_u_turn_us"),
        RegulatoryOneWayStraight("regulatory_one_way_straight_us"),
        RegulatoryReversibleLanes("regulatory_reversible_lanes_us"),
        RegulatoryRoadClosedToVehicles("regulatory_road_closed_to_vehicles_us"),
        RegulatoryStop("regulatory_stop_us"),
        RegulatoryTrafficSignalPhotoEnforced("regulatory_traffic_signal_photo_enforced_us"),
        RegulatoryTripleLanesGoStraightCenterLane("regulatory_triple_lanes_go_straight_center_lane"),
        WarningBicyclesCrossing("warning_bicycles_crossing_us"),
        WarningHeightRestriction("warning_height_restriction_us"),
        WarningPassLeftOrRight("warning_pass_left_or_right_us"),
        WarningPedestriansCrossing("warning_pedestrians_crossing_us"),
        WarningRoadNarrowsLeft("warning_road_narrows_left_us"),
        WarningRoadNarrowsRight("warning_road_narrows_right_us"),
        WarningSchoolZone("warning_school_zone_us"),
        WarningStopAhead("warning_stop_ahead_us"),
        WarningTrafficSignals("warning_traffic_signals_us"),
        WarningTwoWayTraffic("warning_two_way_traffic_us"),
        WarningYieldAhead("warning_yield_ahead_us"),
        InformationHighway("information_highway_exit_us"),
        RegulatoryDoNotBlockIntersection("regulatory_do_not_block_intersection_us"),
        RegulatoryKeepRightPicture("regulatory_keep_right_picture_us"),
        RegulatoryKeepRightText("regulatory_keep_right_text_us"),
        RegulatoryNoHeavyGoodsVehiclesPicture("regulatory_no_heavy_goods_vehicles_picture_us"),
        RegulatoryNoLeftTurnText("regulatory_no_left_turn_text_us"),
        RegulatoryOneWayLeftArrow("regulatory_one_way_left_arrow_us"),
        RegulatoryOneWayLeftArrowText("regulatory_one_way_left_arrow_text_us"),
        RegulatoryOneWayLeftText("regulatory_one_way_left_text_us"),
        RegulatoryOneWayRightArrow("regulatory_one_way_right_arrow_us"),
        RegulatoryOneWayRightArrowText("regulatory_one_way_right_arrow_text_us"),
        RegulatoryOneWayRightText("regulatory_one_way_right_text_us"),
        RegulatoryTurnLeftAhead("regulatory_turn_left_ahead_us"),
        RegulatoryTurnLeft("regulatory_turn_left_us"),
        RegulatoryTurnLeftOrRight("regulatory_turn_left_or_right_us"),
        RegulatoryTurnRightAhead("regulatory_turn_right_ahead_us"),
        RegulatoryYield("regulatory_yield_us"),
        WarningRailwayCrossing("warning_railway_crossing_us"),
        WarningHairpinCurveRight("warning_hairpin_curve_right_us"),
        ComplementaryOneDirectionLeft("complementary_one_direction_left_us"),
        ComplementaryOneDirectionRight("complementary_one_direction_right_us"),
        WarningCurveLeft("warning_curve_left_us"),
        WarningCurveRight("warning_curve_right_us"),
        WarningHorizontalAlignmentLeft("warning_horizontal_alignment_left_us"),
        WarningHorizontalAlignmentRight("warning_horizontal_alignment_right_us"),
        RegulatoryTurnRight("regulatory_turn_right_us"),
        WhiteTablesText("white_tables_text_us"),
        Lanes("lanes_us"),
        GreenPlates("green_plates_us"),
        WarningText("warning_text_us"),
        WarningCrossroads("warning_crossroads_us"),
        WarningPicture("warning_picture_us"),
        ComplementaryKeepLeft("complementary_keep_left_us"),
        ComplementaryKeepRight("complementary_keep_right_us"),
        RegulatoryExceptBicycle("regulatory_except_bicycle_us"),
        WarningAddedLaneRight("warning_added_lane_right_us"),
        WarningDeadEndText("warning_dead_end_text_us"),
        WarningDipText("warning_dip_text_us"),
        WarningEmergencyVehicles("warning_emergency_vehicles_us"),
        WarningEndText("warning_end_text_us"),
        WarningFallingRocksOrDebrisRight("warning_falling_rocks_or_debris_right_us"),
        WarningLowGroundClearance("warning_low_ground_clearance_us"),
        WarningObstructionMarker("warning_obstruction_marker_us"),
        WarningPlayground("warning_playground_us"),
        WarningSecondRoadRight("warning_second_road_right_us"),
        WarningTurnLeftOnlyArrow("warning_turn_left_only_arrow_us"),
        WarningTurnLeftOrRightOnlyArrow("warning_turn_left_or_right_only_arrow_us"),
        WarningTramsCrossing("warning_trams_crossing_us"),
        WarningUnevenRoad("warning_uneven_road_us"),
        WarningWildAnimals("warning_wild_animals_us"),
        RegulatoryParkingRestrictions("regulatory_parking_restrictions_us"),
        RegulatoryYieldOrStopForPedestrians("regulatory_yield_or_stop_for_pedestrians_us")
    }

    enum class SignNumber(val value: Int = 0) {
        SpeedLimit_5(5),
        SpeedLimit_15(15),
        SpeedLimit_25(25),
        SpeedLimit_35(35),
        SpeedLimit_45(45),
        SpeedLimit_55(55),
        SpeedLimit_65(65),
        SpeedLimit_75(75),
        SpeedLimit_85(85),
        SpeedLimit_10(10),
        SpeedLimit_20(20),
        SpeedLimit_30(30),
        SpeedLimit_40(40),
        SpeedLimit_50(50),
        SpeedLimit_60(60),
        SpeedLimit_70(70),
        SpeedLimit_80(80),
        SpeedLimit_90(90),
        Unknown;

        companion object {
            fun fromNumber(value: Double) = SignNumber.values().firstOrNull { it.value.toDouble() == value } ?: Unknown
        }
    }
}
