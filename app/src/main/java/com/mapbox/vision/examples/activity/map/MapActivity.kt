package com.mapbox.vision.examples.activity.map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.vision.examples.R
import kotlinx.android.synthetic.main.activity_ar_map.mapView
import kotlinx.android.synthetic.main.activity_map.back


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val MAP_STYLE = "mapbox://styles/willwhite/cjkmusatv0rox2roea7dz7r1p"
    }

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
        mapboxMap.setStyle(Style.Builder().fromUri(MAP_STYLE))
    }
}
