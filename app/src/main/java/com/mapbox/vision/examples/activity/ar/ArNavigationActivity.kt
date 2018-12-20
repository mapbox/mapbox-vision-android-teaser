package com.mapbox.vision.examples.activity.ar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.Toast
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.RouteFetcher
import com.mapbox.services.android.navigation.v5.route.RouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.vision.VisionManager
import com.mapbox.vision.examples.R
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import kotlinx.android.synthetic.main.activity_ar_navigation.back
import kotlinx.android.synthetic.main.activity_ar_navigation.mapbox_ar_view

class ArNavigationActivity : AppCompatActivity(), LocationEngineListener, RouteListener {

    companion object {
        private const val EXTRA_ROUTE = "Route"

        fun start(context: Activity, directionsRoute: DirectionsRoute) {
            context.startActivity(
                    Intent(context, ArNavigationActivity::class.java)
                            .putExtra(EXTRA_ROUTE, directionsRoute)
            )
        }
    }

    private val arLocationEngine by lazy {
        val locationEngineProvider = LocationEngineProvider(this)
        locationEngineProvider.obtainBestLocationEngineAvailable().apply {
            priority = LocationEnginePriority.HIGH_ACCURACY
            interval = 0
            fastestInterval = 1000
        }
    }

    private lateinit var mapboxNavigation: MapboxNavigation

    private var lastKnownRoutProgress: RouteProgress? = null

    private val progressChangeListener = ProgressChangeListener { _, routeProgress ->
        lastKnownRoutProgress = routeProgress
    }

    private lateinit var routeFetcher: RouteFetcher

    private val offRouteListener = OffRouteListener { location ->
        routeFetcher.findRouteFromRouteProgress(location, lastKnownRoutProgress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        VisionManager.create()
        setContentView(R.layout.activity_ar_navigation)

        VisionManager.setModelPerformanceConfig(
                ModelPerformanceConfig.Merged(
                        performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.LOW)
                )
        )

        back.setOnClickListener {
            onBackPressed()
        }

        val builder = MapboxNavigationOptions
                .builder()
                .enableOffRouteDetection(true)
        mapboxNavigation = MapboxNavigation(this, getString(R.string.mapbox_access_token), builder.build())

        routeFetcher = RouteFetcher(this, getString(R.string.mapbox_access_token))
        routeFetcher.addRouteListener(this@ArNavigationActivity)
    }

    override fun onResume() {
        super.onResume()
        arLocationEngine.apply {
            addLocationEngineListener(this@ArNavigationActivity)
            activate()
        }
        VisionManager.start()
        mapboxNavigation.addOffRouteListener(mapbox_ar_view)
        mapboxNavigation.addOffRouteListener(offRouteListener)
        mapboxNavigation.addProgressChangeListener(mapbox_ar_view)
        mapboxNavigation.addProgressChangeListener(progressChangeListener)
        mapboxNavigation.locationEngine = arLocationEngine
        mapboxNavigation.startNavigation(intent.getSerializableExtra(EXTRA_ROUTE) as DirectionsRoute)
    }

    override fun onPause() {
        arLocationEngine.apply {
            removeLocationUpdates()
            removeLocationEngineListener(this@ArNavigationActivity)
            deactivate()
        }
        mapboxNavigation.removeProgressChangeListener(mapbox_ar_view)
        mapboxNavigation.removeProgressChangeListener(progressChangeListener)
        mapboxNavigation.removeOffRouteListener(mapbox_ar_view)
        mapboxNavigation.removeOffRouteListener(offRouteListener)
        mapboxNavigation.stopNavigation()
        VisionManager.stop()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        VisionManager.destroy()
    }

    override fun onLocationChanged(location: Location?) {
        // Do nothing
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() = arLocationEngine.requestLocationUpdates()

    override fun onErrorReceived(throwable: Throwable?) {
        mapboxNavigation.stopNavigation()
        Toast.makeText(this, R.string.can_not_colculate_new_rout, Toast.LENGTH_SHORT).show()
    }

    override fun onResponseReceived(response: DirectionsResponse, routeProgress: RouteProgress?) {
        mapboxNavigation.stopNavigation()
        if (response.routes().isEmpty()) {
            Toast.makeText(this, R.string.can_not_colculate_new_rout, Toast.LENGTH_SHORT).show()
            return
        }
        val route = response.routes()[0]
        mapboxNavigation.startNavigation(route)
    }

}
