package com.mapbox.vision.examples.activity.ar

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.vision.examples.R
import kotlinx.android.synthetic.main.activity_ar_map.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArMapActivity : AppCompatActivity(), MapboxMap.OnMapClickListener,
        LocationEngineListener, OnMapReadyCallback {

    private lateinit var locationLayerPlugin: LocationLayerPlugin
    private lateinit var locationEngine: LocationEngine
    private lateinit var originPoint: Point
    private lateinit var mapboxMap: MapboxMap

    private var destinationMarker: Marker? = null

    private var currentRoute: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_map)

        back.setOnClickListener {
            onBackPressed()
        }
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        start_ar.setOnClickListener {
            if (currentRoute == null) {
                Toast.makeText(this, "Route is not ready yet!", Toast.LENGTH_LONG).show()
            } else {
                ArNavigationActivity.start(this, currentRoute!!)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        if (::locationLayerPlugin.isInitialized) {
            locationLayerPlugin.onStart()
        }
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
        if (::locationLayerPlugin.isInitialized) {
            locationLayerPlugin.onStop()
        }
        locationEngine.removeLocationEngineListener(this)
        locationEngine.removeLocationUpdates()
        locationEngine.deactivate()
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

    override fun onMapClick(destination: LatLng) {
        destinationMarker?.let(mapboxMap::removeMarker)
        destinationMarker = mapboxMap.addMarker(MarkerOptions().position(destination))

        if (!::originPoint.isInitialized) {
            Toast.makeText(this, "Source location is not determined yet!", Toast.LENGTH_LONG).show()
            return
        }

        getRoute(
                origin = originPoint,
                destination = Point.fromLngLat(destination.longitude, destination.latitude)
        )

        start_ar.visibility = View.VISIBLE
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        enableLocationPlugin()
        mapboxMap.addOnMapClickListener(this)
    }

    override fun onLocationChanged(location: Location) {
        originPoint = Point.fromLngLat(location.longitude, location.latitude)
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() {
        locationEngine.requestLocationUpdates()
    }

    private fun getRoute(origin: Point, destination: Point) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        if (response.body() == null || response.body()!!.routes().size < 1) {
                            return
                        }

                        currentRoute = response.body()!!.routes()[0]

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute!!.removeRoute()
                        } else {
                            navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute)
                        }
                        navigationMapRoute!!.addRoute(currentRoute)
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {}
                })
    }

    private fun enableLocationPlugin() {
        initializeLocationEngine()
        locationLayerPlugin = LocationLayerPlugin(mapView, mapboxMap)
        locationLayerPlugin.cameraMode = CameraMode.TRACKING
        lifecycle.addObserver(locationLayerPlugin)
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.priority = LocationEnginePriority.HIGH_ACCURACY
        locationEngine.addLocationEngineListener(this)
        locationEngine.activate()

        val lastLocation = locationEngine.lastLocation

        if (lastLocation != null) {
            originPoint = Point.fromLngLat(lastLocation.longitude, lastLocation.latitude)
        }
    }
}
