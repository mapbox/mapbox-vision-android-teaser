package com.mapbox.vision.common.view

import android.animation.Animator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.mabpox.vision.teaser.common.R
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils
import com.mapbox.vision.common.BaseVisionActivity
import com.mapbox.vision.common.models.UiSign
import com.mapbox.vision.common.utils.SoundsPlayer
import com.mapbox.vision.common.utils.classification.SignResources
import com.mapbox.vision.common.utils.classification.Tracker
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.Camera
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.mobile.core.models.FrameStatistics
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications
import com.mapbox.vision.mobile.core.models.detection.DetectionClass
import com.mapbox.vision.mobile.core.models.frame.ImageSize
import com.mapbox.vision.mobile.core.models.position.VehicleState
import com.mapbox.vision.mobile.core.models.road.LaneDirection
import com.mapbox.vision.mobile.core.models.road.LaneEdgeType
import com.mapbox.vision.mobile.core.models.road.RoadDescription
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.safety.core.VisionSafetyListener
import com.mapbox.vision.safety.core.models.CollisionDangerLevel
import com.mapbox.vision.safety.core.models.CollisionObject
import com.mapbox.vision.safety.core.models.RoadRestrictions
import com.mapbox.vision.view.VisionView
import com.mapbox.vision.view.VisualizationMode
import kotlinx.android.synthetic.main.activity_main.back
import kotlinx.android.synthetic.main.activity_main.dashboard_container
import kotlinx.android.synthetic.main.activity_main.detection_button
import kotlinx.android.synthetic.main.activity_main.distance_to_car_label
import kotlinx.android.synthetic.main.activity_main.fps_performance_view
import kotlinx.android.synthetic.main.activity_main.lane_detection_button
import kotlinx.android.synthetic.main.activity_main.lane_detections_container
import kotlinx.android.synthetic.main.activity_main.lane_detections_list
import kotlinx.android.synthetic.main.activity_main.lane_view
import kotlinx.android.synthetic.main.activity_main.root
import kotlinx.android.synthetic.main.activity_main.safety_button
import kotlinx.android.synthetic.main.activity_main.safety_mode_container
import kotlinx.android.synthetic.main.activity_main.safety_view
import kotlinx.android.synthetic.main.activity_main.segmentation_button
import kotlinx.android.synthetic.main.activity_main.sign_info_container
import kotlinx.android.synthetic.main.activity_main.signs_button
import kotlinx.android.synthetic.main.activity_main.speed_limit_current
import kotlinx.android.synthetic.main.activity_main.speed_limit_next
import kotlinx.android.synthetic.main.activity_main.vision_view
import kotlinx.android.synthetic.main.activity_replay.*

abstract class BaseTeaserActivity : BaseVisionActivity() {

    protected abstract fun initVisionManager(visionView: VisionView): Boolean

    protected abstract fun destroyVisionManager()

    protected abstract fun getFrameStatistics(): FrameStatistics

    protected abstract fun initViews(root: View)

    enum class AppMode {
        Segmentation,
        Classification,
        Detection,
        Safety,
        Lanes
    }

    private val signResources: SignResources = SignResources.Impl(this)

    private var signSize = 0
    private var margin = 0

    private var laneHeight = 0

    private var tracker: Tracker<UiSign> = Tracker(5)
    private var appMode: AppMode = AppMode.Detection

    private var isPermissionsGranted = false
    private var visionManagerWasInit = false
    private lateinit var soundsPlayer: SoundsPlayer

    private var country = Country.Unknown

