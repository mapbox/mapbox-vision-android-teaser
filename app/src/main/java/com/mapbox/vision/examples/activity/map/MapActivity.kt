package com.mapbox.vision.examples.activity.map

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.vision.examples.R
import kotlinx.android.synthetic.main.activity_ar_map.mapView
import kotlinx.android.synthetic.main.activity_map.back


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var locationLayerPlugin: LocationLayerPlugin? = null

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
        locationLayerPlugin?.onStart()
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
        locationLayerPlugin?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        enableLocationPlugin()
    }

    private fun enableLocationPlugin() {
        locationLayerPlugin = LocationLayerPlugin(mapView, mapboxMap).apply {
            cameraMode = CameraMode.TRACKING
            lifecycle.addObserver(this)
        }
    }
}
