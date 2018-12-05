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
import com.mapbox.vision.core.utils.SystemInfoUtils
import com.mapbox.vision.core.utils.snapdragon.SupportedSnapdragonBoards
import com.mapbox.vision.corewrapper.update.RoadRestrictionsListener
import com.mapbox.vision.corewrapper.update.VisionEventsListener
import com.mapbox.vision.examples.R
import com.mapbox.vision.examples.activity.ar.ArMapActivity
import com.mapbox.vision.examples.activity.map.MapActivity
import com.mapbox.vision.examples.models.UiSignValueModel
import com.mapbox.vision.examples.utils.SoundsPlayer
import com.mapbox.vision.examples.utils.classification.SignResourceMapper
import com.mapbox.vision.examples.utils.classification.Tracker
import com.mapbox.vision.examples.utils.hide
import com.mapbox.vision.examples.utils.show

import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.view.VisualizationMode
import com.mapbox.vision.visionevents.CalibrationProgress

import com.mapbox.vision.visionevents.LaneDepartureState

import com.mapbox.vision.visionevents.events.classification.SignClassification
import com.mapbox.vision.visionevents.events.detection.Collision
import com.mapbox.vision.visionevents.events.detection.Detections
import com.mapbox.vision.visionevents.events.position.Position
import com.mapbox.vision.visionevents.events.roaddescription.Direction
import com.mapbox.vision.visionevents.events.roaddescription.MarkingType
import com.mapbox.vision.visionevents.events.roaddescription.RoadDescription
import com.mapbox.vision.visionevents.events.roadrestrictions.SpeedLimit
import com.mapbox.vision.visionevents.events.segmentation.SegmentationMask
import com.mapbox.vision.visionevents.events.worlddescription.WorldDescription
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1

        private const val SEGMENTATION_MODE = 0
        private const val CLASSIFICATION_MODE = 1
        private const val DETECTION_MODE = 2
        private const val DISTANCE_TO_CAR_MODE = 3
        private const val LINE_DETECTION_MODE = 4
    }

    private val signResourceMapper: SignResourceMapper = SignResourceMapper.Impl(this)

    private var signSize = 0
    private var margin = 0

    private var lineHeight = 0

    private var tracker: Tracker<UiSignValueModel> = Tracker(5)
    private var currentMode = DETECTION_MODE

    private var isPermissionsGranted = false
    private lateinit var soundsPlayer: SoundsPlayer


    private var currentModelPerformanceConfig: ModelPerformanceConfig = ModelPerformanceConfig.Merged(
            performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
    )

    private var lastSpeed: Double = 0.0

    private val visionEventsListener = object : VisionEventsListener {

        @SuppressLint("SetTextI18n")
        private fun extractFpsInfo() {
            val frameStatistics = VisionManager.getFrameStatistics()

            when (currentModelPerformanceConfig) {
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

        override fun detectionsUpdated(detections: Detections) {
            if (currentMode == DETECTION_MODE) {
                extractFpsInfo()
            }
        }

        override fun segmentationUpdated(segmentationMask: SegmentationMask) {
            if (currentMode == SEGMENTATION_MODE) {
                extractFpsInfo()
            }
        }

        override fun signClassificationUpdated(signClassification: SignClassification) {
            if (currentMode == CLASSIFICATION_MODE) {
                extractFpsInfo()
                with(tracker) {
                    update(UiSignValueModel.getSignValueListBySignClassification(signClassification))
                    drawSigns(getCurrent())
                }
            }
        }

        override fun roadDescriptionUpdated(roadDescription: RoadDescription) {
            if (currentMode == LINE_DETECTION_MODE) {
                extractFpsInfo()
                drawLinesDetection(roadDescription)
            }
        }

        private val distanceFormatter by lazy {
            LocaleUtils().let { localeUtils ->
                val language = localeUtils.inferDeviceLanguage(this@MainActivity)
                val unitType = localeUtils.getUnitTypeForDeviceLocale(this@MainActivity)
                val roundingIncrement = NavigationConstants.ROUNDING_INCREMENT_FIVE
                DistanceFormatter(this@MainActivity, language, unitType, roundingIncrement)
            }
        }


        private var currentCollisionState: Collision.CollisionState = Collision.CollisionState.NOT_TRIGGERED

        private var calibrationProgress: CalibrationProgress = CalibrationProgress(progress = 0, completed = false)

        override fun worldDescriptionUpdated(worldDescription: WorldDescription) {

            if (currentMode == DISTANCE_TO_CAR_MODE) {
                calibration_progress.hide()
                extractFpsInfo()

                if (calibrationProgress.completed) {
                    if (worldDescription.carInFrontIndex < 0 || worldDescription.carInFrontIndex >= worldDescription.objects.size) {
                        soundsPlayer.stop()
                        currentCollisionState = Collision.CollisionState.NOT_TRIGGERED
                        distance_to_car_label.hide()
                        safety_mode.hide()
                        return
                    }

                    val carInFront = worldDescription.objects[worldDescription.carInFrontIndex]
                    val collision = worldDescription.collisions.firstOrNull { it.objectDescription == carInFront }

                    if (collision == null) {
                        soundsPlayer.stop()
                        currentCollisionState = Collision.CollisionState.NOT_TRIGGERED

                    } else {

                        if (currentCollisionState != collision.state) {
                            soundsPlayer.stop()
                            when (collision.state) {
                                Collision.CollisionState.WARNING -> {
                                    soundsPlayer.playWarning()
                                }
                                Collision.CollisionState.CRITICAL -> {
                                    soundsPlayer.playCritical()
                                }
                                else -> {
                                }
                            }
                            currentCollisionState = collision.state
                        }
                    }

                    distance_to_car_label.show()
                    safety_mode.show()
                    distance_to_car_label.text = distanceFormatter.formatDistance(carInFront.distance)

                    when (currentCollisionState) {
                        Collision.CollisionState.NOT_TRIGGERED -> {
                            safety_mode.drawDistanceToCar(carInFront)
                        }
                        Collision.CollisionState.WARNING -> {
                            safety_mode.drawWarnings(
                                    worldDescription.collisions.map { it.objectDescription }
                            )
                        }
                        Collision.CollisionState.CRITICAL -> {
                            safety_mode.drawCritical()
                        }
                    }
                } else {
                    distance_to_car_label.hide()
                    safety_mode.hide()
                    calibration_progress.show()
                    calibration_progress.text = getString(
                            R.string.calibration_progress,
                            calibrationProgress.progress
                    )

                }
            }
        }

        override fun estimatedPositionUpdated(position: Position) {
            lastSpeed = position.speed
        }

        override fun calibrationProgressUpdated(calibrationProgress: CalibrationProgress) {
            this.calibrationProgress = calibrationProgress
        }

        override fun laneDepartureStateUpdated(laneDepartureState: LaneDepartureState) {
            if (currentMode == LINE_DETECTION_MODE) {
                extractFpsInfo()
                when (laneDepartureState) {
                    LaneDepartureState.Alert -> {
                        line_departure.show()
                        soundsPlayer.playLaneDepartureWarning()
                    }
                    else -> {
                        soundsPlayer.stop()
                        line_departure.hide()
                    }
                }
            }
        }
    }

    val currentSpeedLimit: SpeedLimit? = null
    val speedLimitTranslation by lazy {
        resources.getDimension(R.dimen.speed_limit_translation)
    }

    private val speedLimitListener = object : RoadRestrictionsListener {

        override fun speedLimitUpdated(speedLimit: SpeedLimit) {
            if (currentSpeedLimit == speedLimit) {
                return
            }

            val imageResource = signResourceMapper.getSignResourceForCurrentSpeed(
                    UiSignValueModel(
                            signType = UiSignValueModel.SignType.SpeedLimit,
                            signNum = UiSignValueModel.SignNumber.fromNumber(speedLimit.maxSpeed.toDouble())
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

            if (speedLimit.maxSpeed != 0f) {
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

        VisionManager.create()
        VisionManager.setVisionEventListener(visionEventsListener)
        VisionManager.setRoadRestrictionsListener(speedLimitListener)

        soundsPlayer = SoundsPlayer(this)

        if (!allPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE)
            return
        } else {
            onPermissionsGranted()
        }
    }

    private fun onPermissionsGranted() {
        isPermissionsGranted = true

        VisionManager.setModelPerformanceConfig(
                currentModelPerformanceConfig
        )

        signSize = resources.getDimension(R.dimen.dp64).toInt()
        lineHeight = resources.getDimension(R.dimen.dp40).toInt()
        margin = resources.getDimension(R.dimen.dp8).toInt()

        back.setOnClickListener { onBackClick() }
        segm_container.setOnClickListener { setSegmentationMode() }
        sign_detection_container.setOnClickListener { setSignClassificationMode() }
        det_container.setOnClickListener { setDetectionMode() }
        distance_container.setOnClickListener { setSafetyMode() }
        safety_mode_container.hide()
        line_detection_container.setOnClickListener { setLineDetectionMode() }
        object_mapping_button_container.setOnClickListener {
            startActivity(MapActivity.createIntent(this))
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

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allPermissionsGranted() && requestCode == PERMISSIONS_REQUEST_CODE) {
            onPermissionsGranted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionsGranted) {
            VisionManager.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isPermissionsGranted) {
            VisionManager.stop()
        }
        soundsPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        VisionManager.destroy()
    }

    private fun onBackClick() {
        dashboard_container.show()
        hideLineDetectionContainer()
        hideSignsContainer()
        safety_mode_container.hide()
        back.hide()
    }

    private fun drawSigns(signsValueUis: List<UiSignValueModel>) {
        sign_info_container.removeAllViews()
        for (signValue in signsValueUis) {
            val image = ImageView(this)
            val lp = ViewGroup.MarginLayoutParams(signSize, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.leftMargin = margin
            image.layoutParams = lp

            image.setImageResource(signResourceMapper.getSignResource(signValue))

            // Adds the view to the layout
            sign_info_container.addView(image)
        }
    }

    private fun drawLinesDetection(roadDescription: RoadDescription) {

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

        fun MarkingType.imageSource(isForward: Boolean = false) = when (this) {
            MarkingType.CURB -> if (isForward) {
                R.drawable.ic_right_curb
            } else {
                R.drawable.ic_left_curb
            }
            MarkingType.SOLID -> R.drawable.ic_separator_lane
            MarkingType.DOUBLE_SOLID -> R.drawable.ic_separator_double_lane
            MarkingType.DASHES -> R.drawable.ic_half_lane
            MarkingType.UNKNOWN -> R.drawable.ic_unknown_lane
        }

        fun Direction.imageSource() = when (this) {
            Direction.FORWARD -> R.drawable.ic_arrow_forward
            Direction.BACKWARD -> R.drawable.ic_arrow
            Direction.REVERS -> R.drawable.ic_arrow_reversed
        }

        lines_detections_container.removeAllViews()

        for (index in roadDescription.lines.indices) {
            val line = roadDescription.lines[index]
            val leftMarkingImageView = getImageView(index == 0)
            leftMarkingImageView.setImageResource(line.leftMarking.type.imageSource())
            lines_detections_container.addView(leftMarkingImageView)

            val directionImageView = getImageView()
            if (index == roadDescription.currentLane) {
                directionImageView.setImageResource(R.drawable.ic_blue_arrow)
            } else {
                directionImageView.setImageResource(line.direction.imageSource())
            }
            lines_detections_container.addView(directionImageView)

            if (index == roadDescription.lines.lastIndex) {
                val rightMarkingImageView = getImageView()
                rightMarkingImageView.setImageResource(line.rightMarking.type.imageSource(true))
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
        currentModelPerformanceConfig = ModelPerformanceConfig.Merged(
                performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
                currentModelPerformanceConfig
        )
        vision_view.visualizationMode = VisualizationMode.CLEAR
        currentMode = CLASSIFICATION_MODE

        tracker = Tracker(5)

        hideLineDetectionContainer()
        sign_info_container.show()
        dashboard_container.hide()
        safety_mode_container.hide()
        back.show()
    }

    private fun setDetectionMode() {
        soundsPlayer.stop()
        currentModelPerformanceConfig = ModelPerformanceConfig.Merged(
                performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
                currentModelPerformanceConfig
        )

        vision_view.visualizationMode = VisualizationMode.DETECTION
        currentMode = DETECTION_MODE

        hideLineDetectionContainer()
        hideSignsContainer()
        dashboard_container.hide()
        safety_mode_container.hide()
        back.show()
    }

    private fun setSegmentationMode() {
        soundsPlayer.stop()
        currentModelPerformanceConfig = ModelPerformanceConfig.Merged(
                performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
                currentModelPerformanceConfig
        )

        vision_view.visualizationMode = VisualizationMode.SEGMENTATION
        currentMode = SEGMENTATION_MODE

        hideLineDetectionContainer()
        hideSignsContainer()
        dashboard_container.hide()
        safety_mode_container.hide()
        back.show()
    }

    private fun setSafetyMode() {
        soundsPlayer.stop()
        currentModelPerformanceConfig = ModelPerformanceConfig.Separate(
                detectionPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH),
                segmentationPerformance = ModelPerformance.Off
        )
        VisionManager.setModelPerformanceConfig(
                currentModelPerformanceConfig

        )

        vision_view.visualizationMode = VisualizationMode.CLEAR
        currentMode = DISTANCE_TO_CAR_MODE

        hideLineDetectionContainer()
        hideSignsContainer()
        dashboard_container.hide()
        safety_mode_container.show()
        back.show()
    }

    private fun setLineDetectionMode() {

        soundsPlayer.stop()
        currentModelPerformanceConfig = ModelPerformanceConfig.Separate(
                detectionPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.LOW),
                segmentationPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
        )
        VisionManager.setModelPerformanceConfig(
                currentModelPerformanceConfig

        )

        vision_view.visualizationMode = VisualizationMode.CLEAR
        currentMode = LINE_DETECTION_MODE

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
