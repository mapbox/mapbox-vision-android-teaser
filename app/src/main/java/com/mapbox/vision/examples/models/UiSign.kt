package com.mapbox.vision.examples.models

import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications

data class UiSign(val signType: SignType, val signNum: SignNumber) {

    companion object {
        fun getUiSigns(frameSignClassifications: FrameSignClassifications): List<UiSign> {
            val signTypeValues = SignType.values()

            return frameSignClassifications.signs.map { signClassification ->
                val signTypeIndex = signClassification.sign.type.ordinal
                if (signTypeIndex >= signTypeValues.size) {
                    throw IllegalStateException("Illegal sign! ${signClassification.sign}")
                }

                val signType = signTypeValues[signTypeIndex]

                val signNumber = if (signTypeIndex > SignType.SpeedLimitRamp.ordinal) {
                    SignNumber.Unknown
                } else {
                    SignNumber.fromNumber(signClassification.sign.number)
                }

                UiSign(signType, signNumber)
            }
        }
    }

    enum class SignType(
        val usResourceName: String = "",
        val chinaResourceName: String = usResourceName,
        val ukResourceName: String = usResourceName
    ) {
        Unknown(usResourceName = "unknown"),
        SpeedLimit(
            usResourceName = "speed_limit_us_",
            chinaResourceName = "speed_limit_cn_",
            ukResourceName = "speed_limit_uk_"
        ),
        SpeedLimitEnd(
            usResourceName = "speed_limit_end_us_",
            chinaResourceName = "speed_limit_end_cn_",
            ukResourceName = "speed_limit_end_uk_"
        ),
        SpeedLimitMin(
            usResourceName = "speed_limit_min_us_",
            chinaResourceName = "speed_limit_min_cn_",
            ukResourceName = "speed_limit_min_uk_"
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
        WarningRoundabout(
            usResourceName = "warning_roundabout_us",
            ukResourceName = "warning_round_about_uk"
        ),
        WarningSpeedBump(usResourceName = "warning_speed_bump_us", ukResourceName = "warning_speed_bump_uk"),
        WarningWindingRoad(
            usResourceName = "warning_winding_road_us",
            chinaResourceName = "warning_winding_road_cn",
            ukResourceName = "warning_winding_road_uk"
        ),
        InformationBikeRoute(usResourceName = "information_bike_route_us"),
        InformationParking(
            usResourceName = "information_parking_us",
            ukResourceName = "information_parking_uk"
        ),
        RegulatoryAllDirectionsPermitted(usResourceName = "regulatory_all_directions_permitted_us"),
        RegulatoryBicyclesOnly(
            usResourceName = "regulatory_bicycles_only_us",
            chinaResourceName = "regulatory_bicycles_only_cn",
            ukResourceName = "regulatory_bicycles_only_uk"
        ),
        RegulatoryDoNotPass(usResourceName = "regulatory_do_not_pass_us"),
        RegulatoryDoNotDriveOnShoulder(usResourceName = "regulatory_do_not_drive_on_shoulder_us"),
        RegulatoryDualLanesAllDirectionsOnRight(usResourceName = "regulatory_dual_lanes_all_directions_on_right_us"),
        RegulatoryDualLanesGoLeftOrRight(usResourceName = "regulatory_dual_lanes_go_left_or_right_us"),
        RegulatoryDualLanesGoStraightOnLeft(usResourceName = "regulatory_dual_lanes_go_straight_on_left_us"),
        RegulatoryDualLanesGoStraightOnRight(usResourceName = "regulatory_dual_lanes_go_left_or_right_us"),
        RegulatoryDualLanesTurnLeft(usResourceName = "regulatory_dual_lanes_turn_left_us"),
        RegulatoryDualLanesTurnLeftOrStraight(
            usResourceName = "regulatory_dual_lanes_turn_left_or_straight_us",
            ukResourceName = "regulatory_dual_lanes_turn_left_or_straight_uk"
        ),
        RegulatoryDualLanesTurnRightOrStraight(
            usResourceName = "regulatory_dual_lanes_turn_right_or_straight_us",
            ukResourceName = "regulatory_dual_lanes_turn_right_or_straight_uk"
        ),
        RegulatoryEndOfSchoolZone(usResourceName = "regulatory_end_of_school_zone_us"),
        RegulatoryGoStraight(
            usResourceName = "regulatory_go_straight_us",
            chinaResourceName = "regulatory_go_straight_cn",
            ukResourceName = "regulatory_go_straight_uk"
        ),
        RegulatoryGoStraightOrTurnLeft(usResourceName = "regulatory_go_straight_or_turn_left_us"),
        RegulatoryGoStraightOrTurnRight(usResourceName = "regulatory_go_straight_or_turn_right_us"),
        RegulatoryHeightLimit(
            usResourceName = "regulatory_height_limit_us",
            chinaResourceName = "regulatory_height_limit_cn",
            ukResourceName = "regulatory_height_limit_uk"
        ),
        RegulatoryLeftTurnYieldOnGreen(usResourceName = "regulatory_left_turn_yield_on_green_us"),
        RegulatoryNoBicycles(
            usResourceName = "regulatory_no_bicycles_us",
            chinaResourceName = "regulatory_no_bicycles_cn",
            ukResourceName = "regulatory_no_bicycles_uk"
        ),
        RegulatoryNoEntry(
            usResourceName = "regulatory_no_entry_us",
            chinaResourceName = "regulatory_no_entry_cn",
            ukResourceName = "regulatory_no_entry_uk"
        ),
        RegulatoryNoLeftOrUTurn(usResourceName = "regulatory_no_left_or_u_turn_us"),
        RegulatoryNoLeftTurn(
            usResourceName = "regulatory_no_left_turn_us",
            chinaResourceName = "regulatory_no_left_turn_cn"
        ),
        RegulatoryNoMotorVehicles(
            usResourceName = "regulatory_no_motor_vehicles_us",
            chinaResourceName = "regulatory_no_motor_vehicles_cn",
            ukResourceName = "regulatory_no_motor_vehicles_uk"
        ),
        RegulatoryNoParking(
            usResourceName = "regulatory_no_parking_us",
            chinaResourceName = "regulatory_no_parking_cn",
            ukResourceName = "regulatory_no_parking_uk"
        ),
        RegulatoryNoParkingOrNoStopping(
            usResourceName = "regulatory_no_parking_or_no_stopping_us",
            ukResourceName = "regulatory_no_parking_or_no_stopping_uk"
        ),
        RegulatoryNoPedestrians(
            usResourceName = "regulatory_no_pedestrians_us",
            chinaResourceName = "regulatory_no_pedestrians_cn",
            ukResourceName = "regulatory_no_pedestrians_uk"
        ),
        RegulatoryNoRightTurn(
            usResourceName = "regulatory_no_right_turn_us",
            chinaResourceName = "regulatory_no_right_turn_cn",
            ukResourceName = "regulatory_no_right_turn_uk"
        ),
        RegulatoryNoStopping(usResourceName = "regulatory_no_stopping_us"),
        RegulatoryNoStraightThrough(
            usResourceName = "regulatory_no_straight_through_us",
            chinaResourceName = "regulatory_no_straight_through_cn"
        ),
        RegulatoryNoUTurn(
            usResourceName = "regulatory_no_u_turn_us",
            chinaResourceName = "regulatory_no_u_turn_cn",
            ukResourceName = "regulatory_no_u_turn_uk"
        ),
        RegulatoryOneWayStraight(
            usResourceName = "regulatory_one_way_straight_us",
            ukResourceName = "regulatory_one_way_straight_uk"
        ),
        RegulatoryReversibleLanes(usResourceName = "regulatory_reversible_lanes_us"),
        RegulatoryRoadClosedToVehicles(
            usResourceName = "regulatory_road_closed_to_vehicles_us",
            chinaResourceName = "regulatory_road_closed_to_vehicles_cn",
            ukResourceName = "regulatory_road_closed_to_vehicles_uk"
        ),
        RegulatoryStop(
            usResourceName = "regulatory_stop_us",
            chinaResourceName = "regulatory_stop_cn",
            ukResourceName = "regulatory_stop_uk"
        ),
        RegulatoryTrafficSignalPhotoEnforced(usResourceName = "regulatory_traffic_signal_photo_enforced_us"),
        RegulatoryTripleLanesGoStraightCenterLane(usResourceName = "regulatory_triple_lanes_go_straight_center_lane"),
        WarningBicyclesCrossing(
            usResourceName = "warning_bicycles_crossing_us",
            chinaResourceName = "warning_bicycles_crossing_cn",
            ukResourceName = "warning_bicycles_crossing_uk"
        ),
        WarningHeightRestriction(usResourceName = "warning_height_restriction_us"),
        WarningPassLeftOrRight(
            usResourceName = "warning_pass_left_or_right_us",
            ukResourceName = "warning_pass_left_or_right_uk"
        ),
        WarningPedestriansCrossing(
            usResourceName = "warning_pedestrians_crossing_us",
            chinaResourceName = "warning_pedestrians_crossing_cn",
            ukResourceName = "warning_pedestrians_crossing_uk"
        ),
        WarningRoadNarrowsLeft(
            usResourceName = "warning_road_narrows_left_us",
            chinaResourceName = "warning_road_narrows_left_cn",
            ukResourceName = "warning_road_narrows_left_uk"
        ),
        WarningRoadNarrowsRight(
            usResourceName = "warning_road_narrows_right_us",
            chinaResourceName = "warning_road_narrows_right_cn",
            ukResourceName = "warning_road_narrows_right_uk"
        ),
        WarningSchoolZone(
            usResourceName = "warning_school_zone_us",
            chinaResourceName = "warning_school_zone_cn",
            ukResourceName = "warning_school_zone_uk"
        ),
        WarningStopAhead(usResourceName = "warning_stop_ahead_us"),
        WarningTrafficSignals(
            usResourceName = "warning_traffic_signals_us",
            ukResourceName = "warning_traffic_signals_uk"
        ),
        WarningTwoWayTraffic(
            usResourceName = "warning_two_way_traffic_us",
            ukResourceName = "warning_two_way_traffic_uk"
        ),
        WarningYieldAhead(usResourceName = "warning_yield_ahead_us"),
        InformationHighway(usResourceName = "information_highway_exit_us"),
        RegulatoryDoNotBlockIntersection(usResourceName = "regulatory_do_not_block_intersection_us"),
        RegulatoryKeepRightPicture(usResourceName = "regulatory_keep_right_picture_us"),
        RegulatoryKeepRightText(usResourceName = "regulatory_keep_right_text_us"),
        RegulatoryNoHeavyGoodsVehiclesPicture(
            usResourceName = "regulatory_no_heavy_goods_vehicles_picture_us",
            chinaResourceName = "regulatory_no_heavy_goods_vehicles_picture_cn",
            ukResourceName = "regulatory_no_heavy_goods_vehicles_picture_uk"
        ),
        RegulatoryNoLeftTurnText(
            usResourceName = "regulatory_no_left_turn_text_us",
            ukResourceName = "regulatory_no_left_turn_uk"
        ),
        RegulatoryOneWayLeftArrow(
            usResourceName = "regulatory_one_way_left_arrow_us",
            ukResourceName = "regulatory_one_way_left_arrow_uk"
        ),
        RegulatoryOneWayLeftArrowText(usResourceName = "regulatory_one_way_left_arrow_text_us"),
        RegulatoryOneWayLeftText(usResourceName = "regulatory_one_way_left_text_us"),
        RegulatoryOneWayRightArrow(usResourceName = "regulatory_one_way_right_arrow_us"),
        RegulatoryOneWayRightArrowText(
            usResourceName = "regulatory_one_way_right_arrow_text_us",
            ukResourceName = "regulatory_one_way_right_arrow_uk"
        ),
        RegulatoryOneWayRightText(usResourceName = "regulatory_one_way_right_text_us"),
        RegulatoryTurnLeftAhead(usResourceName = "regulatory_turn_left_ahead_us"),
        RegulatoryTurnLeft(
            usResourceName = "regulatory_turn_left_us",
            chinaResourceName = "regulatory_turn_left_cn",
            ukResourceName = "regulatory_turn_left_uk"
        ),
        RegulatoryTurnLeftOrRight(
            usResourceName = "regulatory_turn_left_or_right_us",
            chinaResourceName = "regulatory_turn_left_or_right_cn",
            ukResourceName = "regulatory_turn_left_or_right_uk"
        ),
        RegulatoryTurnRightAhead(usResourceName = "regulatory_turn_right_ahead_us"),
        RegulatoryYield(
            usResourceName = "regulatory_yield_us",
            chinaResourceName = "regulatory_yield_cn",
            ukResourceName = "regulatory_yield_uk"
        ),
        WarningRailwayCrossing(
            usResourceName = "warning_railway_crossing_us",
            chinaResourceName = "warning_railway_crossing_cn",
            ukResourceName = "warning_railway_crossing_uk"
        ),
        WarningHairpinCurveRight(usResourceName = "warning_hairpin_curve_right_us"),
        ComplementaryOneDirectionLeft(usResourceName = "complementary_one_direction_left_us"),
        ComplementaryOneDirectionRight(usResourceName = "complementary_one_direction_right_us"),
        WarningCurveLeft(
            usResourceName = "warning_curve_left_us",
            chinaResourceName = "warning_curve_left_cn",
            ukResourceName = "warning_curve_left_uk"
        ),
        WarningCurveRight(
            usResourceName = "warning_curve_right_us",
            chinaResourceName = "warning_curve_right_cn",
            ukResourceName = "warning_curve_right_uk"
        ),
        WarningHorizontalAlignmentLeft(usResourceName = "warning_horizontal_alignment_left_us"),
        WarningHorizontalAlignmentRight(usResourceName = "warning_horizontal_alignment_right_us"),
        RegulatoryTurnRight(
            usResourceName = "regulatory_turn_right_us",
            chinaResourceName = "regulatory_turn_right_cn",
            ukResourceName = "regulatory_turn_right_uk"
        ),
        WhiteTablesText(usResourceName = "white_tables_text_us"),
        Lanes(usResourceName = "lanes_us"),
        GreenPlates(usResourceName = "green_plates_us"),
        WarningText(usResourceName = "warning_text_us"),
        WarningCrossroads(usResourceName = "warning_crossroads_us"),
        WarningPicture(usResourceName = "warning_picture_us"),
        ComplementaryKeepLeft(
            usResourceName = "complementary_keep_left_us",
            ukResourceName = "complementary_keep_left_uk"
        ),
        ComplementaryKeepRight(
            usResourceName = "complementary_keep_right_us",
            chinaResourceName = "complementary_keep_right_cn",
            ukResourceName = "complementary_keep_right_uk"
        ),
        RegulatoryExceptBicycle(usResourceName = "regulatory_except_bicycle_us"),
        WarningAddedLaneRight(usResourceName = "warning_added_lane_right_us"),
        WarningDeadEndText(usResourceName = "warning_dead_end_text_us", ukResourceName = "warning_dead_end_text_uk"),
        WarningDipText(usResourceName = "warning_dip_text_us"),
        WarningEmergencyVehicles(usResourceName = "warning_emergency_vehicles_us"),
        WarningEndText(usResourceName = "warning_end_text_us"),
        WarningFallingRocksOrDebrisRight(usResourceName = "warning_falling_rocks_or_debris_right_us", ukResourceName = "warning_falling_rocks_or_debris_right_uk"),
        WarningLowGroundClearance(usResourceName = "warning_low_ground_clearance_us", ukResourceName = "warning_low_ground_clearance_uk"),
        WarningObstructionMarker(usResourceName = "warning_obstruction_marker_us"),
        WarningPlayground(usResourceName = "warning_playground_us"),
        WarningSecondRoadRight(
            usResourceName = "warning_second_road_right_us",
            chinaResourceName = "warning_second_road_right_cn"
        ),
        WarningTurnLeftOnlyArrow(usResourceName = "warning_turn_left_only_arrow_us"),
        WarningTurnLeftOrRightOnlyArrow(usResourceName = "warning_turn_left_or_right_only_arrow_us"),
        WarningTramsCrossing(usResourceName = "warning_trams_crossing_us", ukResourceName = "warning_trams_crossing_uk"),
        WarningUnevenRoad(usResourceName = "warning_uneven_road_us", ukResourceName = "warning_uneven_road_uk"),
        WarningWildAnimals(usResourceName = "warning_wild_animals_us", ukResourceName = "warning_wild_animals_uk"),
        RegulatoryParkingRestrictions(usResourceName = "regulatory_parking_restrictions_us"),
        RegulatoryYieldOrStopForPedestrians(usResourceName = "regulatory_yield_or_stop_for_pedestrians_us"),
        RegulatoryNoBuses(
            chinaResourceName = "regulatory_no_buses_cn",
            ukResourceName = "regulatory_no_buses_uk"
        ),
        RegulatoryNoSmallPassengerCar(chinaResourceName = "regulatory_no_small_passenger_car_cn"),
        RegulatoryNoMotorcyclesOrMopeds(
            chinaResourceName = "regulatory_no_motorcycles_or_mopeds_cn",
            ukResourceName = "regulatory_no_motorcycles_or_mopeds_uk"
        ),
        RegulatoryNoTurnLeftOrTurnRight(chinaResourceName = "regulatory_no_turn_left_or_turn_right_cn"),
        RegulatoryNoOvertaking(
            chinaResourceName = "regulatory_no_overtaking_cn",
            ukResourceName = "regulatory_no_overtaking_uk"
        ),
        RegulatoryNoHonking(chinaResourceName = "regulatory_no_honking_cn"),
        RegulatoryWidthLimit(
            chinaResourceName = "regulatory_width_limit_cn",
            ukResourceName = "regulatory_width_limit_uk"
        ),
        RegulatoryAxleWeightLimit(chinaResourceName = "regulatory_axle_weight_limit_cn", ukResourceName = "regulatory_axle_weight_limit_uk"),
        RegulatoryNoVehiclesCarryingExplosives(chinaResourceName = "regulatory_no_vehicles_carrying_explosives_cn"),
        RegulatoryRoundabout(
            chinaResourceName = "regulatory_roundabout_cn",
            ukResourceName = "regulatory_roundabout_uk"
        ),
        RegulatoryHonking(chinaResourceName = "regulatory_honking_cn"),
        RegulatoryPedestriansCrossing(
            chinaResourceName = "regulatory_pedestrians_crossing_cn",
            ukResourceName = "regulatory_pedestrians_crossing_uk"
        ),
        RegulatoryMotorVehicles(
            chinaResourceName = "regulatory_motor_vehicles_cn",
            ukResourceName = "regulatory_motor_vehicles_uk"
        ),
        RegulatoryUTurn(chinaResourceName = "regulatory_u_turn_cn"),
        WarningSteepAscent(chinaResourceName = "warning_steep_ascent_cn", ukResourceName = "warning_steep_ascent_uk"),
        WarningSteepDescent(chinaResourceName = "warning_steep_descent_cn", ukResourceName = "warning_steep_descent_uk"),
        WarningVillage(chinaResourceName = "warning_village_cn"),
        WarningKeepSlowdown(chinaResourceName = "warning_keep_slowdown_cn"),
        WarningDangerousTraffic(chinaResourceName = "warning_dangerous_traffic_cn", ukResourceName = "warning_dangerous_traffic_uk"),
        WarningRoadworks(chinaResourceName = "warning_roadworks_cn", ukResourceName = "warning_roadworks_uk"),
        WarningSecondRoadLeft(chinaResourceName = "warning_second_road_left_cn"),

        RegulatoryNoTurnOnRedText("TODO"),
        WarningAddedLaneLeft("TODO"),
        WarningFlaggersInRoad("TODO"),
        WarningLoop270Degree("TODO"),
        WarningRoadNarrows(ukResourceName = "warning_road_narrows_uk"),
        WarningSlipperyRoadSurface(ukResourceName = "warning_slippery_road_surface_uk"),

        RegulatoryBusLane(ukResourceName = "regulatory_bus_lane_uk"),
        RegulatoryEndNoOvertaking("TODO"),
        RegulatoryNoHumanCargoTricycleEntry("TODO"),
        RegulatoryNoHumanPassengerTricycleEntry("TODO"),
        RegulatoryNoRickshaws("TODO"),
        RegulatoryNoStraightThroughOrTurnLeft("TODO"),
        RegulatoryNoStraightThroughOrTurnRight("TODO"),
        RegulatoryNoTractors("TODO"),
        RegulatoryNoTricycles("TODO"),
        RegulatoryUTurnOrTurnLeft("TODO"),
        RegulatoryWalk(ukResourceName = "regulatory_walk_uk"),
        WarningDangerousMountainRoadLeft("TODO"),
        WarningDangerousMountainRoadRight("TODO"),
        WarningDomesticAnimals(ukResourceName = "warning_domestic_animals_uk"),
        WarningFallingRocksOrDebrisLeft(ukResourceName = "warning_falling_rocks_or_debris_left_uk"),
        WarningHazardLane("TODO"),
        WarningRailroadCrossingWithoutBarriers(ukResourceName = "warning_railroad_crossing_without_barriers_uk"),
        WarningReverseCurveLeft("TODO"),
        WarningReverseCurveRight("TODO"),
        WarningSoftShoulderLeft(ukResourceName = "warning_soft_shoulder_left_uk"),
        WarningSoftShoulderRight(ukResourceName = "warning_soft_shoulder_right_uk"),
        WarningTunnel("TODO"),
        WarningWaterPavement("TODO"),

        SpeedLimitAdvMax(ukResourceName = "speed_limit_adv_max_uk_"),
        SpeedLimitEndAdv(ukResourceName = "speed_limit_end_adv_uk_"),
        RegulatoryEndLimitedAccessRoad(ukResourceName = "regulatory_end_limited_access_road_uk"),
        RegulatoryEndMotorway(ukResourceName = "regulatory_end_motorway_uk"),
        RegulatoryEquestriansOnly("TODO"),
        RegulatoryGasStation("TODO"),
        InformationHospital(ukResourceName = "information_hospital_uk"),
        InformationLivingStreet(ukResourceName = "information_living_street_uk"),
        RegulatoryMotorway(ukResourceName = "regulatory_motorway_uk"),
        RegulatorySharedLaneBicyclesPedestrians(ukResourceName = "regulatory_shared_lane_bicycles_pedestrians_uk"),
        RegulatoryEndPriorityRoad(ukResourceName = "regulatory_end_priority_road_uk"),
        RegulatoryEndProhibition(ukResourceName = "regulatory_end_prohibition_uk"),
        RegulatoryGiveWayToOncomingTraffic(ukResourceName = "regulatory_give_way_to_oncoming_traffic_uk"),
        RegulatoryMinSafeDist(ukResourceName = "regulatory_min_safe_dist_uk"),
        RegulatoryNoDangerGoods(ukResourceName = "regulatory_no_danger_goods_uk"),
        RegulatoryNoOverHeavy(ukResourceName = "regulatory_no_over_heavy_uk"),
        RegulatoryPriorityOverOncomingTraffic(ukResourceName = "regulatory_priority_over_oncoming_traffic_uk"),
        RegulatoryPriorityRoad(ukResourceName = "regulatory_priority_road_uk"),
        RegulatoryWeightLimit(ukResourceName = "regulatory_weight_limit_uk"),
        WarningDangerousCrosswinds(ukResourceName = "warning_dangerous_crosswinds_uk"),
        WarningIcyRoad(ukResourceName = "warning_icy_road_uk"),
        WarningLowFlyingAircraft(ukResourceName = "warning_low_flying_aircraft_uk"),
        WarningOpeningOrSwingBridge(ukResourceName = "warning_opening_or_swing_bridge_uk"),
        WarningRailwayCrossingWithBarriers(ukResourceName = "warning_railway_crossing_with_barriers_uk"),
        WarningTrafficQueues(ukResourceName = "warning_traffic_queues_uk")
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
            fun fromNumber(value: Float) = values().firstOrNull { it.value == value.toInt() } ?: Unknown
        }
    }
}
