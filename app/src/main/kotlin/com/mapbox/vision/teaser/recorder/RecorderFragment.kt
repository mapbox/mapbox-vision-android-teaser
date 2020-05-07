package com.mapbox.vision.teaser.recorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.vision.VisionManager
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.ar.core.models.RoutePoint
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.utils.getRoutePoints
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.android.synthetic.main.fragment_recorder.*

class RecorderFragment : Fragment() {

    companion object {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ssZ", Locale.US)
        private fun buildFileName() = dateFormat.format(Date(System.currentTimeMillis()))

        private const val ARG_PARAM_SESSIONS_PATH = "ARG_PARAM_SESSIONS_PATH"
        private const val ARG_PARAM_JSON_ROUTE = "ARG_PARAM_JSON_ROUTE"
        val TAG: String = RecorderFragment::class.java.simpleName

        fun newInstance(sessionsPath: String, jsonRoute: String?) = RecorderFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM_SESSIONS_PATH, sessionsPath)
                putString(ARG_PARAM_JSON_ROUTE, jsonRoute)
            }
        }
    }

    private lateinit var baseSessionsPath: String
    private var routePoints: Array<RoutePoint> = arrayOf()
    private var directionsRoute: DirectionsRoute? = null
    private var isArManagerInitiated = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        baseSessionsPath = arguments?.getString(ARG_PARAM_SESSIONS_PATH) ?: ""
        return if (baseSessionsPath.isNotEmpty()) {
            val jsonRoute = arguments?.getString(ARG_PARAM_JSON_ROUTE)
            if (jsonRoute != null) {
                directionsRoute = DirectionsRoute.fromJson(jsonRoute)
                routePoints = directionsRoute?.getRoutePoints() ?: arrayOf()
            }
            inflater.inflate(R.layout.fragment_recorder, container, false)
        } else {
            requireActivity().onBackPressed()
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initStopRecordButton()
    }

    private fun initStopRecordButton() {
        stop_record.setOnClickListener {
            VisionManager.stopRecording()
            requireActivity().onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        onResumeArManager()
        VisionManager.startRecording("$baseSessionsPath/${buildFileName()}")
    }

    override fun onPause() {
        super.onPause()
        onPauseArManager()
        VisionManager.stopRecording()
    }

    private fun onResumeArManager() {
        val routeEta = directionsRoute?.duration()?.toFloat() ?: 0f
        isArManagerInitiated = routePoints.isNotEmpty() && routeEta != 0f
        if (isArManagerInitiated) {
            VisionArManager.create(VisionManager)
            val arRoute = Route(points = routePoints, eta = routeEta)
            VisionArManager.setRoute(arRoute)
        }
    }

    private fun onPauseArManager() {
        if (isArManagerInitiated) {
            isArManagerInitiated = false
            VisionArManager.destroy()
        }
    }
}
