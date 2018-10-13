package com.mapbox.vision.examples

import android.support.multidex.MultiDexApplication
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.vision.VisionManager

class DemoApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        VisionManager.init(this, getString(R.string.mapbox_access_token))
    }
}
