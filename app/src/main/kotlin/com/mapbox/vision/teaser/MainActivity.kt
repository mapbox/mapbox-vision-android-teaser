package com.mapbox.vision.teaser

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.setPadding
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils
import com.mapbox.vision.VisionManager
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications
import com.mapbox.vision.mobile.core.models.detection.DetectionClass
import com.mapbox.vision.mobile.core.models.position.VehicleState
import com.mapbox.vision.mobile.core.models.road.LaneDirection
import com.mapbox.vision.mobile.core.models.road.LaneEdgeType
import com.mapbox.vision.mobile.core.models.road.RoadDescription
import com.mapbox.vision.mobile.core.utils.SystemInfoUtils
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.teaser.view.show
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.teaser.MainActivity.VisionManagerMode.Camera
import com.mapbox.vision.teaser.MainActivity.VisionManagerMode.Replay
import com.mapbox.vision.safety.core.VisionSafetyListener
import com.mapbox.vision.safety.core.models.CollisionDangerLevel
import com.mapbox.vision.safety.core.models.CollisionObject
import com.mapbox.vision.safety.core.models.RoadRestrictions
import com.mapbox.vision.teaser.ar.ArMapActivity
import com.mapbox.vision.teaser.models.UiSign
import com.mapbox.vision.teaser.recorder.RecorderFragment
import com.mapbox.vision.teaser.replayer.ArReplayNavigationActivity
import com.mapbox.vision.teaser.replayer.ReplayModeFragment
import com.mapbox.vision.teaser.utils.SoundsPlayer
import com.mapbox.vision.teaser.utils.classification.SignResources
import com.mapbox.vision.teaser.utils.classification.Tracker
import com.mapbox.vision.teaser.utils.dpToPx
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.utils.VisionLogger
import com.mapbox.vision.view.VisionView
import com.mapbox.vision.view.VisualizationMode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ReplayModeFragment.OnSelectModeItemListener {

    enum class VisionManagerMode {
        Camera,
        Replay,
    }

    enum class VisionMode {
        Segmentation,
        Classification,
        Detection,
        Safety,
        Lanes,
    }

    companion object {
        private val BASE_SESSION_PATH = "${Environment.getExternalStorageDirectory().absolutePath}/MapboxVisionTelemetry"
        private const val PERMISSION_FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
        private const val PERMISSIONS_REQUEST_CODE = 123
    }

    private var visionManagerMode = Camera
    private var sessionPath = ""

    private val signResources: SignResources = SignResources.Impl(this)

    private var signSize = 0
    private var margin = 0

    private var lineHeight = 0

    private var tracker: Tracker<UiSign> = Tracker(5)
    private var visionMode: VisionMode = VisionMode.Detection
    private var isPermissionsGranted = false
    private var visionManagerWasInit = false
    private lateinit var soundsPlayer: SoundsPlayer
    private var country = Country.Unknown
    private var modelPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
    private var lastSpeed: Float = 0f
    private var calibrationProgress = 0f

    private val visionEventsListener = object : VisionEventsListener {

        override fun onCountryUpdated(country: Country) {
            this@MainActivity.country = country
        }

        override fun onFrameSignClassificationsUpdated(frameSignClassifications: FrameSignClassifications) {
            if (visionMode == VisionMode.Classification) {
                runOnUiThread {
                    tracker.update(UiSign.getUiSigns(frameSignClassifications))
                    drawSigns(tracker.getCurrent())
                }
            }
        }

        override fun onRoadDescriptionUpdated(roadDescription: RoadDescription) {
            if (visionMode == VisionMode.Lanes) {
                runOnUiThread {
                    drawLanesDetection(roadDescription)
                }
            }
        }

        override fun onVehicleStateUpdated(vehicleState: VehicleState) {
            lastSpeed = vehicleState.speed
        }

        override fun onCameraUpdated(camera: com.mapbox.vision.mobile.core.models.Camera) {
            calibrationProgress = camera.calibrationProgress
            runOnUiThread {
                fps_performance_view.setCalibrationProgress(calibrationProgress)
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onUpdateCompleted() {
            runOnUiThread {
                if (visionManagerWasInit) {
                    val frameStatistics = when (visionManagerMode) {
                        Camera -> VisionManager.getFrameStatistics()
                        Replay -> VisionReplayManager.getFrameStatistics()
                    }
                    fps_performance_view.showInfo(frameStatistics)
                }
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
            if (visionMode == VisionMode.Safety) {
                runOnUiThread {

                    if (calibrationProgress == 1f) {
                        distance_to_car_label.show()
                        safety_mode.show()
                        calibration_progress.hide()

                        val collision =
                                collisions.firstOrNull { it.`object`.objectClass == DetectionClass.Car }
                        if (collision == null) {
                            soundsPlayer.stop()
                            currentDangerLevel = CollisionDangerLevel.None
                            distance_to_car_label.hide()
                            safety_mode.hide()
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
                            safety_mode.show()
                            distance_to_car_label.text =
                                    distanceFormatter.formatDistance(collision.`object`.position.y)

                            when (currentDangerLevel) {
                                CollisionDangerLevel.None -> safety_mode.clean()
                                CollisionDangerLevel.Warning -> safety_mode.drawWarnings(collisions)
                                CollisionDangerLevel.Critical -> safety_mode.drawCritical()
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)

        if (!SystemInfoUtils.isVisionSupported()) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.vision_not_supported_title)
                    .setView(
                            TextView(this).apply {
                                setPadding(dpToPx(20f).toInt())
                                movementMethod = LinkMovementMethod.getInstance()
                                isClickable = true
                                text = HtmlCompat.fromHtml(
                                        getString(R.string.vision_not_supported_message),
                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                )
                            }
                    )
                    .setCancelable(false)
                    .show()

            VisionLogger.e(
                    "BoardNotSupported",
                    "System Info: [${SystemInfoUtils.obtainSystemInfo()}]"
            )
        }

        setContentView(R.layout.activity_main)

        if (!allPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE)
        } else {
            onPermissionsGranted()
        }

        soundsPlayer = SoundsPlayer(this)
    }

    private fun onPermissionsGranted() {
        isPermissionsGranted = true

        signSize = dpToPx(64f).toInt()
        lineHeight = dpToPx(40f).toInt()
        margin = dpToPx(8f).toInt()

        back.setOnClickListener { onBackClick() }
        segm_container.setOnClickListener { setVisionMode(VisionMode.Segmentation) }
        sign_detection_container.setOnClickListener { setVisionMode(VisionMode.Classification) }
        det_container.setOnClickListener { setVisionMode(VisionMode.Detection) }
        distance_container.setOnClickListener { setVisionMode(VisionMode.Safety) }
        safety_mode_container.hide()
        line_detection_container.setOnClickListener { setVisionMode(VisionMode.Lanes) }

        initRootLongTap()
        fps_performance_view.hide()

        initArNavigationButton()
        initReplayModeButton()
        tryToInitVisionManager()
    }

    private fun initRootLongTap() {
        root.setOnLongClickListener {
            if (fps_performance_view.visibility == View.GONE) {
                fps_performance_view.show()
            } else {
                fps_performance_view.hide()
            }
            return@setOnLongClickListener true
        }
    }

    private fun initArNavigationButton() {
        ar_navigation_button_container.setOnClickListener {
            when (visionManagerMode) {
                Camera -> startActivity(Intent(this@MainActivity, ArMapActivity::class.java))
                Replay -> startArSession()
            }
        }
    }

    private fun initReplayModeButton() {
        replay_mode_button_container.setOnClickListener {
            showReplayModeFragment()
        }
    }

    private fun tryToInitVisionManager() {
        if (isPermissionsGranted && !visionManagerWasInit) {
            visionManagerWasInit = when (visionManagerMode) {
                Camera -> initVisionManagerCamera(vision_view)
                Replay -> initVisionManagerReplay(vision_view, sessionPath)
            }
        }
    }

    private fun stopVisionManager() {
        if (isPermissionsGranted && visionManagerWasInit) {
            when (visionManagerMode) {
                Camera -> destroyVisionManagerCamera()
                Replay -> destroyVisionManagerReplay()
            }
            visionManagerWasInit = false
        }
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

    private fun setVisionMode(mode: VisionMode) {
        fps_performance_view.resetAverageFps()
        soundsPlayer.stop()

        back.show()
        dashboard_container.hide()
        safety_mode_container.hide()
        hideLineDetectionContainer()
        hideSignsContainer()

        visionMode = mode
        when (visionMode) {
            VisionMode.Classification -> {
                vision_view.visualizationMode = VisualizationMode.Clear
                tracker = Tracker(5)
                sign_info_container.show()
            }
            VisionMode.Detection -> {
                vision_view.visualizationMode = VisualizationMode.Detection
            }
            VisionMode.Segmentation -> {
                vision_view.visualizationMode = VisualizationMode.Segmentation
            }
            VisionMode.Safety -> {
                vision_view.visualizationMode = VisualizationMode.Clear
                safety_mode_container.show()
                safety_mode.hide()
                calibration_progress.hide()
                distance_to_car_label.hide()
            }
            VisionMode.Lanes -> {
                vision_view.visualizationMode = VisualizationMode.Clear
                lines_detections_container.show()
            }
        }
    }

    private fun initVisionManagerCamera(visionView: VisionView): Boolean {
        VisionManager.create()
        visionView.setVisionManager(VisionManager)
        VisionManager.visionEventsListener = visionEventsListener
        VisionManager.start()
        VisionManager.setModelPerformance(modelPerformance)

        VisionSafetyManager.create(VisionManager)
        VisionSafetyManager.visionSafetyListener = visionSafetyListener
        return true
    }

    private fun destroyVisionManagerCamera() {
        VisionSafetyManager.destroy()
        VisionManager.stop()
        VisionManager.destroy()
    }

    private fun initVisionManagerReplay(visionView: VisionView, sessionPath: String): Boolean {
        if (sessionPath.isEmpty()) {
            return false
        }

        this.sessionPath = sessionPath
        VisionReplayManager.create(sessionPath)
        VisionReplayManager.visionEventsListener = visionEventsListener
        VisionReplayManager.start()
        VisionReplayManager.setModelPerformance(modelPerformance)
        visionView.setVisionManager(VisionReplayManager)

        VisionSafetyManager.create(VisionReplayManager)
        VisionSafetyManager.visionSafetyListener = visionSafetyListener
        return true
    }

    private fun destroyVisionManagerReplay() {
        VisionSafetyManager.destroy()
        VisionReplayManager.stop()
        VisionReplayManager.destroy()
    }

    private fun showReplayModeFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, ReplayModeFragment.newInstance(BASE_SESSION_PATH), ReplayModeFragment.TAG)
                .addToBackStack(ReplayModeFragment.TAG)
                .commit()
        hideDashboardView()
    }

    private fun showRecordingFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, RecorderFragment.newInstance(BASE_SESSION_PATH), RecorderFragment.TAG)
                .addToBackStack(RecorderFragment.TAG)
                .commit()
        hideDashboardView()
    }

    private fun hideDashboardView() {
        dashboard_container.hide()
        title_teaser.hide()
    }

    private fun showDashboardView() {
        dashboard_container.show()
        title_teaser.show()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            if ((fragment is OnBackPressedListener && fragment.onBackPressed()).not()) {
                if (supportFragmentManager.popBackStackImmediate() && supportFragmentManager.backStackEntryCount == 0) {
                    showDashboardView()
                    title_teaser.show()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSessionSelected(sessionName: String) {
        stopVisionManager()
        visionManagerMode = Replay
        sessionPath = "$BASE_SESSION_PATH/$sessionName"
        tryToInitVisionManager()
    }

    override fun onCameraSelected() {
        stopVisionManager()
        visionManagerMode = Camera
        sessionPath = ""
        tryToInitVisionManager()
    }

    override fun onRecordingSelected() {
        onCameraSelected()
        vision_view.visualizationMode = VisualizationMode.Clear
        showRecordingFragment()
    }

    private fun startArSession() {
        if (sessionPath.isEmpty()) {
            Toast.makeText(this, "Select a session first.", Toast.LENGTH_SHORT).show()
        } else {
            ArReplayNavigationActivity.start(this, sessionPath)
        }
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

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // PERMISSION_FOREGROUND_SERVICE was added for targetSdkVersion >= 28, it is normal and always granted, but should be added to the Manifest file
                // on devices with Android < P(9) checkSelfPermission(PERMISSION_FOREGROUND_SERVICE) can return PERMISSION_DENIED, but in fact it is GRANTED, so skip it
                // https://developer.android.com/guide/components/services#Foreground
                if (permission == PERMISSION_FOREGROUND_SERVICE) {
                    continue
                }
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
