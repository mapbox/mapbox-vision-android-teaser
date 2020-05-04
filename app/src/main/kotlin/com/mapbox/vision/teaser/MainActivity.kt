package com.mapbox.vision.teaser

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.mapbox.vision.VisionManager
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.mobile.core.models.FrameStatistics
import com.mapbox.vision.teaser.view.BaseTeaserActivity
import com.mapbox.vision.teaser.view.show
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.teaser.MainActivity.VisionManagerMode.*
import com.mapbox.vision.teaser.ar.ArMapActivity
import com.mapbox.vision.teaser.replayer.ArReplayNavigationActivity
import com.mapbox.vision.teaser.replayer.ReplayModeFragment
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.view.VisionView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseTeaserActivity(), ReplayModeFragment.OnClickModeItemListener {

    enum class VisionManagerMode {
        Realtime,
        Replay,
        Recorder
    }

    private var mode = Realtime
    private var sessionPath = ""

    override fun initViews(root: View) {
        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener {
                when (mode) {
                    Realtime -> startActivity(Intent(this@MainActivity, ArMapActivity::class.java))
                    Replay -> startArSession()
                    Recorder -> return@setOnClickListener
                }
            }
        }

        root.findViewById<LinearLayout>(R.id.replay_mode_button_container).apply {
            setOnClickListener { showReplayModeFragment() }
        }
    }

    override fun initVisionManager(visionView: VisionView): Boolean {
        return when (mode) {
            Realtime -> initVisionManagerRealtime(visionView)
            Replay -> initVisionManagerReplay(visionView, sessionPath)
            Recorder -> false
        }
    }

    private fun initVisionManagerRealtime(visionView: VisionView): Boolean {
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
            Realtime -> destroyVisionManagerRealtime()
            Replay -> destroyVisionManagerReplay()
            Recorder -> return
        }
    }

    override fun getFrameStatistics(): FrameStatistics? {
        return when (mode) {
            Realtime -> VisionManager.getFrameStatistics()
            Replay -> VisionReplayManager.getFrameStatistics()
            Recorder -> null
        }
    }

    private fun destroyVisionManagerRealtime() {
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
        if (fragment is OnBackPressedListener) {
            if (fragment.onBackPressed().not()) {
                supportFragmentManager.popBackStack()
                showDashboardView()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onClickSessionItem(sessionName: String) {
        stopVisionManager()
        mode = Replay
        sessionPath = "$BASE_SESSION_PATH/$sessionName"
        title_teaser.setText(R.string.app_title_replayer)
        tryToInitVisionManager()
    }

    override fun onClickCamera() {
        stopVisionManager()
        mode = Realtime
        sessionPath = ""
        title_teaser.setText(R.string.app_title_teaser)
        tryToInitVisionManager()
    }

    private fun startArSession() {
        if (sessionPath.isEmpty()) {
            Toast.makeText(this, "Select a session first.", Toast.LENGTH_SHORT).show()
        } else {
            ArReplayNavigationActivity.start(this, sessionPath)
        }
    }
}
