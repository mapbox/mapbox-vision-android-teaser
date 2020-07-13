package com.mapbox.vision.examples.activity.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.mapbox.vision.VisionManager
import com.mapbox.vision.common.view.BaseTeaserActivity
import com.mapbox.vision.common.view.show
import com.mapbox.vision.examples.ConnectService
import com.mapbox.vision.examples.DemoApplication.Companion.CAMERA_IP
import com.mapbox.vision.examples.R
import com.mapbox.vision.examples.activity.ar.ArMapActivity
import com.mapbox.vision.mobile.core.models.CameraParameters
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.view.VisionView
import com.mapbox.vision.vlc.externalcamera.ExternalVideoSourceImpl

class MainActivity : BaseTeaserActivity() {

    companion object{
       val TAG = "MainActivity"
    }
    override fun initViews(root: View) {
        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener { startActivity(Intent(this@MainActivity, ArMapActivity::class.java)) }
        }

        root.findViewById<ImageView>(R.id.title_teaser).apply {
            show()
        }
    }

    private var connect: Boolean = false
    val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.e(TAG, "onServiceDisconnected")
            connect = true
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.e(TAG, "onServiceConnected")

            (service as ConnectService.ServiceBinder).register(object : ConnectService.MessageReceiver {
                override fun onReceive(x: Double, y: Double, z: Double) {
                    Log.e(TAG, "receive x:$x, y:$y, z: $z")
                    if(!connect) return
                    VisionManager.setDeviceMotion(
                            accelerationX = x.toFloat(), // acceleration to the front, m/s2
                            accelerationY = y.toFloat(), // acceleration to the left, m/s2
                            accelerationZ = z.toFloat(), // acceleration to the top, m/s2
                            orientationX = 0f,  // angle along X axis, radians, zero if parallel to X, negative rotated clockwise
                            orientationY = -Math.PI.toFloat() / 2,  // angle along Y axis, radians, -PI/2 if parallel to Z, 0 if camera points to bottom
                            orientationZ = 0f,  // angle along Z axis, radians
                            gyroscopeX = 0f,    // acceleration along X axis, rad/s
                            gyroscopeY = 0f,    // acceleration along Y axis, rad/s
                            gyroscopeZ = 0f)     // acceleration along Z axis, rad/s)
                }
            })
        }

    }
    override fun onResume() {
        super.onResume()
//        val bindService = applicationContext.bindService(Intent(this, ConnectService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
//        if(connect) {
//            unbindService(connection)
//        }
    }
    override fun getFrameStatistics() = VisionManager.getFrameStatistics()

    override fun initVisionManager(visionView: VisionView): Boolean {
        val externalVideoSource = ExternalVideoSourceImpl(
                application = VisionManager.application,
                externalCameraIp = "rtsp://${CAMERA_IP}/media/stream2", //for AP mode
//                externalCameraIp = "rtsp://${CAMERA_IP}/media/stream2", //for STA mode
//                externalCameraIp = "/sdcard/Drive4K_East_3rd_Ring_Road_Beijing.mp4", //local mode
                externalCameraParameters = CameraParameters(
                        width = 1280,
                        height = 720,
                        focalInPixelsX = 1280f * 6.0f / 5.07f,
                        focalInPixelsY = 720f * 6.0f / 3.38f
                )
        )
        VisionManager.create(externalVideoSource, false)
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
