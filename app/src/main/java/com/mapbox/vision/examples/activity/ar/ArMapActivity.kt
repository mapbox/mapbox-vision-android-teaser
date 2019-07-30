package com.mapbox.vision.examples.activity.ar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.vision.examples.R
import com.mapbox.vision.utils.VisionLogger
import kotlinx.android.synthetic.main.activity_ar_map.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArMapActivity : AppCompatActivity(), MapboxMap.OnMapClickListener, OnMapReadyCallback {

    companion object {
        private var TAG = ArMapActivity::class.java.simpleName
        private const val MAP_STYLE = "mapbox://styles/mapbox/dark-v10"
    }

    private var originPoint: Point? = null

    private lateinit var mapboxMap: MapboxMap

    private var destinationMarker: Marker? = null

    private var currentRoute: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var locationComponent: LocationComponent? = null

    private val arLocationEngine by lazy {
        LocationEngineProvider.getBestLocationEngine(this)
    }

    private val arLocationEngineRequest by lazy {
        LocationEngineRequest.Builder(0)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build()
    }

    private val locationCallback by lazy {
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                with(result as LocationEngineResult) {
                    originPoint = Point.fromLngLat(lastLocation?.longitude ?: .0, lastLocation?.latitude?: .0)
                }
            }

            override fun onFailure(exception: Exception) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_map)

        back.setOnClickListener {
            onBackPressed()
        }
        mapView.onCreate(savedInstanceState)
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
        mapView.getMapAsync(this)
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
        arLocationEngine.removeLocationUpdates(locationCallback)
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
        mapboxMap.setStyle(Style.Builder().fromUri(MAP_STYLE)) {
            enableLocationComponent()
        }

        mapboxMap.addOnMapClickListener(this)
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
                        navigationMapRoute!!.updateRouteVisibilityTo(false)
                    } else {
                        navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute)
                    }
                    navigationMapRoute!!.addRoute(currentRoute)
                }

                override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {}
            })
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        initializeLocationEngine()

        val locationComponentOptions = LocationComponentOptions.builder(this)
                .build()
        locationComponent = mapboxMap.locationComponent

        val locationComponentActivationOptions = LocationComponentActivationOptions
                .builder(this, mapboxMap.style!!)
                .locationEngine(arLocationEngine)
                .locationComponentOptions(locationComponentOptions)
                .build()

        locationComponent?.let {
            it.activateLocationComponent(locationComponentActivationOptions)
            it.isLocationComponentEnabled = true
            it.cameraMode = CameraMode.TRACKING
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
        try {
            arLocationEngine.requestLocationUpdates(arLocationEngineRequest, locationCallback, mainLooper)
        } catch (se: SecurityException) {
            VisionLogger.d(TAG, se.toString())
        }

        arLocationEngine.getLastLocation(locationCallback)
    }
}
