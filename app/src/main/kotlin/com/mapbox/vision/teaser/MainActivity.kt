package com.mapbox.vision.teaser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.mapbox.vision.VisionManager
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications
import com.mapbox.vision.mobile.core.models.position.VehicleState
import com.mapbox.vision.mobile.core.models.road.LaneDirection
import com.mapbox.vision.mobile.core.models.road.LaneEdgeType
import com.mapbox.vision.mobile.core.models.road.RoadDescription
import com.mapbox.vision.mobile.core.utils.SystemInfoUtils
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.teaser.MainActivity.VisionMode.Camera
import com.mapbox.vision.teaser.MainActivity.VisionMode.Replay
import com.mapbox.vision.teaser.ar.ArMapActivity
import com.mapbox.vision.teaser.ar.ArNavigationActivity
import com.mapbox.vision.teaser.models.UiSign
import com.mapbox.vision.teaser.recorder.RecorderFragment
import com.mapbox.vision.teaser.replayer.ArReplayNavigationActivity
import com.mapbox.vision.teaser.replayer.ReplayModeFragment
import com.mapbox.vision.teaser.utils.PermissionsUtils
import com.mapbox.vision.teaser.utils.classification.SignResources
import com.mapbox.vision.teaser.utils.classification.Tracker
import com.mapbox.vision.teaser.utils.dpToPx
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.teaser.view.show
import com.mapbox.vision.teaser.view.toggleVisibleGone
import com.mapbox.vision.utils.VisionLogger
import com.mapbox.vision.view.VisionView
import com.mapbox.vision.view.VisualizationMode
import java.io.File
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ReplayModeFragment.OnSelectModeItemListener {

    enum class VisionMode {
        Camera,
        Replay
    }

    enum class VisionFeature {
        Segmentation,
        Classification,
        Detection,
        Lanes
    }

    companion object {
        private val BASE_SESSION_PATH = "${Environment.getExternalStorageDirectory().absolutePath}/MapboxVisionTelemetry"
        private const val TRACKER_DEFAULT_COUNT = 5
        private const val START_AR_MAP_ACTIVITY_FOR_NAVIGATION_RESULT_CODE = 100
        private const val START_AR_MAP_ACTIVITY_FOR_RECORDING_RESULT_CODE = 110
    }

    var visionMode = Camera
    private var sessionPath = ""

    val signResources: SignResources = SignResources.Impl(this)

    private var signSize = 0
    private var margin = 0

    private var lineHeight = 0
    private var tracker: Tracker<UiSign> = Tracker(TRACKER_DEFAULT_COUNT)
    private var visionFeature: VisionFeature = VisionFeature.Detection
    private var isPermissionsGranted = false
    private var visionManagerWasInit = false
    private var country = Country.Unknown
    private var modelPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)

    private val visionEventsListener = object : VisionEventsListener {

        override fun onCountryUpdated(country: Country) {
            this@MainActivity.country = country
            requireSafetyFragment()?.country = country
        }

        override fun onFrameSignClassificationsUpdated(frameSignClassifications: FrameSignClassifications) {
            if (visionFeature == VisionFeature.Classification) {
                runOnUiThread {
                    tracker.update(UiSign.getUiSigns(frameSignClassifications))
                    drawSigns(tracker.getCurrent())
                }
            }
        }

        override fun onRoadDescriptionUpdated(roadDescription: RoadDescription) {
            if (visionFeature == VisionFeature.Lanes) {
                runOnUiThread {
                    drawLanesDetection(roadDescription)
                }
            }
        }

        override fun onVehicleStateUpdated(vehicleState: VehicleState) {
            requireSafetyFragment()?.lastSpeed = vehicleState.speed
        }

        override fun onCameraUpdated(camera: com.mapbox.vision.mobile.core.models.Camera) {
            requireSafetyFragment()?.calibrationProgress = camera.calibrationProgress
            runOnUiThread {
                fps_performance_view.setCalibrationProgress(camera.calibrationProgress)
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onUpdateCompleted() {
            runOnUiThread {
                if (visionManagerWasInit) {
                    val frameStatistics = when (visionMode) {
                        Camera -> VisionManager.getFrameStatistics()
                        Replay -> VisionReplayManager.getFrameStatistics()
                    }
                    fps_performance_view.showInfo(frameStatistics)
                    if (visionMode == Replay) {
                        playback_seek_bar_view.setProgress(VisionReplayManager.getProgress())
                    }
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

        if (!PermissionsUtils.requestPermissionsIfNotGranted(this)) {
            onPermissionsGranted()
        }
    }

    private fun createSessionFolderIfNotExist() {
        val folder = File(BASE_SESSION_PATH)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                throw IllegalStateException("Can't create image folder = $folder")
            }
        }
    }

    private fun onPermissionsGranted() {
        isPermissionsGranted = true

        createSessionFolderIfNotExist()

        signSize = dpToPx(64f).toInt()
        lineHeight = dpToPx(40f).toInt()
        margin = dpToPx(8f).toInt()
        back.setOnClickListener { onBackClick() }
        segm_container.setOnClickListener { setVisionMode(VisionFeature.Segmentation) }
        sign_detection_container.setOnClickListener { setVisionMode(VisionFeature.Classification) }
        det_container.setOnClickListener { setVisionMode(VisionFeature.Detection) }
        distance_container.setOnClickListener {
            vision_view.visualizationMode = VisualizationMode.Clear
            showSafetyFragment()
        }
        line_detection_container.setOnClickListener { setVisionMode(VisionFeature.Lanes) }

        initRootLongTap()
        initRootTap()
        fps_performance_view.hide()

        initArNavigationButton()
        initReplayModeButton()
        tryToInitVisionManager()
    }

    private fun initRootLongTap() {
        root.setOnLongClickListener {
            fps_performance_view.toggleVisibleGone()
            return@setOnLongClickListener true
        }
    }

    private fun initRootTap() {
        root.setOnClickListener {
            if (visionMode == Replay) {
                playback_seek_bar_view.toggleVisibleGone()
            }
        }
    }

    private fun initArNavigationButton() {
        ar_navigation_button_container.setOnClickListener {
            when (visionMode) {
                Camera -> startArMapActivityForNavigation()
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
            visionManagerWasInit = when (visionMode) {
                Camera -> initVisionManagerCamera(vision_view)
                Replay -> initVisionManagerReplay(vision_view, sessionPath)
            }
        }
    }

    private fun stopVisionManager() {
        if (isPermissionsGranted && visionManagerWasInit) {
            when (visionMode) {
                Camera -> destroyVisionManagerCamera()
                Replay -> {
                    destroyVisionManagerReplay()
                    playback_seek_bar_view.onSeekBarChangeListener = null
                }
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
    }

    private fun onBackClick() {
        dashboard_container.show()
        hideLineDetectionContainer()
        hideSignsContainer()
        back.hide()
        playback_seek_bar_view.hide()
    }

    private fun drawSigns(uiSigns: List<UiSign>) {
        sign_info_container.removeAllViews()
        for (uiSign in uiSigns) {
            sign_info_container.addView(
                    ImageView(this).apply {
                        layoutParams =
                                ViewGroup.MarginLayoutParams(signSize, ViewGroup.LayoutParams.WRAP_CONTENT)
                                        .apply {
                                            leftMargin = margin
                                        }
                        setImageResource(signResources.getSignResource(uiSign, country))
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

    private fun setVisionMode(mode: VisionFeature) {
        fps_performance_view.resetAverageFps()

        back.show()
        dashboard_container.hide()
        hideLineDetectionContainer()
        hideSignsContainer()
        playback_seek_bar_view.hide()

        visionFeature = mode
        when (visionFeature) {
            VisionFeature.Classification -> {
                vision_view.visualizationMode = VisualizationMode.Clear
                tracker = Tracker(TRACKER_DEFAULT_COUNT)
                sign_info_container.show()
            }
            VisionFeature.Detection -> {
                vision_view.visualizationMode = VisualizationMode.Detection
            }
            VisionFeature.Segmentation -> {
                vision_view.visualizationMode = VisualizationMode.Segmentation
            }
            VisionFeature.Lanes -> {
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
        return true
    }

    private fun destroyVisionManagerCamera() {
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

        playback_seek_bar_view.setDuration(VisionReplayManager.getDuration())
        playback_seek_bar_view.onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    VisionReplayManager.setProgress(progress.toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        }

        return true
    }

    private fun destroyVisionManagerReplay() {
        VisionReplayManager.stop()
        VisionReplayManager.destroy()
    }

    private fun showReplayModeFragment(stateLoss: Boolean = false) {
        val fragment = ReplayModeFragment.newInstance(BASE_SESSION_PATH)
        showFragment(fragment, ReplayModeFragment.TAG, stateLoss)
    }

    private fun showRecorderFragment(jsonRoute: String?, stateLoss: Boolean = false) {
        val fragment = RecorderFragment.newInstance(BASE_SESSION_PATH, jsonRoute)
        showFragment(fragment, RecorderFragment.TAG, stateLoss)
    }

    private fun showSafetyFragment(stateLoss: Boolean = false) {
        val fragment = SafetyFragment.newInstance()
        showFragment(fragment, SafetyFragment.TAG, stateLoss)
    }

    private fun showFragment(fragment: Fragment, tag: String, stateLoss: Boolean = false) {
        val fragmentTransaction = supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
        if (stateLoss) {
            fragmentTransaction.commitAllowingStateLoss()
        } else {
            fragmentTransaction.commit()
        }
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
            if (!(fragment is OnBackPressedListener && fragment.onBackPressed())) {
                if (supportFragmentManager.popBackStackImmediate() && supportFragmentManager.backStackEntryCount == 0) {
                    showDashboardView()
                    title_teaser.show()
                    playback_seek_bar_view.hide()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSessionSelected(sessionName: String) {
        stopVisionManager()
        visionMode = Replay
        sessionPath = "$BASE_SESSION_PATH/$sessionName"
        tryToInitVisionManager()
    }

    override fun onCameraSelected() {
        stopVisionManager()
        visionMode = Camera
        sessionPath = ""
        tryToInitVisionManager()
    }

    override fun onRecordingSelected() {
        startArMapActivityForRecording()
    }

    private fun startArMapActivityForNavigation() {
        startArMapActivity(START_AR_MAP_ACTIVITY_FOR_NAVIGATION_RESULT_CODE)
    }

    private fun startArMapActivityForRecording() {
        startArMapActivity(START_AR_MAP_ACTIVITY_FOR_RECORDING_RESULT_CODE)
    }

    private fun startArMapActivity(resultCode: Int) {
        val intent = Intent(this@MainActivity, ArMapActivity::class.java)
        startActivityForResult(intent, resultCode)
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
        if (PermissionsUtils.allPermissionsGrantedByRequest(this, requestCode)) {
            onPermissionsGranted()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            START_AR_MAP_ACTIVITY_FOR_NAVIGATION_RESULT_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val jsonRoute = data.getStringExtra(ArMapActivity.ARG_RESULT_JSON_ROUTE)
                    if (!jsonRoute.isNullOrEmpty()) {
                        ArNavigationActivity.start(this, jsonRoute)
                    }
                }
            }
            START_AR_MAP_ACTIVITY_FOR_RECORDING_RESULT_CODE -> {
                val jsonRoute = data?.getStringExtra(ArMapActivity.ARG_RESULT_JSON_ROUTE)
                onCameraSelected()
                vision_view.visualizationMode = VisualizationMode.Clear

                // Using state loss here to keep code simple, lost of RecorderFragment is not critical for UX
                showRecorderFragment(jsonRoute, stateLoss = true)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun requireSafetyFragment() = supportFragmentManager.findFragmentById(R.id.fragment_container) as? SafetyFragment

    fun isCameraMode() = visionMode == Camera
}
