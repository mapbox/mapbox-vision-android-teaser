package com.mapbox.vision.replayer

import android.widget.Toast
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.common.view.BaseTeaserActivity
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.view.VisionView

class ReplayActivity : BaseTeaserActivity(), SessionsFragment.ISessionChangeListener {

    override val view1Click = { startArSession() }

    override val view2Click = { showSessionsList() }

    override fun getAppType() = AppType.Replayer

    override fun getFrameStatistics() = VisionReplayManager.getFrameStatistics()

    override fun initVisionManager(visionView: VisionView): Boolean {
        if (sessionPath.isNullOrEmpty()) {
            return false
        }

        VisionReplayManager.create(sessionPath!!)
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
