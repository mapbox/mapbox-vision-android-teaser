package com.mapbox.vision.teaser.ar

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.utils.buildNavigationOptions
import kotlinx.android.synthetic.main.activity_ar_map.*

class ArMapActivity : AppCompatActivity(), MapboxMap.OnMapClickListener, OnMapReadyCallback {

    companion object {
        private val TAG = ArMapActivity::class.java.simpleName
        const val ARG_RESULT_JSON_ROUTE = "ARG_RESULT_JSON_ROUTE"
    }

    private var originPoint: Point? = null

    private lateinit var mapboxMap: MapboxMap
    private var mapboxNavigation: MapboxNavigation? = null

    private var destinationMarker: Marker? = null

    private var currentRoute: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var locationComponent: LocationComponent? = null

    private val locationObserver = object : LocationObserver {
        override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
            originPoint = Point.fromLngLat(
                enhancedLocation.longitude,
                enhancedLocation.latitude
            )
        }

        override fun onRawLocationChanged(rawLocation: Location) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_map)

        back.setOnClickListener {
            onBackPressed()
        }
        mapView.onCreate(savedInstanceState)
        start_ar.setOnClickListener {
            val route = currentRoute
            if (route != null) {
                val jsonRoute = route.toJson()
                val data = Intent().apply {
                    putExtra(ARG_RESULT_JSON_ROUTE, jsonRoute)
                }
                setResult(RESULT_OK, data)
                finish()
            } else {
                Toast.makeText(this, "Route is not ready yet!", Toast.LENGTH_LONG).show()
            }
        }

        mapboxNavigation = MapboxNavigation(buildNavigationOptions())
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapView.getMapAsync(this)
        mapboxNavigation?.startTripSession()
        mapboxNavigation?.registerLocationObserver(locationObserver)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.stopTripSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }

    override fun onMapClick(destination: LatLng): Boolean {
        destinationMarker?.let(mapboxMap::removeMarker)
        destinationMarker = mapboxMap.addMarker(MarkerOptions().position(destination))

        if (originPoint == null) {
            Toast.makeText(this, "Source location is not determined yet!", Toast.LENGTH_LONG).show()
            return false
        }

        getRoute(
            origin = originPoint!!,
            destination = Point.fromLngLat(destination.longitude, destination.latitude)
        )

        start_ar.visibility = View.VISIBLE

        return true
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(Style.DARK)) {
            enableLocationComponent()
        }

        mapboxMap.addOnMapClickListener(this)
    }

    private fun getRoute(origin: Point, destination: Point) {
        mapboxNavigation?.requestRoutes(
            routeOptions = RouteOptions.builder()
                .applyDefaultParams()
                .accessToken(Mapbox.getAccessToken()!!)
                .coordinates(listOf(origin, destination))
                .build(),
            routesRequestCallback = object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    currentRoute = routes.first()

                    // Draw the route on the map
                    if (navigationMapRoute == null) {
                        navigationMapRoute = NavigationMapRoute.Builder(
                            mapView,
                            mapboxMap,
                            this@ArMapActivity,
                        )
                            .withStyle(R.style.MapboxStyleNavigationMapRoute)
                            .build()
                    } else {
                        navigationMapRoute?.updateRouteVisibilityTo(false)
                    }
                    navigationMapRoute?.addRoute(currentRoute)
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    Toast.makeText(this@ArMapActivity, "Route request canceled!", Toast.LENGTH_LONG).show()
                }

                override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                    Toast.makeText(this@ArMapActivity, "Route request failure!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        val locationComponentOptions = LocationComponentOptions.builder(this)
            .build()
        locationComponent = mapboxMap.locationComponent

        val locationComponentActivationOptions = LocationComponentActivationOptions
            .builder(this, mapboxMap.style!!)
            .locationComponentOptions(locationComponentOptions)
            .build()

        locationComponent?.let {
            it.activateLocationComponent(locationComponentActivationOptions)
            it.isLocationComponentEnabled = true
            it.cameraMode = CameraMode.TRACKING
        }
    }
}