    protected var modelPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)

    private var lastSpeed: Float = 0f
    private var calibrationProgress = 0f

    protected val visionEventsListener = object : VisionEventsListener {

        override fun onCountryUpdated(country: Country) {
            this@BaseTeaserActivity.country = country
        }

        override fun onFrameSignClassificationsUpdated(frameSignClassifications: FrameSignClassifications) {
            if (appMode == AppMode.Classification) {
                runOnUiThread {
                    tracker.update(UiSign.getUiSigns(frameSignClassifications))
                    drawSigns(tracker.getCurrent())
                }
            }
        }

        override fun onRoadDescriptionUpdated(roadDescription: RoadDescription) {
            if (appMode == AppMode.Lanes) {
                runOnUiThread {
                    if (calibrationProgress == 1f) {
                        calibration_progress.hide()

                        lane_detections_list.show()
                        fillLaneList(roadDescription)

                        lane_view.show()
                        lane_view.drawLane(roadDescription.lanes.firstOrNull())
                    } else {
                        calibration_progress.show()
                        lane_view.hide()
                        lane_detections_list.hide()
                    }
                }
            }
        }

        override fun onVehicleStateUpdated(vehicleState: VehicleState) {
            lastSpeed = vehicleState.speed
        }

        override fun onCameraUpdated(camera: Camera) {
            runOnUiThread {
                calibrationProgress = camera.calibrationProgress
                fps_performance_view.setCalibrationProgress(calibrationProgress)
                lane_view.frameSize = ImageSize(
                    imageWidth = camera.frameWidth,
                    imageHeight = camera.frameHeight
                )
                calibration_progress.text = getString(
                    R.string.calibration_progress,
                    (calibrationProgress * 100).toInt()
                )
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onUpdateCompleted() {
            runOnUiThread {
                if (visionManagerWasInit) {
                    fps_performance_view.setFpsStatistics(getFrameStatistics())
                }
            }
        }
    }

    protected val visionSafetyListener = object : VisionSafetyListener {

        private var currentDangerLevel: CollisionDangerLevel = CollisionDangerLevel.None

        private val distanceFormatter by lazy {
            LocaleUtils().let { localeUtils ->
                val language = localeUtils.inferDeviceLanguage(this@BaseTeaserActivity)
                val unitType = localeUtils.getUnitTypeForDeviceLocale(this@BaseTeaserActivity)
                val roundingIncrement = NavigationConstants.ROUNDING_INCREMENT_FIVE
                DistanceFormatter(this@BaseTeaserActivity, language, unitType, roundingIncrement)
            }
        }

        override fun onCollisionsUpdated(collisions: Array<CollisionObject>) {
            if (appMode == AppMode.Safety) {
                runOnUiThread {
                    if (calibrationProgress == 1f) {
                        calibration_progress.hide()
                        distance_to_car_label.show()
                        safety_view.show()

                        val collision =
                            collisions.firstOrNull {
                                it.`object`.objectClass == DetectionClass.Car
                            }

                        if (collision == null) {
                            soundsPlayer.stop()
                            currentDangerLevel = CollisionDangerLevel.None
                            distance_to_car_label.hide()
                            safety_view.hide()
                        } else {
                            if (currentDangerLevel != collision.dangerLevel) {
                                soundsPlayer.stop()
                                when (collision.dangerLevel) {
                                    CollisionDangerLevel.None -> Unit
                                    CollisionDangerLevel.Warning -> soundsPlayer.playWarning()
                                    CollisionDangerLevel.Critical -> soundsPlayer.playCritical()
                                }
                                currentDangerLevel = collision.dangerLevel
                            }

                            distance_to_car_label.show()
                            safety_view.show()
                            distance_to_car_label.text =
                                distanceFormatter.formatDistance(collision.`object`.position.y)

                            when (currentDangerLevel) {
                                CollisionDangerLevel.None -> safety_view.clean()
                                CollisionDangerLevel.Warning -> safety_view.drawWarnings(collisions)
                                CollisionDangerLevel.Critical -> safety_view.drawCritical()
                            }
                        }
                    } else {
                        calibration_progress.show()
                        distance_to_car_label.hide()
                        safety_view.hide()
                    }
                }
            }
        }

        val speedLimitTranslation by lazy {
            resources.getDimension(R.dimen.speed_limit_translation)
        }

        override fun onRoadRestrictionsUpdated(roadRestrictions: RoadRestrictions) {
            runOnUiThread {
                val imageResource = signResources.getSpeedSignResource(
                    UiSign.WithNumber(
                        signType = UiSign.SignType.SpeedLimit,
                        signNumber = UiSign.SignNumber.fromNumber(roadRestrictions.speedLimits.car.max)
                    ),
                    speed = lastSpeed,
                    country = country
                )

                speed_limit_current.animate().cancel()
                speed_limit_next.animate().cancel()

                speed_limit_current.apply {
                    show()
                    translationY = 0f
                    alpha = 1f
                    animate()
                        .translationY(speedLimitTranslation / 2)
                        .alpha(0f)
                        .scaleX(0.5f)
                        .scaleY(0.5f)
                        .setDuration(500L)
                        .setListener(
                            object : Animator.AnimatorListener {
                                override fun onAnimationRepeat(animation: Animator?) {}

                                override fun onAnimationEnd(animation: Animator?) {
                                    setImageResource(imageResource)
                                    translationY = 0f
                                    alpha = 1f
                                    scaleX = 1f
                                    scaleY = 1f
                                    speed_limit_next.hide()
                                }

                                override fun onAnimationCancel(animation: Animator?) {}

                                override fun onAnimationStart(animation: Animator?) {}
                            }
                        )
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }

                if (roadRestrictions.speedLimits.car.max != 0f) {
                    speed_limit_next.apply {
                        translationY = -speedLimitTranslation
                        setImageResource(imageResource)
                        show()
                        animate().translationY(0f)
                            .setDuration(500L)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    }
                } else {
                    speed_limit_next.hide()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        soundsPlayer = SoundsPlayer(this)
    }

    override fun setLayout() {
        setContentView(R.layout.activity_main)
    }

    override fun onPermissionsGranted() {
        isPermissionsGranted = true

        signSize = resources.getDimension(R.dimen.dp64).toInt()
        laneHeight = resources.getDimension(R.dimen.dp40).toInt()
        margin = resources.getDimension(R.dimen.dp8).toInt()

        back.setOnClickListener { onBackClick() }
        segmentation_button.setOnClickListener { setAppMode(AppMode.Segmentation) }
        signs_button.setOnClickListener { setAppMode(AppMode.Classification) }
        detection_button.setOnClickListener { setAppMode(AppMode.Detection) }
        safety_button.setOnClickListener { setAppMode(AppMode.Safety) }
        lane_detection_button.setOnClickListener { setAppMode(AppMode.Lanes) }

        root.setOnLongClickListener {
            if (fps_performance_view.visibility == View.GONE) {
                fps_performance_view.show()
            } else {
                fps_performance_view.hide()
            }
            return@setOnLongClickListener true
        }
        fps_performance_view.hide()

        tryToInitVisionManager()

        initViews(root)
    }

    private fun tryToInitVisionManager() {
        if (isPermissionsGranted && !visionManagerWasInit) {
            visionManagerWasInit = initVisionManager(vision_view)
        }
    }

    private fun stopVisionManager() {
        if (isPermissionsGranted && visionManagerWasInit) {
            destroyVisionManager()
            visionManagerWasInit = false
        }
    }

    protected fun restartVisionManager() {
        stopVisionManager()
        tryToInitVisionManager()
    }

    override fun onResume() {
        super.onResume()
        tryToInitVisionManager()
        vision_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        stopVisionManager()
        vision_view.onPause()
        soundsPlayer.stop()
    }

    private fun onBackClick() {
        soundsPlayer.stop()
        dashboard_container.show()
        hideLaneDetectionContainer()
        hideSignsContainer()
        safety_mode_container.hide()
        back.hide()
    }

    private fun drawSigns(signsValueUis: List<UiSign>) {
        sign_info_container.removeAllViews()
        for (signValue in signsValueUis) {
            sign_info_container.addView(
                ImageView(this).apply {
                    layoutParams =
                        ViewGroup.MarginLayoutParams(signSize, ViewGroup.LayoutParams.WRAP_CONTENT)
                            .apply {
                                leftMargin = margin
                            }
                    setImageResource(signResources.getSignResource(signValue, country))
                }
            )
        }
    }

    private fun fillLaneList(roadDescription: RoadDescription) {
        fun getImageView(isFirst: Boolean = false): ImageView {
            val image = ImageView(this)
            val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, laneHeight)
            if (isFirst) {
                lp.marginStart = margin
            }
            lp.marginEnd = margin
            image.layoutParams = lp
            return image
        }

        fun LaneEdgeType.toDrawableId(isForward: Boolean = false): Int = when (this) {
            LaneEdgeType.Curb -> if (isForward) {
                R.drawable.ic_right_curb
            } else {
                R.drawable.ic_left_curb
            }
            LaneEdgeType.Construction -> TODO()
            LaneEdgeType.MarkupDashed -> R.drawable.ic_half_lane
            LaneEdgeType.MarkupDoubleSolid -> R.drawable.ic_separator_double_lane
            LaneEdgeType.MarkupSolid -> R.drawable.ic_separator_lane
            LaneEdgeType.Unknown -> R.drawable.ic_unknown_lane
        }

        fun LaneDirection.toDrawableId() = when (this) {
            LaneDirection.Backward -> R.drawable.ic_arrow
            LaneDirection.Forward -> R.drawable.ic_arrow_forward
            LaneDirection.Reverse -> R.drawable.ic_arrow_reversed
            LaneDirection.Unknown -> TODO()
        }

        lane_detections_list.removeAllViews()

        for (index in roadDescription.lanes.indices) {
            val lane = roadDescription.lanes[index]
            val leftMarkingImageView = getImageView(index == 0)
            leftMarkingImageView.setImageResource(lane.leftEdge.type.toDrawableId())
            lane_detections_list.addView(leftMarkingImageView)

            val directionImageView = getImageView()
            if (index == roadDescription.currentLaneIndex) {
                directionImageView.setImageResource(R.drawable.ic_blue_arrow)
            } else {
                directionImageView.setImageResource(lane.direction.toDrawableId())
            }
            lane_detections_list.addView(directionImageView)

            if (index == roadDescription.lanes.lastIndex) {
                val rightMarkingImageView = getImageView()
                rightMarkingImageView.setImageResource(lane.rightEdge.type.toDrawableId(true))
                lane_detections_list.addView(rightMarkingImageView)
            }
        }
    }

    private fun hideSignsContainer() {
        sign_info_container.removeAllViews()
        sign_info_container.hide()
    }

    private fun hideLaneDetectionContainer() {
        lane_detections_list.removeAllViews()
        lane_detections_container.hide()
    }

    private fun setAppMode(mode: AppMode) {
        fps_performance_view.resetAverageFps()
        soundsPlayer.stop()

        back.show()
        dashboard_container.hide()
        safety_mode_container.hide()
        hideLaneDetectionContainer()
        hideSignsContainer()
        calibration_progress.hide()

        appMode = mode
        when (appMode) {
            AppMode.Classification -> {
                vision_view.visualizationMode = VisualizationMode.Clear
                tracker = Tracker(5)
                sign_info_container.show()
            }
            AppMode.Detection -> {
                vision_view.visualizationMode = VisualizationMode.Detection
            }
            AppMode.Segmentation -> {
                vision_view.visualizationMode = VisualizationMode.Segmentation
            }
            AppMode.Safety -> {
                vision_view.visualizationMode = VisualizationMode.Clear
                safety_mode_container.show()
                safety_view.hide()
                calibration_progress.show()
                distance_to_car_label.hide()
            }
            AppMode.Lanes -> {
                vision_view.visualizationMode = VisualizationMode.Clear
                lane_detections_container.show()
                calibration_progress.show()
            }
        }
    }
}
