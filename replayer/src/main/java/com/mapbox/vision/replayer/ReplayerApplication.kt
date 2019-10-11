package com.mapbox.vision.replayer

import androidx.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import com.mapbox.vision.VisionManager
import io.fabric.sdk.android.Fabric

class ReplayerApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        VisionManager.init(this, getString(R.string.mapbox_access_token))
    }
}
