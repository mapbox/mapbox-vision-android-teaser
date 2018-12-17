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

    enum class SignType(
            val usResourceName: String = "",
            val chinaResourceName: String = ""
    ) {
        Unknown(usResourceName = "unknown", chinaResourceName = "unknown"),
        SpeedLimit(
                usResourceName = "speed_limit_us_",
                chinaResourceName = "speed_limit_cn_"
        ),
        SpeedLimitEnd(
                usResourceName = "speed_limit_end_us_",
                chinaResourceName = "speed_limit_end_cn_"
        ),
        SpeedLimitMin(
                usResourceName = "speed_limit_min_us_",
                chinaResourceName = "speed_minimum_cn_"
        ),
        SpeedLimitNight(usResourceName = "speed_limit_night_us_"),
        SpeedLimitTrucks(usResourceName = "speed_limit_trucks_us_"),
        Mass(chinaResourceName = "mass_cn_"),
        SpeedLimitComplementary(usResourceName = "speed_limit_comp_us_"),
        SpeedLimitExit(usResourceName = "warning_exit_us_"),
        SpeedLimitRamp(usResourceName = "warning_ramp_us_"),
        WarningTurnLeft(usResourceName = "warning_turn_left_us"),
        WarningTurnRight(usResourceName = "warning_turn_right_us"),
        WarningHairpinCurveLeft(usResourceName = "warning_hairpin_curve_left_us"),
        WarningRoundabout(usResourceName = "warning_roundabout_us"),
        WarningSpeedBump(usResourceName = "warning_speed_bump_us"),
        WarningWindingRoad(
                usResourceName = "warning_winding_road_us",
                chinaResourceName = "warning_winding_road_cn"
        ),
        InformationBikeRoute(usResourceName = "information_bike_route_us"),
        InformationParking(usResourceName = "information_parking_us"),
        RegulatoryAllDirectionsPermitted(usResourceName = "regulatory_all_directions_permitted_us"),
        RegulatoryBicyclesOnly(
                usResourceName = "regulatory_bicycles_only_us",
                chinaResourceName = "regulatory_bicycles_only_cn"
        ),
        RegulatoryDoNotPass(usResourceName = "regulatory_do_not_pass_us"),
        RegulatoryDoNotDriveOnShoulder(usResourceName = "regulatory_do_not_drive_on_shoulder_us"),
        RegulatoryDualLanesAllDirectionsOnRight(usResourceName = "regulatory_dual_lanes_all_directions_on_right_us"),
        RegulatoryDualLanesGoLeftOrRight(usResourceName = "regulatory_dual_lanes_go_left_or_right_us"),
        RegulatoryDualLanesGoStraightOnLeft(usResourceName = "regulatory_dual_lanes_go_straight_on_left_us"),
        RegulatoryDualLanesGoStraightOnRight(usResourceName = "regulatory_dual_lanes_go_left_or_right_us"),
        RegulatoryDualLanesTurnLeft(usResourceName = "regulatory_dual_lanes_turn_left_us"),
        RegulatoryDualLanesTurnLeftOrStraight(usResourceName = "regulatory_dual_lanes_turn_left_or_straight_us"),
        RegulatoryDualLanesTurnRightOrStraight(usResourceName = "regulatory_dual_lanes_turn_right_or_straight_us"),
        RegulatoryEndOfSchoolZone(usResourceName = "regulatory_end_of_school_zone_us"),
        RegulatoryGoStraight(
                usResourceName = "regulatory_go_straight_us",
                chinaResourceName = "regulatory_go_straight_cn"
        ),
        RegulatoryGoStraightOrTurnLeft(usResourceName = "regulatory_go_straight_or_turn_left_us"),
        RegulatoryGoStraightOrTurnRight(usResourceName = "regulatory_go_straight_or_turn_right_us"),
        RegulatoryHeightLimit(
                usResourceName = "regulatory_height_limit_us",
                chinaResourceName = "regulatory_height_limit_cn"
        ),
        RegulatoryLeftTurnYieldOnGreen(usResourceName = "regulatory_left_turn_yield_on_green_us"),
        RegulatoryNoBicycles(
                usResourceName = "regulatory_no_bicycles_us",
                chinaResourceName = "regulatory_no_bicycles_cn"
        ),
        RegulatoryNoEntry(
                usResourceName = "regulatory_no_entry_us",
                chinaResourceName = "regulatory_no_entry_cn"
        ),
        RegulatoryNoLeftOrUTurn(usResourceName = "regulatory_no_left_or_u_turn_us"),
        RegulatoryNoLeftTurn(
                usResourceName = "regulatory_no_left_turn_us",
                chinaResourceName = "regulatory_no_left_turn_cn"
        ),
        RegulatoryNoMotorVehicles(
                usResourceName = "regulatory_no_motor_vehicles_us",
                chinaResourceName = "regulatory_no_motor_vehicles_cn"
        ),
        RegulatoryNoParking(
                usResourceName = "regulatory_no_parking_us",
                chinaResourceName = "regulatory_no_parking_cn"
        ),
        RegulatoryNoParkingOrNoStopping(usResourceName = "regulatory_no_parking_or_no_stopping_us"),
        RegulatoryNoPedestrians(
                usResourceName = "regulatory_no_pedestrians_us",
                chinaResourceName = "regulatory_no_pedestrians_cn"
        ),
        RegulatoryNoRightTurn(
                usResourceName = "regulatory_no_right_turn_us",
                chinaResourceName = "regulatory_no_right_turn_cn"
        ),
        RegulatoryNoStopping(usResourceName = "regulatory_no_stopping_us"),
        RegulatoryNoStraightThrough(
                usResourceName = "regulatory_no_straight_through_us",
                chinaResourceName = "regulatory_no_straight_through_cn"
        ),
        RegulatoryNoUTurn(
                usResourceName = "regulatory_no_u_turn_us",
                chinaResourceName = "regulatory_no_u_turn_cn"
        ),
        RegulatoryOneWayStraight(usResourceName = "regulatory_one_way_straight_us"),
        RegulatoryReversibleLanes(usResourceName = "regulatory_reversible_lanes_us"),
        RegulatoryRoadClosedToVehicles(
                usResourceName = "regulatory_road_closed_to_vehicles_us",
                chinaResourceName = "regulatory_road_closed_to_vehicles_cn"
        ),
        RegulatoryStop(
                usResourceName = "regulatory_stop_us",
                chinaResourceName = "regulatory_stop_cn"
        ),
        RegulatoryTrafficSignalPhotoEnforced(usResourceName = "regulatory_traffic_signal_photo_enforced_us"),
        RegulatoryTripleLanesGoStraightCenterLane(usResourceName = "regulatory_triple_lanes_go_straight_center_lane"),
        WarningBicyclesCrossing(
                usResourceName = "warning_bicycles_crossing_us",
                chinaResourceName = "warning_bicycles_crossing_cn"
        ),
        WarningHeightRestriction(usResourceName = "warning_height_restriction_us"),
        WarningPassLeftOrRight(usResourceName = "warning_pass_left_or_right_us"),
        WarningPedestriansCrossing(
                usResourceName = "warning_pedestrians_crossing_us",
                chinaResourceName = "warning_pedestrians_crossing_cn"
        ),
        WarningRoadNarrowsLeft(
                usResourceName = "warning_road_narrows_left_us",
                chinaResourceName = "warning_road_narrows_left_cn"
        ),
        WarningRoadNarrowsRight(
                usResourceName = "warning_road_narrows_right_us",
                chinaResourceName = "warning_road_narrows_right_cn"
        ),
        WarningSchoolZone(
                usResourceName = "warning_school_zone_us",
                chinaResourceName = "warning_school_zone_cn"
        ),
        WarningStopAhead(usResourceName = "warning_stop_ahead_us"),
        WarningTrafficSignals(usResourceName = "warning_traffic_signals_us"),
        WarningTwoWayTraffic(usResourceName = "warning_two_way_traffic_us"),
        WarningYieldAhead(usResourceName = "warning_yield_ahead_us"),
        InformationHighway(usResourceName = "information_highway_exit_us"),
        RegulatoryDoNotBlockIntersection(usResourceName = "regulatory_do_not_block_intersection_us"),
        RegulatoryKeepRightPicture(usResourceName = "regulatory_keep_right_picture_us"),
        RegulatoryKeepRightText(usResourceName = "regulatory_keep_right_text_us"),
        RegulatoryNoHeavyGoodsVehiclesPicture(
                usResourceName = "regulatory_no_heavy_goods_vehicles_picture_us",
                chinaResourceName = "regulatory_no_heavy_goods_vehicles_picture_cn"
        ),
        RegulatoryNoLeftTurnText(usResourceName = "regulatory_no_left_turn_text_us"),
        RegulatoryOneWayLeftArrow(usResourceName = "regulatory_one_way_left_arrow_us"),
        RegulatoryOneWayLeftArrowText(usResourceName = "regulatory_one_way_left_arrow_text_us"),
        RegulatoryOneWayLeftText(usResourceName = "regulatory_one_way_left_text_us"),
        RegulatoryOneWayRightArrow(usResourceName = "regulatory_one_way_right_arrow_us"),
        RegulatoryOneWayRightArrowText(usResourceName = "regulatory_one_way_right_arrow_text_us"),
        RegulatoryOneWayRightText(usResourceName = "regulatory_one_way_right_text_us"),
        RegulatoryTurnLeftAhead(usResourceName = "regulatory_turn_left_ahead_us"),
        RegulatoryTurnLeft(
                usResourceName = "regulatory_turn_left_us",
                chinaResourceName = "regulatory_turn_left_cn"
        ),
        RegulatoryTurnLeftOrRight(
                usResourceName = "regulatory_turn_left_or_right_us",
                chinaResourceName = "regulatory_turn_left_or_right_cn"
        ),
        RegulatoryTurnRightAhead(usResourceName = "regulatory_turn_right_ahead_us"),
        RegulatoryYield(
                usResourceName = "regulatory_yield_us",
                chinaResourceName = "regulatory_yield_cn"
        ),
        WarningRailwayCrossing(
                usResourceName = "warning_railway_crossing_us",
                chinaResourceName = "warning_railway_crossing_cn"
        ),
        WarningHairpinCurveRight(usResourceName = "warning_hairpin_curve_right_us"),
        ComplementaryOneDirectionLeft(usResourceName = "complementary_one_direction_left_us"),
        ComplementaryOneDirectionRight(usResourceName = "complementary_one_direction_right_us"),
        WarningCurveLeft(
                usResourceName = "warning_curve_left_us",
                chinaResourceName = "warning_curve_left_cn"
        ),
        WarningCurveRight(
                usResourceName = "warning_curve_right_us",
                chinaResourceName = "warning_curve_right_cn"
        ),
        WarningHorizontalAlignmentLeft(usResourceName = "warning_horizontal_alignment_left_us"),
        WarningHorizontalAlignmentRight(usResourceName = "warning_horizontal_alignment_right_us"),
        RegulatoryTurnRight(
                usResourceName = "regulatory_turn_right_us",
                chinaResourceName = "regulatory_turn_right_cn"
        ),
        WhiteTablesText(usResourceName = "white_tables_text_us"),
        Lanes(usResourceName = "lanes_us"),
        GreenPlates(usResourceName = "green_plates_us"),
        WarningText(usResourceName = "warning_text_us"),
        WarningCrossroads(usResourceName = "warning_crossroads_us"),
        WarningPicture(usResourceName = "warning_picture_us"),
        ComplementaryKeepLeft(usResourceName = "complementary_keep_left_us"),
        ComplementaryKeepRight(
                usResourceName = "complementary_keep_right_us",
                chinaResourceName = "complementary_keep_right_cn"
        ),
        RegulatoryExceptBicycle(usResourceName = "regulatory_except_bicycle_us"),
        WarningAddedLaneRight(usResourceName = "warning_added_lane_right_us"),
        WarningDeadEndText(usResourceName = "warning_dead_end_text_us"),
        WarningDipText(usResourceName = "warning_dip_text_us"),
        WarningEmergencyVehicles(usResourceName = "warning_emergency_vehicles_us"),
        WarningEndText(usResourceName = "warning_end_text_us"),
        WarningFallingRocksOrDebrisRight(usResourceName = "warning_falling_rocks_or_debris_right_us"),
        WarningLowGroundClearance(usResourceName = "warning_low_ground_clearance_us"),
        WarningObstructionMarker(usResourceName = "warning_obstruction_marker_us"),
        WarningPlayground(usResourceName = "warning_playground_us"),
        WarningSecondRoadRight(
                usResourceName = "warning_second_road_right_us",
                chinaResourceName = "warning_second_road_right_cn"
        ),
        WarningTurnLeftOnlyArrow(usResourceName = "warning_turn_left_only_arrow_us"),
        WarningTurnLeftOrRightOnlyArrow(usResourceName = "warning_turn_left_or_right_only_arrow_us"),
        WarningTramsCrossing(usResourceName = "warning_trams_crossing_us"),
        WarningUnevenRoad(usResourceName = "warning_uneven_road_us"),
        WarningWildAnimals(usResourceName = "warning_wild_animals_us"),
        RegulatoryParkingRestrictions(usResourceName = "regulatory_parking_restrictions_us"),
        RegulatoryYieldOrStopForPedestrians(usResourceName = "regulatory_yield_or_stop_for_pedestrians_us"),
        RegulatoryNoBuses(chinaResourceName = "regulatory_no_buses_cn"),
        RegulatoryNoSmallPassengerCar(chinaResourceName = "regulatory_no_small_passenger_car_cn"),
        RegulatoryNoMotorcyclesOrMopeds(chinaResourceName = "regulatory_no_motorcycles_or_mopeds_cn"),
        RegulatoryNoTurnLeftOrTurnRight(chinaResourceName = "regulatory_no_turn_left_or_turn_right_cn"),
        RegulatoryNoOvertaking(chinaResourceName = "regulatory_no_overtaking_cn"),
        RegulatoryNoHonking(chinaResourceName = "regulatory_no_honking_cn"),
        RegulatoryWidthLimit(chinaResourceName = "regulatory_width_limit_cn"),
        RegulatoryAxleWeightLimit(chinaResourceName = "regulatory_axle_weight_limit_cn"),
        RegulatoryNoVehiclesCarryingExplosives(chinaResourceName = "regulatory_no_vehicles_carrying_explosives_cn"),
        RegulatoryRoundabout(chinaResourceName = "regulatory_roundabout_cn"),
        RegulatoryHonking(chinaResourceName = "regulatory_honking_cn"),
        RegulatoryPedestriansCrossing(chinaResourceName = "regulatory_pedestrians_crossing_cn"),
        RegulatoryMotorVehicles(chinaResourceName = "regulatory_motor_vehicles_cn"),
        RegulatoryUTurn(chinaResourceName = "regulatory_u_turn_cn"),
        WarningSteepAscent(chinaResourceName = "warning_steep_ascent_cn"),
        WarningSteepDescent(chinaResourceName = "warning_steep_descent_cn"),
        WarningVillage(chinaResourceName = "warning_village_cn"),
        WarningKeepSlowdown(chinaResourceName = "warning_keep_slowdown_cn"),
        WarningDangerousTraffic(chinaResourceName = "warning_dangerous_traffic_cn"),
        WarningRoadworks(chinaResourceName = "warning_roadworks_cn"),
        WarningSecondRoadLeft(chinaResourceName = "warning_second_road_left_cn")
    }

    @Suppress("EnumEntryName")
    enum class SignNumber(val value: Int = 0) {
        Limit_5(5),
        Limit_15(15),
        Limit_25(25),
        Limit_35(35),
        Limit_45(45),
        Limit_55(55),
        Limit_65(65),
        Limit_75(75),
        Limit_85(85),
        Limit_10(10),
        Limit_20(20),
        Limit_30(30),
        Limit_40(40),
        Limit_50(50),
        Limit_60(60),
        Limit_70(70),
        Limit_80(80),
        Limit_90(90),
        Limit_100(100),
        Limit_110(110),
        Limit_120(120),
        Unknown;

        companion object {
            fun fromNumber(value: Double) = SignNumber.values().firstOrNull { it.value.toDouble() == value }
                                            ?: Unknown
        }
    }
}
