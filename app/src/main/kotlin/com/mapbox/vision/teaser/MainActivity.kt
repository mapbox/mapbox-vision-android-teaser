package com.mapbox.vision.teaser

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.mapbox.vision.VisionManager
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.mobile.core.models.FrameStatistics
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.teaser.MainActivity.VisionManagerMode.Camera
import com.mapbox.vision.teaser.MainActivity.VisionManagerMode.Replay
import com.mapbox.vision.teaser.ar.ArMapActivity
import com.mapbox.vision.teaser.recorder.RecorderFragment
import com.mapbox.vision.teaser.replayer.ArReplayNavigationActivity
import com.mapbox.vision.teaser.replayer.ReplayModeFragment
import com.mapbox.vision.teaser.view.BaseTeaserActivity
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.teaser.view.show
import com.mapbox.vision.view.VisionView
import com.mapbox.vision.view.VisualizationMode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseTeaserActivity(), ReplayModeFragment.OnSelectModeItemListener {

    enum class VisionManagerMode {
        Camera,
        Replay,
    }

    private var mode = Camera
    private var sessionPath = ""

    override fun initViews(root: View) {
        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener {
                when (mode) {
                    Camera -> startActivity(Intent(this@MainActivity, ArMapActivity::class.java))
                    Replay -> startArSession()
                }
            }
        }

        root.findViewById<LinearLayout>(R.id.replay_mode_button_container).apply {
            setOnClickListener { showReplayModeFragment() }
        }
    }

    override fun initVisionManager(visionView: VisionView): Boolean {
        return when (mode) {
            Camera -> initVisionManagerCamera(visionView)
            Replay -> initVisionManagerReplay(visionView, sessionPath)
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

    override fun destroyVisionManager() {
        return when (mode) {
            Camera -> destroyVisionManagerCamera()
            Replay -> destroyVisionManagerReplay()
        }
    }

    override fun getFrameStatistics(): FrameStatistics? {
        return when (mode) {
            Camera -> VisionManager.getFrameStatistics()
            Replay -> VisionReplayManager.getFrameStatistics()
        }
    }

    private fun destroyVisionManagerCamera() {
        VisionSafetyManager.destroy()
        VisionManager.stop()
        VisionManager.destroy()
    }

    private fun destroyVisionManagerReplay() {
        VisionSafetyManager.destroy()
        VisionReplayManager.stop()
        VisionReplayManager.destroy()
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
        mode = Replay
        sessionPath = "$BASE_SESSION_PATH/$sessionName"
        tryToInitVisionManager()
    }

    override fun onCameraSelected() {
        stopVisionManager()
        mode = Camera
        sessionPath = ""
        tryToInitVisionManager()
    }

    override fun onRecordingSelected() {
        stopVisionManager()
        mode = Camera
        tryToInitVisionManager()
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
}
