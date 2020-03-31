package com.mapbox.vision.examples.activity.main

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.mapbox.vision.VisionManager
import com.mapbox.vision.common.view.BaseTeaserActivity
import com.mapbox.vision.common.view.show
import com.mapbox.vision.examples.R
import com.mapbox.vision.examples.activity.ar.ArMapActivity
import com.mapbox.vision.mobile.core.models.CameraParameters
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.view.VisionView
import com.mapbox.vision.vlc.externalcamera.ExternalVideoSourceImpl

class MainActivity : BaseTeaserActivity() {

    override fun initViews(root: View) {
        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener { startActivity(Intent(this@MainActivity, ArMapActivity::class.java)) }
        }

        root.findViewById<ImageView>(R.id.title_teaser).apply {
            show()
        }
    }

    override fun getFrameStatistics() = VisionManager.getFrameStatistics()

    override fun initVisionManager(visionView: VisionView): Boolean {
        val externalVideoSource = ExternalVideoSourceImpl(
                application = VisionManager.application,
                externalCameraIp = "rtsp://192.168.99.1/media/stream2", //for AP mode
//                externalCameraIp = "rtsp://192.168.43.2/media/stream2", //for STA mode
//                externalCameraIp = "/sdcard/Drive4K_East_3rd_Ring_Road_Beijing.mp4", //local mode
                externalCameraParameters = CameraParameters(
                        width = 1280,
                        height = 720,
                        focalInPixelsX = 1280f * 6.0f / 5.07f,
                        focalInPixelsY = 720f * 6.0f / 3.38f
                )
        )
        VisionManager.create(externalVideoSource)
//        VisionManager.create()
        visionView.setVisionManager(VisionManager)
        VisionManager.visionEventsListener = visionEventsListener
        VisionManager.start()
        VisionManager.setModelPerformance(modelPerformance)

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
