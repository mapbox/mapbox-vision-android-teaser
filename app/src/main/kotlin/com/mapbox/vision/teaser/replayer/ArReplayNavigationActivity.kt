package com.mapbox.vision.teaser.replayer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Xml
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.ui.v5.camera.DynamicCamera
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCameraUpdate
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.position.VehicleState
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.models.ArFeature
import com.mapbox.vision.teaser.utils.getRoutePoints
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.teaser.view.show
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_ar_navigation.ar_mode_view
import kotlinx.android.synthetic.main.activity_ar_navigation.ar_view
import kotlinx.android.synthetic.main.activity_ar_navigation.back
import kotlinx.android.synthetic.main.activity_ar_navigation_replayer.*
import kotlinx.android.synthetic.main.activity_ar_navigation_replayer.playback_seek_bar_view
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArReplayNavigationActivity : AppCompatActivity(), MapboxMap.OnMapClickListener,
    OnMapReadyCallback {

    companion object {
        private const val KEY_SESSION_PATH = "key_session_path"
        private const val MAP_STYLE = "mapbox://styles/mapbox/dark-v10"
        private const val BEARING_TOLERANCE = 90.0
        private const val DEFAULT_ZOOM = 14.0
        private const val DEFAULT_TILT = 0.0
        private const val DEFAULT_BEARING = 0.0

        fun start(context: Context, sessionPath: String) {
            context.startActivity(
                Intent(
                    context,
                    ArReplayNavigationActivity::class.java
                ).apply { putExtra(KEY_SESSION_PATH, sessionPath) })
        }
    }

    private var navigationMap: NavigationMapboxMap? = null
    private var navigation: MapboxNavigation? = null
    private var lastLocation: Location? = null
    private var toolsVisible = false

    private lateinit var sessionPath: String
    private lateinit var mapView: MapView
    private lateinit var destination: Point

    private var activeArFeature: ArFeature = ArFeature.LaneAndFence
    private var isProgressChangingByUser = false

    private val visionListener = object : VisionEventsListener {
        override fun onVehicleStateUpdated(vehicleState: VehicleState) {
            runOnUiThread {
                if (isProgressChangingByUser) {
                    return@runOnUiThread
                }
                val lat = vehicleState.geoLocation.geoCoordinate.latitude
                val lon = vehicleState.geoLocation.geoCoordinate.longitude
                val azimuth = vehicleState.geoLocation.azimuth
                visionLocationEngine.setLocation(lat, lon, azimuth)
            }
        }

        override fun onUpdateCompleted() {
            runOnUiThread {
                playback_seek_bar_view.setProgress(VisionReplayManager.getProgress())
            }
        }
    }

    private val offRouteListener = OffRouteListener { getRoute(destination) }

    private val routeProgressListener = ProgressChangeListener { location, _ ->
        lastLocation = location
        navigationMap?.updateLocation(location)
    }

    private val visionLocationEngine = object : LocationEngine {

        private var callbacks: MutableSet<LocationEngineCallback<LocationEngineResult>> =
            mutableSetOf()

        private var lastLocation: Location? = null

        override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
            callbacks.remove(callback)
        }

        override fun removeLocationUpdates(pendingIntent: PendingIntent?) {}

        override fun requestLocationUpdates(
            request: LocationEngineRequest,
            callback: LocationEngineCallback<LocationEngineResult>,
            looper: Looper?
        ) {
            callbacks.add(callback)
        }

        override fun requestLocationUpdates(
            request: LocationEngineRequest,
            pendingIntent: PendingIntent?
        ) {
        }

        override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
            callback.onSuccess(LocationEngineResult.create(lastLocation))
        }

        fun setLocation(lat: Double, lon: Double, azimuth: Double) {
            if (lastLocation == null) {
                lastLocation = Location("Vision")
            }

            lastLocation!!.apply {
                latitude = lat
                longitude = lon
                bearing = azimuth.toFloat()
            }

            callbacks.forEach {
                it.onSuccess(LocationEngineResult.create(lastLocation))
            }
        }
    }

    private val locationCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult) {
            val location = result.lastLocation ?: return

            if (lastLocation == null) {
                moveCameraTo(location)
                navigationMap?.retrieveMap()
                    ?.addOnMapClickListener(this@ArReplayNavigationActivity)
            }

            lastLocation = location
            navigationMap?.updateLocation(location)
        }

        override fun onFailure(exception: java.lang.Exception) {
            exception.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_navigation_replayer)

        sessionPath = intent.getStringExtra(KEY_SESSION_PATH)

        back.setOnClickListener { onBackPressed() }

        ar_view.setOnClickListener {
            if (toolsVisible) {
                hideTools()
            } else {
                showTools()
            }
            toolsVisible = !toolsVisible
        }
        applyArFeature()
        ar_mode_view.setOnClickListener {
            activeArFeature = activeArFeature.getNextFeature()
            applyArFeature()
        }
        initMapView(savedInstanceState)
    }

    private fun applyArFeature() {
        ar_mode_view.setImageResource(activeArFeature.drawableId)
        ar_view.setLaneVisible(activeArFeature.isLaneVisible)
        ar_view.setFenceVisible(activeArFeature.isFenceVisible)
    }

    @SuppressLint("ResourceType")
    private fun initMapView(savedInstanceState: Bundle?) {
        val parser = resources.getXml(R.layout.map_attrs)
        try {
            parser.next()
            parser.nextTag()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val attr = Xml.asAttributeSet(parser)
        mapView = MapView(this, attr)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.Builder().fromUri(MAP_STYLE)) {
            navigationMap = NavigationMapboxMap(mapView, mapboxMap)

            requestLocationUpdates()

            navigation = MapboxNavigation(this, getString(R.string.mapbox_access_token)).apply {
                locationEngine = visionLocationEngine
                cameraEngine = DynamicCamera(mapboxMap)
                addOffRouteListener(offRouteListener)
                addProgressChangeListener(routeProgressListener)
            }
            navigationMap?.apply {
                addProgressChangeListener(navigation!!)
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        destination = Point.fromLngLat(point.longitude, point.latitude)
        getRoute(destination)
        navigationMap?.clearMarkers()
        navigationMap?.addMarker(this, destination)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        visionLocationEngine.requestLocationUpdates(
            LocationEngineRequest.Builder(1).build(),
            locationCallback,
            null
        )
    }

    private fun removeLocationUpdates() {
        visionLocationEngine.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        VisionReplayManager.create(sessionPath)
        VisionReplayManager.visionEventsListener = visionListener
        VisionReplayManager.start()

        VisionReplayManager.setModelPerformance(
            modelPerformance = ModelPerformance.On(
                ModelPerformanceMode.FIXED,
                ModelPerformanceRate.LOW
            )
        )

        playback_seek_bar_view.setDuration(VisionReplayManager.getDuration())
        playback_seek_bar_view.onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    VisionReplayManager.setProgress(progress.toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isProgressChangingByUser = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isProgressChangingByUser = false
            }
        }

        VisionArManager.create(VisionReplayManager)
        ar_view.setArManager(VisionArManager)
        ar_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        ar_view.onPause()

        VisionArManager.destroy()

        VisionReplayManager.stop()
        VisionReplayManager.destroy()

        navigation?.stopNavigation()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        navigationMap?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        navigationMap?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

        navigation?.run {
            (cameraEngine as DynamicCamera).clearMap()
            onDestroy()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun getRoute(destination: Point) {
        val origin = Point.fromLngLat(lastLocation!!.longitude, lastLocation!!.latitude)
        val bearing = java.lang.Float.valueOf(lastLocation!!.bearing).toDouble()
        NavigationRoute.builder(this)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(origin, bearing, BEARING_TOLERANCE)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (response.body() == null || response.body()!!.routes().size < 1) {
                        return
                    }

                    handleRoute(response.body()!!.routes()[0])
                }

                override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                    throwable.printStackTrace()
                }
            })
    }

    private fun handleRoute(route: DirectionsRoute) {
        navigationMap?.run {
            drawRoute(route)
            resumeCamera(lastLocation!!)
        }

        navigation?.startNavigation(route)

        // Location updates will be received from ProgressChangeListener
        removeLocationUpdates()

        navigationMap?.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)

        val cameraUpdate = cameraOverheadUpdate()
        cameraUpdate?.let {
            val navUpdate = NavigationCameraUpdate(it)
            navigationMap?.retrieveCamera()?.update(navUpdate)
        }

        VisionArManager.setRoute(
            Route(
                points = route.getRoutePoints(),
                eta = route.duration()?.toFloat() ?: 0f
            )
        )
    }

    private fun hideTools() {
        playback_seek_bar_view.hide()
        map_container.removeView(mapView)
    }

    private fun showTools() {
        playback_seek_bar_view.show()
        map_container.addView(mapView)
    }

    private fun moveCameraTo(location: Location) {
        val cameraPosition = buildCameraPositionFrom(location, location.bearing.toDouble())
        navigationMap?.retrieveMap()?.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition),
            TimeUnit.SECONDS.toMillis(2).toInt()
        )
    }

    private fun buildCameraPositionFrom(location: Location, bearing: Double): CameraPosition {
        return CameraPosition.Builder()
            .zoom(DEFAULT_ZOOM)
            .target(LatLng(location.latitude, location.longitude))
            .bearing(bearing)
            .tilt(DEFAULT_TILT)
            .build()
    }

    private fun cameraOverheadUpdate(): CameraUpdate? {
        if (lastLocation == null) {
            return null
        }
        val cameraPosition = buildCameraPositionFrom(lastLocation!!, DEFAULT_BEARING)
        return CameraUpdateFactory.newCameraPosition(cameraPosition)
    }
}
