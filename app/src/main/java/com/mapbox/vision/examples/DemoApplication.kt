package com.mapbox.vision.examples

import android.support.multidex.MultiDexApplication
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.vision.VisionManager
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric



class DemoApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        VisionManager.init(this, getString(R.string.mapbox_access_token))
    }
}
