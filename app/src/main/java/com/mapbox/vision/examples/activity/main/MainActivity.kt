package com.mapbox.vision.examples.activity.main

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils
import com.mapbox.vision.VisionManager
import com.mapbox.vision.examples.R
import com.mapbox.vision.examples.activity.ar.ArMapActivity
import com.mapbox.vision.examples.activity.map.MapActivity
import com.mapbox.vision.examples.models.UiSign
import com.mapbox.vision.examples.utils.SoundsPlayer
import com.mapbox.vision.examples.utils.classification.SignResourceMapper
import com.mapbox.vision.examples.utils.classification.Tracker
import com.mapbox.vision.examples.utils.hide
import com.mapbox.vision.examples.utils.show
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.Camera
import com.mapbox.vision.mobile.core.models.FrameSegmentation
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications
import com.mapbox.vision.mobile.core.models.detection.DetectionClass
import com.mapbox.vision.mobile.core.models.detection.FrameDetections
import com.mapbox.vision.mobile.core.models.position.VehicleState
import com.mapbox.vision.mobile.core.models.road.LaneDirection
import com.mapbox.vision.mobile.core.models.road.LaneEdgeType
import com.mapbox.vision.mobile.core.models.road.RoadDescription
import com.mapbox.vision.mobile.core.utils.SystemInfoUtils
import com.mapbox.vision.mobile.core.utils.snapdragon.SupportedSnapdragonBoards
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.safety.core.VisionSafetyListener
import com.mapbox.vision.safety.core.models.CollisionDangerLevel
import com.mapbox.vision.safety.core.models.CollisionObject
import com.mapbox.vision.safety.core.models.RoadRestrictions
import com.mapbox.vision.view.VisualizationMode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
    }

    enum class AppMode {
        Segmentation,
        Classification,
        Detection,
        Safety,
        Lanes
    }

    private val signResourceMapper: SignResourceMapper = SignResourceMapper.Impl(this)

    private var signSize = 0
    private var margin = 0

    private var lineHeight = 0

    private var tracker: Tracker<UiSign> = Tracker(5)
    private var appMode: AppMode = AppMode.Detection

    private var isPermissionsGranted = false
    private var visionManagerWasInit = false
    private lateinit var soundsPlayer: SoundsPlayer

    private var appModelPerformanceConfig: ModelPerformanceConfig = ModelPerformanceConfig.Merged(
        performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
    )

    private var lastSpeed: Float = 0f
    private var calibrationProgress = 0f

    private val visionEventsListener = object : VisionEventsListener {

        override fun onFrameDetectionsUpdated(frameDetections: FrameDetections) {
            vision_view.setDetections(frameDetections)
        }

        override fun onFrameSegmentationUpdated(frameSegmentation: FrameSegmentation) {
            vision_view.setSegmentation(frameSegmentation)
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
                    drawLanesDetection(roadDescription)
                }
            }
        }

        override fun onVehicleStateUpdated(vehicleState: VehicleState) {
            lastSpeed = vehicleState.speed
        }

        override fun onCameraUpdated(camera: Camera) {
            calibrationProgress = camera.calibrationProgress
        }

        @SuppressLint("SetTextI18n")
        override fun onUpdateCompleted() {
            val frameStatistics = VisionManager.getFrameStatistics()
            runOnUiThread {
                when (appModelPerformanceConfig) {
                    is ModelPerformanceConfig.Merged -> {
                        segmentation_fps.text = "S: 0"
                        detection_fps.text = "D: 0"
                        merge_model_fps.text = "MM: ${frameStatistics.segmentationDetectionFPS}"
                    }
                    is ModelPerformanceConfig.Separate -> {
                        segmentation_fps.text = "S: ${frameStatistics.segmentationFPS}"
                        detection_fps.text = "D: ${frameStatistics.detectionFPS}"
                        merge_model_fps.text = "MM: 0"
                    }
                }
                core_update_fps.text = "CU: ${frameStatistics.coreUpdateFPS}"
            }
        }
    }

    private val visionSafetyListener = object : VisionSafetyListener {

        private var currentDangerLevel: CollisionDangerLevel = CollisionDangerLevel.None

        private val distanceFormatter by lazy {
            LocaleUtils().let { localeUtils ->
                val language = localeUtils.inferDeviceLanguage(this@MainActivity)
                val unitType = localeUtils.getUnitTypeForDeviceLocale(this@MainActivity)
                val roundingIncrement = NavigationConstants.ROUNDING_INCREMENT_FIVE
                DistanceFormatter(this@MainActivity, language, unitType, roundingIncrement)
            }
        }

        override fun onCollisionsUpdated(collisions: Array<CollisionObject>) {
            if (appMode == AppMode.Safety) {
                runOnUiThread {

                    if (calibrationProgress == 1f) {
                        distance_to_car_label.show()
                        safety_mode.show()
                        calibration_progress.hide()

                        val collision = collisions.firstOrNull { it.`object`.objectClass == DetectionClass.Car }
                        if (collision == null) {
                            soundsPlayer.stop()
                            currentDangerLevel = CollisionDangerLevel.None
                            distance_to_car_label.hide()
                            safety_mode.hide()
                        } else {

                            if (currentDangerLevel != collision.dangerLevel) {
                                soundsPlayer.stop()
                                when (collision.dangerLevel) {
                                    CollisionDangerLevel.Warning -> {
                                        soundsPlayer.playWarning()
                                    }
                                    CollisionDangerLevel.Critical -> {
                                        soundsPlayer.playCritical()
                                    }
                                    else -> {
                                    }
                                }
                                currentDangerLevel = collision.dangerLevel
                            }

                            distance_to_car_label.show()
                            safety_mode.show()
                            distance_to_car_label.text = distanceFormatter.formatDistance(collision.`object`.position.y)

                            when (currentDangerLevel) {
                                CollisionDangerLevel.None -> Unit
                                CollisionDangerLevel.Warning -> {
                                    safety_mode.drawWarnings(collisions)
                                }
                                CollisionDangerLevel.Critical -> {
                                    safety_mode.drawCritical()
                                }
                            }
                        }
                    } else {
                        distance_to_car_label.hide()
                        safety_mode.hide()
                        calibration_progress.show()
                        calibration_progress.text = getString(
                            R.string.calibration_progress,
                            (calibrationProgress * 100).toInt()
                        )
                    }
                }
            }
        }

        val speedLimitTranslation by lazy {
            resources.getDimension(R.dimen.speed_limit_translation)
        }

        override fun onRoadRestrictionsUpdated(roadRestrictions: RoadRestrictions) {
            val imageResource = signResourceMapper.getSignResourceForCurrentSpeed(
                UiSign(
                    signType = UiSign.SignType.SpeedLimit,
                    signNum = UiSign.SignNumber.fromNumber(roadRestrictions.speedLimits.car.max)
                ),
                speed = lastSpeed
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

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!SupportedSnapdragonBoards.isBoardSupported(SystemInfoUtils.getSnpeSupportedBoard())) {
            val text =
                Html.fromHtml("The device is not supported, you need <b>Snapdragon-powered</b> device with <b>OpenCL</b> support, more details at <b>https://www.mapbox.com/android-docs/vision/overview/</b>")
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        soundsPlayer = SoundsPlayer(this)

        if (!allPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE)
        } else {
            onPermissionsGranted()
        }
    }

    private fun onPermissionsGranted() {
        isPermissionsGranted = true

        signSize = resources.getDimension(R.dimen.dp64).toInt()
        lineHeight = resources.getDimension(R.dimen.dp40).toInt()
        margin = resources.getDimension(R.dimen.dp8).toInt()

        back.setOnClickListener { onBackClick() }
        segm_container.setOnClickListener { setSegmentationMode() }
        sign_detection_container.setOnClickListener { setSignClassificationMode() }
        det_container.setOnClickListener { setDetectionMode() }
        distance_container.setOnClickListener { setSafetyMode() }
        safety_mode_container.hide()
        line_detection_container.setOnClickListener { setLaneDetectionMode() }
        object_mapping_button_container.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        ar_navigation_button_container.setOnClickListener {
            startActivity(Intent(this, ArMapActivity::class.java))
        }
        root.setOnLongClickListener {
            if (fps_info_container.visibility == View.GONE) {
                fps_info_container.show()
            } else {
                fps_info_container.hide()
            }
            return@setOnLongClickListener true

        }
        fps_info_container.hide()

        tryToInitVisionManager()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allPermissionsGranted() && requestCode == PERMISSIONS_REQUEST_CODE) {
            onPermissionsGranted()
        }
    }

    private fun tryToInitVisionManager() {
        if (isPermissionsGranted && !visionManagerWasInit) {
            VisionManager.create()
            VisionManager.start(visionEventsListener)
            VisionManager.setModelPerformanceConfig(appModelPerformanceConfig)
            VisionManager.setVideoSourceListener(vision_view)

            VisionSafetyManager.create(VisionManager, visionSafetyListener)

            visionManagerWasInit = true
        }
    }

    override fun onStart() {
        super.onStart()
        tryToInitVisionManager()
    }

    override fun onStop() {
        super.onStop()

        if (isPermissionsGranted && visionManagerWasInit) {
            VisionSafetyManager.destroy()
            VisionManager.stop()
            VisionManager.destroy()
            visionManagerWasInit = false
        }
        soundsPlayer.stop()
    }

    private fun onBackClick() {
        soundsPlayer.stop()
        dashboard_container.show()
        hideLineDetectionContainer()
        hideSignsContainer()
        safety_mode_container.hide()
        back.hide()
    }

    private fun drawSigns(signsValueUis: List<UiSign>) {
        sign_info_container.removeAllViews()
        for (signValue in signsValueUis) {
            sign_info_container.addView(
                ImageView(this).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(signSize, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        leftMargin = margin
                    }
                    setImageResource(signResourceMapper.getSignResource(signValue))
                }
            )
        }
    }

    private fun drawLanesDetection(roadDescription: RoadDescription) {
        fun getImageView(isFirst: Boolean = false): ImageView {
            val image = ImageView(this)
            val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, lineHeight)
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

        lines_detections_container.removeAllViews()

        for (index in roadDescription.lanes.indices) {
            val lane = roadDescription.lanes[index]
            val leftMarkingImageView = getImageView(index == 0)
            leftMarkingImageView.setImageResource(lane.leftEdge.type.toDrawableId())
            lines_detections_container.addView(leftMarkingImageView)

            val directionImageView = getImageView()
            if (index == roadDescription.currentLaneIndex) {
                directionImageView.setImageResource(R.drawable.ic_blue_arrow)
            } else {
                directionImageView.setImageResource(lane.direction.toDrawableId())
            }
            lines_detections_container.addView(directionImageView)

            if (index == roadDescription.lanes.lastIndex) {
                val rightMarkingImageView = getImageView()
                rightMarkingImageView.setImageResource(lane.rightEdge.type.toDrawableId(true))
                lines_detections_container.addView(rightMarkingImageView)
            }
        }
    }

    private fun hideSignsContainer() {
        sign_info_container.removeAllViews()
        sign_info_container.hide()
    }

    private fun hideLineDetectionContainer() {
        line_departure.hide()
        lines_detections_container.removeAllViews()
        lines_detections_container.hide()
    }

    private fun setSignClassificationMode() {
        soundsPlayer.stop()
        appModelPerformanceConfig = ModelPerformanceConfig.Merged(
            performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
            appModelPerformanceConfig
        )
        vision_view.visualizationMode = VisualizationMode.Clear
        appMode = MainActivity.AppMode.Classification

        tracker = Tracker(5)

        hideLineDetectionContainer()
        sign_info_container.show()
        dashboard_container.hide()
        safety_mode_container.hide()
        back.show()
    }

    private fun setDetectionMode() {
        soundsPlayer.stop()
        appModelPerformanceConfig = ModelPerformanceConfig.Merged(
            performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
            appModelPerformanceConfig
        )

        vision_view.visualizationMode = VisualizationMode.Detections
        appMode = MainActivity.AppMode.Detection

        hideLineDetectionContainer()
        hideSignsContainer()
        dashboard_container.hide()
        safety_mode_container.hide()
        back.show()
    }

    private fun setSegmentationMode() {
        soundsPlayer.stop()
        appModelPerformanceConfig = ModelPerformanceConfig.Merged(
            performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
            appModelPerformanceConfig
        )

        vision_view.visualizationMode = VisualizationMode.Segmentation
        appMode = MainActivity.AppMode.Segmentation

        hideLineDetectionContainer()
        hideSignsContainer()
        dashboard_container.hide()
        safety_mode_container.hide()
        back.show()
    }

    private fun setSafetyMode() {
        soundsPlayer.stop()
        appModelPerformanceConfig = ModelPerformanceConfig.Separate(
            detectionPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH),
            segmentationPerformance = ModelPerformance.Off
        )
        VisionManager.setModelPerformanceConfig(
            appModelPerformanceConfig
        )

        vision_view.visualizationMode = VisualizationMode.Clear
        appMode = MainActivity.AppMode.Safety

        hideLineDetectionContainer()
        hideSignsContainer()
        dashboard_container.hide()
        safety_mode_container.show()
        calibration_progress.hide()
        distance_to_car_label.hide()
        safety_mode.hide()
        back.show()
    }

    private fun setLaneDetectionMode() {
        soundsPlayer.stop()
        appModelPerformanceConfig = ModelPerformanceConfig.Separate(
            detectionPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.LOW),
            segmentationPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
            appModelPerformanceConfig
        )

        vision_view.visualizationMode = VisualizationMode.Clear
        appMode = MainActivity.AppMode.Lanes

        hideSignsContainer()
        dashboard_container.hide()
        lines_detections_container.show()
        safety_mode_container.hide()
        back.show()
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun getRequiredPermissions(): Array<String> {
        return try {
            val info = packageManager?.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val ps = info!!.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                emptyArray()
            }
        } catch (e: Exception) {
            emptyArray()
        }
    }
}
