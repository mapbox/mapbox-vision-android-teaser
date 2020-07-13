package com.mapbox.vision.examples.activity.ar

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
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
import com.mapbox.vision.common.models.ArFeature
import com.mapbox.vision.common.utils.buildStepPointsFromGeometry
import com.mapbox.vision.common.utils.mapToManeuverType
import com.mapbox.vision.examples.ConnectService
import com.mapbox.vision.examples.DemoApplication
import com.mapbox.vision.examples.R
import com.mapbox.vision.examples.activity.main.MainActivity
import com.mapbox.vision.mobile.core.models.CameraParameters
import com.mapbox.vision.mobile.core.models.position.GeoCoordinate
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.utils.VisionLogger
import com.mapbox.vision.vlc.externalcamera.ExternalVideoSourceImpl
import kotlinx.android.synthetic.main.activity_ar_navigation.*
import kotlinx.android.synthetic.main.activity_ar_navigation.back

class ArNavigationActivity : AppCompatActivity(), RouteListener, ProgressChangeListener,
    OffRouteListener {

    companion object {
        private var TAG = ArNavigationActivity::class.java.simpleName

        private const val LOCATION_INTERVAL_DEFAULT = 0L
        private const val LOCATION_INTERVAL_FAST = 1000L

        var directionsRoute: DirectionsRoute? = null

        fun start(context: Activity) {
            context.startActivity(Intent(context, ArNavigationActivity::class.java))
        }
    }

    private val arLocationEngine by lazy {
        LocationEngineProvider.getBestLocationEngine(this)
    }

    private val arLocationEngineRequest by lazy {
        LocationEngineRequest.Builder(LOCATION_INTERVAL_DEFAULT)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(LOCATION_INTERVAL_FAST)
            .build()
    }

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeFetcher: RouteFetcher

    private var lastRouteProgress: RouteProgress? = null
    private var activeArFeature: ArFeature = ArFeature.LaneAndFence

    private val locationCallback by lazy {
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
            }

            override fun onFailure(exception: Exception) {
            }
        }
    }
    val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.e(MainActivity.TAG, "onServiceDisconnected")

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.e(TAG, "onServiceConnected")

            (service as ConnectService.ServiceBinder).register(object : ConnectService.MessageReceiver {
                override fun onReceive(x: Double, y: Double, z: Double) {
                    Log.e(TAG, "receive x:$x, y:$y, z: $z")
                    VisionManager.setDeviceMotion(
                            accelerationX = x.toFloat(), // acceleration to the front, m/s2
                            accelerationY = y.toFloat(), // acceleration to the left, m/s2
                            accelerationZ = z.toFloat(), // acceleration to the top, m/s2
                            orientationX = 0f,  // angle along X axis, radians, zero if parallel to X, negative rotated clockwise
                            orientationY = -Math.PI.toFloat() / 2,  // angle along Y axis, radians, -PI/2 if parallel to Z, 0 if camera points to bottom
                            orientationZ = 0f,  // angle along Z axis, radians
                            gyroscopeX = 0f,    // acceleration along X axis, rad/s
                            gyroscopeY = 0f,    // acceleration along Y axis, rad/s
                            gyroscopeZ = 0f)     // acceleration along Z axis, rad/s)
                }
            })
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_navigation)

        back.setOnClickListener {
            onBackPressed()
        }

        applyArFeature()
        ar_mode_view.setOnClickListener {
            activeArFeature = activeArFeature.getNextFeature()
            applyArFeature()
        }

        val builder = MapboxNavigationOptions
            .builder()

        mapboxNavigation =
            MapboxNavigation(this, getString(R.string.mapbox_access_token), builder.build())

        routeFetcher = RouteFetcher(this, getString(R.string.mapbox_access_token))
        routeFetcher.addRouteListener(this)
    }

    private fun applyArFeature() {
        ar_mode_view.setImageResource(activeArFeature.drawableId)
        ar_view.setLaneVisible(activeArFeature.isLaneVisible)
        ar_view.setFenceVisible(activeArFeature.isFenceVisible)
    }

    override fun onResume() {
        super.onResume()
        try {
            arLocationEngine.requestLocationUpdates(
                arLocationEngineRequest,
                locationCallback,
                mainLooper
            )
        } catch (se: SecurityException) {
            VisionLogger.d(TAG, se.toString())
        }

        mapboxNavigation.addOffRouteListener(this)
        mapboxNavigation.addProgressChangeListener(this)
        mapboxNavigation.locationEngine = arLocationEngine

        val externalVideoSource = ExternalVideoSourceImpl(
                application = VisionManager.application,
                externalCameraIp = "rtsp://${DemoApplication.CAMERA_IP}/media/stream2", //for AP mode
//                externalCameraIp = "rtsp://${DemoApplication.CAMERA_IP}/media/stream2", //for STA mode
//                externalCameraIp = "/sdcard/Drive4K_East_3rd_Ring_Road_Beijing.mp4", //local mode
                externalCameraParameters = CameraParameters(
                        width = 1280,
                        height = 720,
                        focalInPixelsX = 1280f * 6.0f / 5.07f,
                        focalInPixelsY = 720f * 6.0f / 3.38f
                )
        )
        VisionManager.create(externalVideoSource, false)
        VisionManager.start()
        VisionManager.setModelPerformanceConfig(
            ModelPerformanceConfig.Merged(
                performance = ModelPerformance.On(
                    ModelPerformanceMode.FIXED,
                    ModelPerformanceRate.LOW
                )
            )
        )

        VisionArManager.create(VisionManager)
        ar_view.setArManager(VisionArManager)
        ar_view.onResume()

        directionsRoute.let {
            if (it == null) {
                Toast.makeText(this, "Route is not set!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                setRoute(it)
            }
        }

        val bindService = applicationContext.bindService(Intent(this, ConnectService::class.java), connection, Context.BIND_AUTO_CREATE)

    }

    override fun onPause() {
        super.onPause()
        ar_view.onPause()
        VisionArManager.destroy()

        VisionManager.stop()
        VisionManager.destroy()

        arLocationEngine.removeLocationUpdates(locationCallback)
        mapboxNavigation.removeProgressChangeListener(this)
        mapboxNavigation.removeOffRouteListener(this)
        mapboxNavigation.stopNavigation()
        unbindService(connection)
    }

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
        lastRouteProgress = routeProgress

        setRoute(response.routes()[0])
    }

    private fun setRoute(route: DirectionsRoute) {
        mapboxNavigation.startNavigation(route)

        VisionArManager.setRoute(
            Route(
                points = route.getRoutePoints(),
                eta = route.duration()?.toFloat() ?: 0f
            )
        )
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        lastRouteProgress = routeProgress
    }

    override fun userOffRoute(location: Location?) {
        routeFetcher.findRouteFromRouteProgress(location, lastRouteProgress)
    }

    private fun DirectionsRoute.getRoutePoints(): Array<RoutePoint> {
        val routePoints = arrayListOf<RoutePoint>()
        legs()?.forEach { leg ->
            leg.steps()?.forEach { step ->
                val maneuverPoint = RoutePoint(
                    GeoCoordinate(
                        latitude = step.maneuver().location().latitude(),
                        longitude = step.maneuver().location().longitude()
                    ),
                    step.maneuver().type().mapToManeuverType()
                )
                routePoints.add(maneuverPoint)

                step.geometry()
                    ?.buildStepPointsFromGeometry()
                    ?.map { geometryStep ->
                        RoutePoint(
                            GeoCoordinate(
                                latitude = geometryStep.latitude(),
                                longitude = geometryStep.longitude()
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
