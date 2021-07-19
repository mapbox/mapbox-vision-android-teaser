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
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.camera.NavigationCamera.NAVIGATION_TRACKING_MODE_GPS
import com.mapbox.navigation.ui.map.NavigationMapboxMap
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
import com.mapbox.vision.teaser.utils.buildMapboxDistanceFormatter
import com.mapbox.vision.teaser.utils.getRoutePoints
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.teaser.view.show
import kotlinx.android.synthetic.main.activity_ar_navigation.ar_mode_view
import kotlinx.android.synthetic.main.activity_ar_navigation.ar_view
import kotlinx.android.synthetic.main.activity_ar_navigation.back
import kotlinx.android.synthetic.main.activity_ar_navigation_replayer.*
import java.util.concurrent.TimeUnit

class ArReplayNavigationActivity : AppCompatActivity(), MapboxMap.OnMapClickListener,
    OnMapReadyCallback, RoutesObserver {

    companion object {
        private const val KEY_SESSION_PATH = "key_session_path"
        private const val MAP_STYLE = Style.DARK
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
    private var mapboxNavigation: MapboxNavigation? = null
    private var currentLocation: Location? = null
    private var toolsVisible = false

    private lateinit var sessionPath: String
    private lateinit var mapView: MapView
    private lateinit var destination: Point

    private var activeArFeature: ArFeature = ArFeature.LaneAndFence
    private var isProgressChangingByUser = false
    private var visionReplyManagerWasInit = false

    private val visionListener = object : VisionEventsListener {
        override fun onVehicleStateUpdated(vehicleState: VehicleState) {
            runOnUiThread {
                if (visionReplyManagerWasInit) {
                    if (isProgressChangingByUser) {
                        return@runOnUiThread
                    }
                    val lat = vehicleState.geoLocation.geoCoordinate.latitude
                    val lon = vehicleState.geoLocation.geoCoordinate.longitude
                    val azimuth = vehicleState.geoLocation.azimuth
                    visionLocationEngine.setLocation(lat, lon, azimuth)
                }
            }
        }

        override fun onUpdateCompleted() {
            runOnUiThread {
                if (visionReplyManagerWasInit) {
                    playback_seek_bar_view.setProgress(VisionReplayManager.getProgress())
                }
            }
        }
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
                time = System.currentTimeMillis()
            }

            callbacks.forEach {
                it.onSuccess(LocationEngineResult.create(lastLocation))
            }
        }
    }

    private val locationCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult) {
            result.lastLocation?.let {
                currentLocation = it
            }
        }

        override fun onFailure(exception: java.lang.Exception) {
            exception.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_navigation_replayer)

        sessionPath = intent.getStringExtra(KEY_SESSION_PATH) ?: ""
        if (sessionPath.isEmpty()) {
            finish()
            return
        }

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
            requestLocationUpdates()

            mapboxMap.addOnMapClickListener(this@ArReplayNavigationActivity)

            mapboxNavigation = MapboxNavigation(
                NavigationOptions.Builder(applicationContext)
                    .distanceFormatter(
                        buildMapboxDistanceFormatter()
                    )
                    .accessToken(
                        getString(R.string.mapbox_access_token)
                    )
                    .locationEngine(visionLocationEngine)
                    .build()
            ).apply {
                startTripSession()
                registerRoutesObserver(this@ArReplayNavigationActivity)
            }

            navigationMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this)
                .build()
                .apply {
                    addProgressChangeListener(mapboxNavigation!!)
                    updateCameraTrackingMode(NAVIGATION_TRACKING_MODE_GPS)
                    updateLocationLayerRenderMode(RenderMode.GPS)
                }
        }
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        val route = routes.first()
        VisionArManager.setRoute(
            Route(
                points = route.getRoutePoints(),
                eta = route.duration().toFloat() ?: 0f
            )
        )
    }

    override fun onMapClick(point: LatLng): Boolean {
        if (currentLocation == null) {
            return true
        }
        destination = Point.fromLngLat(point.longitude, point.latitude)
        getRoute(destination)
        navigationMap?.clearMarkers()
        navigationMap?.addDestinationMarker(destination)
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
        visionReplyManagerWasInit = true

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

        visionReplyManagerWasInit = false
        VisionReplayManager.stop()
        VisionReplayManager.destroy()
        mapboxNavigation?.setRoutes(emptyList())
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        mapboxNavigation?.stopTripSession()
        removeLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

        mapboxNavigation?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun getRoute(destination: Point) {
        val origin = Point.fromLngLat(currentLocation!!.longitude, currentLocation!!.latitude)
        mapboxNavigation?.requestRoutes(
            routeOptions = RouteOptions.builder()
                .applyDefaultParams()
                .accessToken(Mapbox.getAccessToken()!!)
                .coordinates(listOf(origin, destination))
                .alternatives(false)
                .build()
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

    private fun moveCameraTo(location: Location) =
        navigationMap?.retrieveMap()?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                buildCameraPositionFrom(location, location.bearing.toDouble())
            ),
            TimeUnit.SECONDS.toMillis(2).toInt()
        )

    private fun buildCameraPositionFrom(location: Location, bearing: Double) = CameraPosition.Builder()
        .zoom(DEFAULT_ZOOM)
        .target(LatLng(location.latitude, location.longitude))
        .bearing(bearing)
        .tilt(DEFAULT_TILT)
        .build()
}
