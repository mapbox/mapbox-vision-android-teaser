package com.mapbox.vision.examples.activity.main

import android.content.Intent
import com.mapbox.vision.VisionManager
import com.mapbox.vision.common.view.BaseTeaserActivity
import com.mapbox.vision.examples.activity.ar.ArMapActivity
import com.mapbox.vision.examples.activity.map.MapActivity
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.view.VisionView

class MainActivity : BaseTeaserActivity() {

    override val view1Click = { startActivity(Intent(this, MapActivity::class.java)) }

    override val view2Click = { startActivity(Intent(this, ArMapActivity::class.java)) }

    override fun getAppType() = AppType.Teaser

    override fun getFrameStatistics() = VisionManager.getFrameStatistics()

    override fun initVisionManager(visionView: VisionView): Boolean {
        VisionManager.create()
        visionView.setVisionManager(VisionManager)
        VisionManager.visionEventsListener = visionEventsListener
        VisionManager.start()
        VisionManager.setModelPerformanceConfig(appModelPerformanceConfig)

        VisionSafetyManager.create(VisionManager)
        VisionSafetyManager.visionSafetyListener = visionSafetyListener

        return true
    }

    override fun destroyVisionManager() {
        VisionSafetyManager.destroy()
        VisionManager.stop()
        VisionManager.destroy()
    }
}
