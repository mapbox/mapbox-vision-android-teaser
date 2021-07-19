package com.mapbox.vision.teaser.ar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.vision.VisionManager
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.models.ArFeature
import com.mapbox.vision.teaser.utils.buildNavigationOptions
import com.mapbox.vision.teaser.utils.getRoutePoints
import kotlinx.android.synthetic.main.activity_ar_navigation.*

class ArNavigationActivity : AppCompatActivity(), RoutesObserver {

    companion object {
        private val TAG = ArNavigationActivity::class.java.simpleName

        private const val ARG_INPUT_JSON_ROUTE = "ARG_INPUT_JSON_ROUTE"

        fun start(context: Activity, jsonRoute: String) {
            val intent = Intent(context, ArNavigationActivity::class.java).apply {
                putExtra(ARG_INPUT_JSON_ROUTE, jsonRoute)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var directionsRoute: DirectionsRoute
    private lateinit var mapboxNavigation: MapboxNavigation

    private var activeArFeature: ArFeature = ArFeature.LaneAndFence

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_navigation)

        val jsonRoute = intent.getStringExtra(ARG_INPUT_JSON_ROUTE)
        if (jsonRoute.isNullOrEmpty()) {
            finish()
        }

        directionsRoute = DirectionsRoute.fromJson(jsonRoute)

        back.setOnClickListener {
            onBackPressed()
        }

        applyArFeature()
        ar_mode_view.setOnClickListener {
            activeArFeature = activeArFeature.getNextFeature()
            applyArFeature()
        }

        mapboxNavigation = MapboxNavigation(buildNavigationOptions())
    }

    private fun applyArFeature() {
        ar_mode_view.setImageResource(activeArFeature.drawableId)
        ar_view.setLaneVisible(activeArFeature.isLaneVisible)
        ar_view.setFenceVisible(activeArFeature.isFenceVisible)
    }

    override fun onResume() {
        super.onResume()

        mapboxNavigation?.startTripSession()
        mapboxNavigation.setRoutes(listOf(directionsRoute))

        VisionManager.create()
        VisionManager.start()
        VisionManager.setModelPerformance(
            ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.LOW)
        )

        VisionArManager.create(VisionManager)
        ar_view.setArManager(VisionArManager)
        ar_view.onResume()

        VisionArManager.setRoute(
            Route(
                points = directionsRoute.getRoutePoints(),
                eta = directionsRoute.duration().toFloat()
            )
        )
    }

    override fun onPause() {
        super.onPause()
        ar_view.onPause()
        VisionArManager.destroy()

        VisionManager.stop()
        VisionManager.destroy()

        mapboxNavigation.stopTripSession()
        mapboxNavigation.setRoutes(emptyList())
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        println("Routes changed ${routes.joinToString(", ")}")
        // TODO
    }
}
