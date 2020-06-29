package com.mapbox.vision.common.view

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import com.mabpox.vision.teaser.common.R
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
import com.mapbox.vision.view.DragRectView
import com.mapbox.vision.view.VisionView
import com.mapbox.vision.view.VisualizationMode
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseTeaserActivity : BaseVisionActivity() {

    protected abstract fun initVisionManager(visionView: VisionView): Boolean

    protected abstract fun destroyVisionManager()

    protected abstract fun getFrameStatistics(): FrameStatistics

    protected abstract fun initViews(root: View)

    enum class AppMode {
        Segmentation,
        Classification,
        Detection,
        BackDetection,
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

    private var selectedRevertedArea: RectF? = null
    private val dragCallback = object : DragRectView.Callback {

        override fun onRectFinished(rect: Rect?) {
            val revertRect = Rect(
                rect!!.left,
                vision_view.height - rect!!.top,
                rect!!.right,
                vision_view.height - rect!!.bottom
            )

            selectedRevertedArea = RectF(
                (revertRect.left.toFloat() / vision_view.width).coerceIn(0f, 1f),
                (revertRect.top.toFloat() / vision_view.height).coerceIn(0f, 1f),
                (revertRect.right.toFloat() / vision_view.width).coerceIn(0f, 1f),
                (revertRect.bottom.toFloat() / vision_view.height).coerceIn(0f, 1f)
            )
        }

        override fun onRectOnScreen(onScreen: Boolean) {
            if (onScreen) {
                btn_confirm_area.show()
            } else {
                btn_confirm_area.hide()
            }
        }
    }

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
                debug_view.setCalibrationProgress(calibrationProgress)
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
                    debug_view.setFpsStatistics(getFrameStatistics())
                }
            }
        }
    }

    protected val safetyListener = object : VisionSafetyListener {

        override fun onCollisionsUpdated(collisions: Array<CollisionObject>) {}

        override fun onBackCollisionsUpdated(collisions: Array<CollisionObject>) {
            findViewById<VisionView>(R.id.vision_view).setCustomDetections(
                warningDetections = collisions
                    .filter {
                        it.dangerLevel == CollisionDangerLevel.Warning
                    }
                    .map { it.lastDetection }
                    .toTypedArray(),
                dangerDetections = collisions
                    .filter {
                        it.dangerLevel == CollisionDangerLevel.Critical
                    }
                    .map { it.lastDetection }
                    .toTypedArray()
            )
        }

        override fun onRoadRestrictionsUpdated(roadRestrictions: RoadRestrictions) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        soundsPlayer = SoundsPlayer(this)
    }

    override fun setLayout() {
        setContentView(R.layout.activity_main)

        btn_confirm_area.setOnClickListener {
            btn_confirm_area.hide()
            drag_view.clear()
            drag_view.setCallback(null)
            vision_view.setNewRenderArea(selectedRevertedArea)
            lane_view.setSelectedArea(selectedRevertedArea)
            btn_reset_area.show()

            drag_view.hide()
        }

        btn_reset_area.setOnClickListener {
            selectedRevertedArea = null
            vision_view.setNewRenderArea(selectedRevertedArea)
            lane_view.setSelectedArea(selectedRevertedArea)
            drag_view.setCallback(dragCallback)
            btn_reset_area.hide()

            drag_view.show()
        }

        btn_confirm_area.hide()
        btn_reset_area.hide()
    }

    override fun onPermissionsGranted() {
        isPermissionsGranted = true

        signSize = resources.getDimension(R.dimen.dp64).toInt()
        laneHeight = resources.getDimension(R.dimen.dp40).toInt()
        margin = resources.getDimension(R.dimen.dp8).toInt()

        back.setOnClickListener { onBackClick() }
        segmentation_button.setOnClickListener { setAppMode(AppMode.Segmentation) }
        signs_button.setOnClickListener { setAppMode(AppMode.Classification) }
        detection_button.setOnClickListener { setAppMode(AppMode.BackDetection) }
        safety_button.setOnClickListener { setAppMode(AppMode.Safety) }
        lane_detection_button.setOnClickListener { setAppMode(AppMode.Lanes) }

        root.setOnLongClickListener {
            if (debug_view.visibility == View.GONE) {
                debug_view.show()
            } else {
                debug_view.hide()
            }
            return@setOnLongClickListener true
        }

        debug_view.findViewById<Switch>(R.id.detections_switch_bg).setOnCheckedChangeListener { buttonView, isChecked ->
            vision_view.drawBackgroundDetections = isChecked
        }
        debug_view.findViewById<Switch>(R.id.detections_switch_warning).setOnCheckedChangeListener { buttonView, isChecked ->
            vision_view.drawWarningDetections = isChecked
        }
        debug_view.findViewById<Switch>(R.id.detections_switch_critical).setOnCheckedChangeListener { buttonView, isChecked ->
            vision_view.drawDangerDetections = isChecked
        }

        tryToInitVisionManager()

        initViews(root)
    }

    private fun tryToInitVisionManager() {
        if (isPermissionsGranted && !visionManagerWasInit) {
            drag_view.setCallback(this.dragCallback)
            visionManagerWasInit = initVisionManager(vision_view)
        }
    }

    private fun stopVisionManager() {
        if (isPermissionsGranted && visionManagerWasInit) {
            drag_view.setCallback(null)
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

    protected fun setAppMode(mode: AppMode) {
        debug_view.resetAverageFps()
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
            AppMode.Detection,
            AppMode.BackDetection -> {
                vision_view.visualizationMode = VisualizationMode.BackDetection
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
