package com.mapbox.vision.replayer

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.common.view.BaseTeaserActivity
import com.mapbox.vision.common.view.show
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.view.VisionView
import java.io.File

class ReplayActivity : BaseTeaserActivity(), SessionsFragment.SessionChangeListener {

    override fun initViews(root: View) {
        root.findViewById<ImageView>(R.id.object_mapping).apply {
            setImageDrawable(
                ContextCompat.getDrawable(this@ReplayActivity, R.drawable.ic_section_routing)
            )
        }

        root.findViewById<ImageView>(R.id.ar_navigation).apply {
            setImageDrawable(
                ContextCompat.getDrawable(this@ReplayActivity, R.drawable.ic_select_session)
            )
        }

        root.findViewById<TextView>(R.id.object_mapping_text).apply {
            setText(R.string.start_ar_session)
        }

        root.findViewById<TextView>(R.id.ar_navigation_text).apply {
            setText(R.string.choose_session)
        }

        root.findViewById<LinearLayout>(R.id.object_mapping_button_container).apply {
            setOnClickListener { startArSession() }
        }

        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener { showSessionsList() }
        }

        root.findViewById<TextView>(R.id.title_replayer).apply {
            setText(R.string.app_title)
            show()
        }
    }

    override fun getFrameStatistics() = VisionReplayManager.getFrameStatistics()

    override fun initVisionManager(visionView: VisionView): Boolean {
        if (sessionPath.isEmpty() || !File(sessionPath).exists() || File(sessionPath).list().isEmpty()) {
            showSessionsList()
            return false
        }

        VisionReplayManager.create(sessionPath)
        VisionReplayManager.visionEventsListener = visionEventsListener
        VisionReplayManager.start()
        VisionReplayManager.setModelPerformanceConfig(appModelPerformanceConfig)
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

    private var sessionPath: String = "$BASE_SESSION_PATH/default/"
        set(value) {
            field = "$BASE_SESSION_PATH/$value/"
        }

    private fun startArSession() {
        if (sessionPath.isEmpty()) {
            Toast.makeText(this, "Select a session first.", Toast.LENGTH_SHORT).show()
        } else {
            ArReplayNavigationActivity.start(this, sessionPath)
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
