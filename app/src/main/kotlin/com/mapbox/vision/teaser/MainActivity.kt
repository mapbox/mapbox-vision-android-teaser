package com.mapbox.vision.teaser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.view.WindowManager
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
import com.mapbox.vision.mobile.core.models.road.RoadDescription
import com.mapbox.vision.mobile.core.utils.SystemInfoUtils
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.teaser.MainActivity.VisionMode.Camera
import com.mapbox.vision.teaser.MainActivity.VisionMode.Replay
import com.mapbox.vision.teaser.ar.ArMapActivity
import com.mapbox.vision.teaser.ar.ArNavigationActivity
import com.mapbox.vision.teaser.recorder.RecorderFragment
import com.mapbox.vision.teaser.replayer.ArReplayNavigationActivity
import com.mapbox.vision.teaser.replayer.ReplayModeFragment
import com.mapbox.vision.teaser.utils.PermissionsUtils
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

    companion object {
        private val BASE_SESSION_PATH = "${Environment.getExternalStorageDirectory().absolutePath}/MapboxVisionTelemetry"
        private const val START_AR_MAP_ACTIVITY_FOR_NAVIGATION_RESULT_CODE = 100
        private const val START_AR_MAP_ACTIVITY_FOR_RECORDING_RESULT_CODE = 110
    }

    private var country = Country.Unknown
    private var visionMode = Camera
    private var sessionPath = ""

    private var isPermissionsGranted = false
    private var visionManagerWasInit = false
    private var modelPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)

    private val visionEventsListener = object : VisionEventsListener {

        override fun onCountryUpdated(country: Country) {
            runOnUiThread {
                this@MainActivity.country = country
                requireBaseVisionFragment()?.updateCountry(country)
            }
        }

        override fun onFrameSignClassificationsUpdated(frameSignClassifications: FrameSignClassifications) {
            runOnUiThread {
                requireSignDetectionFragment()?.drawSigns(frameSignClassifications)
            }
        }

        override fun onRoadDescriptionUpdated(roadDescription: RoadDescription) {
            runOnUiThread {
                requireLaneDetectionFragment()?.drawLanesDetection(roadDescription)
            }
        }

        override fun onVehicleStateUpdated(vehicleState: VehicleState) {
            runOnUiThread {
                requireBaseVisionFragment()?.updateLastSpeed(vehicleState.speed)
            }
        }

        override fun onCameraUpdated(camera: com.mapbox.vision.mobile.core.models.Camera) {
            runOnUiThread {
                requireBaseVisionFragment()?.updateCalibrationProgress(camera.calibrationProgress)
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

        if (!PermissionsUtils.requestPermissions(this)) {
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
        back.setOnClickListener { onBackClick() }
        segm_container.setOnClickListener { showSegmentationFragment() }
        sign_detection_container.setOnClickListener { showSignDetectionFragment() }
        det_container.setOnClickListener { showObjectDetectionFragment() }
        distance_container.setOnClickListener { showSafetyFragment() }
        line_detection_container.setOnClickListener { showLaneFragment() }

        initRootLongTap()
        initRootTap()
        fps_performance_view.hide()

        initArNavigationButton()
        initReplayModeButton()
        startVision()
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

    private fun startVision() {
        if (isPermissionsGranted && !visionManagerWasInit) {
            visionManagerWasInit = when (visionMode) {
                Camera -> initVisionManagerCamera(vision_view)
                Replay -> initVisionManagerReplay(vision_view, sessionPath)
            }
        }
    }

    private fun stopVision() {
        if (isPermissionsGranted && visionManagerWasInit) {
            visionManagerWasInit = false
            when (visionMode) {
                Camera -> destroyVisionManagerCamera()
                Replay -> destroyVisionManagerReplay()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startVision()
        vision_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        stopVision()
        vision_view.onPause()
    }

    private fun onBackClick() {
        dashboard_container.show()
        back.hide()
        playback_seek_bar_view.hide()
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
        playback_seek_bar_view.onSeekBarChangeListener = null
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
        vision_view.visualizationMode = VisualizationMode.Clear
        val fragment = SafetyFragment.newInstance()
        showFragment(fragment, SafetyFragment.TAG, stateLoss)
    }

    private fun showSignDetectionFragment(stateLoss: Boolean = false) {
        vision_view.visualizationMode = VisualizationMode.Clear
        val fragment = SignDetectionFragment.newInstance(country)
        showFragment(fragment, SignDetectionFragment.TAG, stateLoss)
    }

    private fun showLaneFragment(stateLoss: Boolean = false) {
        vision_view.visualizationMode = VisualizationMode.Clear
        val fragment = LaneFragment.newInstance()
        showFragment(fragment, LaneFragment.TAG, stateLoss)
    }

    private fun showSegmentationFragment(stateLoss: Boolean = false) {
        vision_view.visualizationMode = VisualizationMode.Segmentation
        showSegmentationDetectionFragment(stateLoss)
    }

    private fun showObjectDetectionFragment(stateLoss: Boolean = false) {
        vision_view.visualizationMode = VisualizationMode.Detection
        showSegmentationDetectionFragment(stateLoss)
    }

    private fun showSegmentationDetectionFragment(stateLoss: Boolean = false) {
        val fragment = SegmentationDetectionFragment.newInstance()
        showFragment(fragment, SegmentationDetectionFragment.TAG, stateLoss)
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
        playback_seek_bar_view.hide()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            if (!(fragment is OnBackPressedListener && fragment.onBackPressed())) {

                if (fragment is RecorderFragment) {
                    when (visionMode) {
                        Camera -> VisionManager.setModelPerformance(modelPerformance)
                        Replay -> VisionReplayManager.setModelPerformance(modelPerformance)
                    }
                }

                if (supportFragmentManager.popBackStackImmediate() && supportFragmentManager.backStackEntryCount == 0) {
                    showDashboardView()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSessionSelected(sessionName: String) {
        stopVision()
        visionMode = Replay
        sessionPath = "$BASE_SESSION_PATH/$sessionName"
        startVision()
    }

    override fun onCameraSelected() {
        stopVision()
        visionMode = Camera
        sessionPath = ""
        startVision()
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
        if (PermissionsUtils.arePermissionsGranted(this, requestCode)) {
            onPermissionsGranted()
        } else {
            val notGranted = PermissionsUtils.getNotGrantedPermissions(this).joinToString(", ")

            AlertDialog.Builder(this)
                .setTitle(R.string.permissions_missing_title)
                .setMessage(
                    getString(R.string.permissions_missing_message, notGranted)
                )
                .setCancelable(false)
                .setPositiveButton(
                    R.string.request_permissions
                ) { _, _ -> PermissionsUtils.requestPermissions(this) }
                .show()

            VisionLogger.e(
                MainActivity::class.java.simpleName,
                "Permissions are not granted : $notGranted"
            )
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

                // set lowest model performance to allow fair 30 fps all the time
                VisionManager.setModelPerformance(ModelPerformance.Off)

                // Using state loss here to keep code simple, lost of RecorderFragment is not critical for UX
                showRecorderFragment(jsonRoute, stateLoss = true)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun requireSignDetectionFragment() = requireBaseVisionFragment() as? SignDetectionFragment

    private fun requireLaneDetectionFragment() = requireBaseVisionFragment() as? LaneFragment

    private fun requireBaseVisionFragment() = supportFragmentManager.findFragmentById(R.id.fragment_container) as? BaseVisionFragment

    fun isCameraMode() = visionMode == Camera
}
