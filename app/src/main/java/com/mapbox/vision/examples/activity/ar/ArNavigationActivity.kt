package com.mapbox.vision.examples.activity.ar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.vision.VisionManager
import com.mapbox.vision.examples.R
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import kotlinx.android.synthetic.main.activity_ar_navigation.*

class ArNavigationActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ROUTE = "Route"

        fun start(context: Activity, directionsRoute: DirectionsRoute) {
            context.startActivity(
                    Intent(context, ArNavigationActivity::class.java)
                            .putExtra(EXTRA_ROUTE, directionsRoute)
            )
        }
    }

    private val mapboxNavigation by lazy { MapboxNavigation(this, Mapbox.getAccessToken()!!) }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_navigation)

        VisionManager.create()
        VisionManager.setModelPerformanceConfig(
                ModelPerformanceConfig.Merged(
                        performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.LOW)
                )
        )

        back.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        VisionManager.start()
        mapboxNavigation.startNavigation(intent.getSerializableExtra(EXTRA_ROUTE) as DirectionsRoute)
        mapboxNavigation.addProgressChangeListener(mapbox_ar_view)
    }

    override fun onPause() {
        super.onPause()
        mapboxNavigation.removeProgressChangeListener(mapbox_ar_view)
        mapboxNavigation.stopNavigation()
        VisionManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        VisionManager.destroy()
    }
}
