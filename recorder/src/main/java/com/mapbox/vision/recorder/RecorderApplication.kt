package com.mapbox.vision.recorder

import androidx.multidex.MultiDexApplication
import com.mapbox.vision.VisionManager

class RecorderApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        VisionManager.init(this, getString(R.string.mapbox_access_token))
    }
}
