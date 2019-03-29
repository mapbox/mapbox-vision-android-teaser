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
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.ar.core.models.RoutePoint
import com.mapbox.vision.examples.R
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.position.GeoCoordinate
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import kotlinx.android.synthetic.main.activity_ar_navigation.*

class ArNavigationActivity : AppCompatActivity(), LocationEngineListener, RouteListener, ProgressChangeListener,
    OffRouteListener {

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
    private lateinit var routeFetcher: RouteFetcher

    private var lastRouteProgress: RouteProgress? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_navigation)

        back.setOnClickListener {
            onBackPressed()
        }

        val builder = MapboxNavigationOptions
            .builder()
            .enableOffRouteDetection(true)
        mapboxNavigation = MapboxNavigation(this, getString(R.string.mapbox_access_token), builder.build())

        routeFetcher = RouteFetcher(this, getString(R.string.mapbox_access_token))
        routeFetcher.addRouteListener(this)
    }

    override fun onResume() {
        super.onResume()
        arLocationEngine.apply {
            addLocationEngineListener(this@ArNavigationActivity)
            activate()
        }
        mapboxNavigation.addOffRouteListener(this)
        mapboxNavigation.addProgressChangeListener(this)
        mapboxNavigation.locationEngine = arLocationEngine
        mapboxNavigation.startNavigation(intent.getSerializableExtra(EXTRA_ROUTE) as DirectionsRoute)

        VisionManager.create(visionEventsListener = object : VisionEventsListener {})
        VisionManager.start()
        VisionManager.setModelPerformanceConfig(
            ModelPerformanceConfig.Merged(
                performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.LOW)
            )
        )
        VisionManager.setVideoSourceListener(mapbox_ar_view)

        VisionArManager.create(VisionManager, mapbox_ar_view)
    }

    override fun onPause() {
        super.onPause()
        VisionArManager.destroy()

        VisionManager.stop()
        VisionManager.destroy()

        arLocationEngine.apply {
            removeLocationUpdates()
            removeLocationEngineListener(this@ArNavigationActivity)
            deactivate()
        }
        mapboxNavigation.removeProgressChangeListener(this)
        mapboxNavigation.removeOffRouteListener(this)
        mapboxNavigation.stopNavigation()
    }

    override fun onLocationChanged(location: Location?) {}

    @SuppressLint("MissingPermission")
    override fun onConnected() = arLocationEngine.requestLocationUpdates()

    override fun onErrorReceived(throwable: Throwable?) {
        throwable?.printStackTrace()

        mapboxNavigation.stopNavigation()
        Toast.makeText(this, R.string.can_not_calculate_new_route, Toast.LENGTH_SHORT).show()
    }

    override fun onResponseReceived(response: DirectionsResponse, routeProgress: RouteProgress?) {
        mapboxNavigation.stopNavigation()

        if (response.routes().isEmpty()) {
            Toast.makeText(this, R.string.can_not_calculate_new_route, Toast.LENGTH_SHORT).show()
            return
        }
        val route = response.routes()[0]
        mapboxNavigation.startNavigation(route)
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        lastRouteProgress = routeProgress

        VisionArManager.setRoute(
            Route(
                points = routeProgress!!.getRoutePoints(),
                eta = routeProgress.durationRemaining().toFloat(),
                sourceStreetName = "TODO()",
                targetStreetName = "TODO()"
            )
        )
    }

    override fun userOffRoute(location: Location?) {
        routeFetcher.findRouteFromRouteProgress(location, lastRouteProgress)
    }

    private fun RouteProgress.getRoutePoints(): Array<RoutePoint> {
        val routePoints = arrayListOf<RoutePoint>()
        this.directionsRoute()?.legs()?.forEach { it ->
            it.steps()?.forEach { step ->
                val maneuverPoint = RoutePoint(
                    GeoCoordinate(
                        latitude = step.maneuver().location().latitude(),
                        longitude = step.maneuver().location().longitude()
                    )
                )
                routePoints.add(maneuverPoint)

                step.intersections()
                    ?.map {
                        RoutePoint(
                            GeoCoordinate(
                                latitude = step.maneuver().location().latitude(),
                                longitude = step.maneuver().location().longitude()
                            )
                        )
                    }
                    ?.let { stepPoints ->
                        routePoints.addAll(stepPoints)
                    }
            }
        }

        return routePoints.toTypedArray()
    }
}
