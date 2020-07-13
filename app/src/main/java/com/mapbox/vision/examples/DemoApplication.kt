package com.mapbox.vision.examples

import androidx.multidex.MultiDexApplication
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.vision.VisionManager

class DemoApplication : MultiDexApplication() {
    companion object {
//        public var CAMERA_IP = "192.168.99.1"
        public var CAMERA_IP = "192.168.43.70"
    }
    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        VisionManager.init(this, getString(R.string.mapbox_access_token))
    }
}
