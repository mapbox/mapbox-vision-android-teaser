package com.mapbox.vision.recorder

import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mapbox.vision.VisionManager

class RecorderApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)?.let {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
        VisionManager.init(this, getString(R.string.mapbox_access_token))
    }
}
