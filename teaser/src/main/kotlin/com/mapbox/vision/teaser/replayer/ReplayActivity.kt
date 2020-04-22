package com.mapbox.vision.teaser.replayer

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.common.view.BaseTeaserActivity
import com.mapbox.vision.common.view.show
import com.mapbox.vision.teaser.replayer.SessionsFragment
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.teaser.R
import com.mapbox.vision.view.VisionView

class ReplayActivity : BaseTeaserActivity(), SessionsFragment.SessionChangeListener {

    override fun initViews(root: View) {
        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener { startArSession() }
        }

        root.findViewById<LinearLayout>(R.id.session_chooser_button_container).apply {
            setOnClickListener { showSessionsList() }
        }

        root.findViewById<TextView>(R.id.title_replayer).apply {
            setText(R.string.app_title_replayer)
            show()
        }
    }

    override fun setLayout() {
        setContentView(R.layout.activity_replay)
    }

    override fun getFrameStatistics() = VisionReplayManager.getFrameStatistics()

    override fun initVisionManager(visionView: VisionView): Boolean {
        if (sessionPath.isNullOrEmpty()) {
            return false
        }

        VisionReplayManager.create(sessionPath!!)
        VisionReplayManager.visionEventsListener = visionEventsListener
        VisionReplayManager.start()
        VisionReplayManager.setModelPerformance(modelPerformance)
        visionView.setVisionManager(VisionReplayManager)

        VisionSafetyManager.create(VisionReplayManager)
        VisionSafetyManager.visionSafetyListener = visionSafetyListener

        return true
    }

    override fun destroyVisionManager() {
        VisionSafetyManager.destroy()
        VisionReplayManager.stop()
        VisionReplayManager.destroy()
    }

    private var sessionPath: String? = null
        set(value) {
            field = "$BASE_SESSION_PATH/$value/"
        }

    private fun startArSession() {
        if (sessionPath.isNullOrEmpty()) {
            Toast.makeText(this, "Select a session first.", Toast.LENGTH_SHORT).show()
        } else {
            ArReplayNavigationActivity.start(this, sessionPath!!)
        }
    }

    private fun showSessionsList() {
        SessionsFragment().show(supportFragmentManager, "sessions_fragment")
    }

    override fun onSessionChanged(dirName: String) {
        sessionPath = dirName
        restartVisionManager()
    }
}
