package com.mapbox.vision.teaser

import androidx.multidex.MultiDexApplication
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.vision.VisionManager

class TeaserApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        VisionManager.init(this, getString(R.string.mapbox_access_token))
    }
}
