package com.mapbox.vision.teaser

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import com.mapbox.vision.VisionManager
import com.mapbox.vision.teaser.view.BaseTeaserActivity
import com.mapbox.vision.teaser.view.show
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.teaser.ar.ArMapActivity
import com.mapbox.vision.teaser.replayer.ReplayModeFragment
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.view.VisionView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseTeaserActivity() {

    override fun initViews(root: View) {
        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener { startActivity(Intent(this@MainActivity, ArMapActivity::class.java)) }
        }

        root.findViewById<LinearLayout>(R.id.replay_mode_button_container).apply {
            setOnClickListener { showReplayModeFragment() }
        }
    }

    override fun getFrameStatistics() = VisionManager.getFrameStatistics()

    override fun initVisionManager(visionView: VisionView): Boolean {
        VisionManager.create()
        visionView.setVisionManager(VisionManager)
        VisionManager.visionEventsListener = visionEventsListener
        VisionManager.start()
        VisionManager.setModelPerformance(modelPerformance)

        VisionSafetyManager.create(VisionManager)
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
        VisionSafetyManager.destroy()
        VisionManager.stop()
        VisionManager.destroy()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is OnBackPressedListener && fragment.onBackPressed()) {
            supportFragmentManager.popBackStack()
            showDashboardView()
        } else {
            super.onBackPressed()
        }
    }
}
