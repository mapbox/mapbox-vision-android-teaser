package com.mapbox.vision.examples.activity.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.vision.examples.R
import kotlinx.android.synthetic.main.activity_ar_map.mapView
import kotlinx.android.synthetic.main.activity_map.*


class MapActivity : AppCompatActivity(), LocationEngineListener, OnMapReadyCallback {

    private lateinit var locationLayerPlugin: LocationLayerPlugin
    private lateinit var locationEngine: LocationEngine

    private lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        back.setOnClickListener { onBackPressed() }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        locationEngine.removeLocationEngineListener(this)
        locationEngine.removeLocationUpdates()
        locationEngine.deactivate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLocationChanged(location: Location?) {
        // DO nothing
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() {
        locationEngine.requestLocationUpdates()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        enableLocationPlugin()
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

    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, MapActivity::class.java)
    }
}
