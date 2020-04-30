package com.mapbox.vision.teaser.recorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.vision.VisionManager
import com.mapbox.vision.teaser.OnBackPressedListener
import com.mapbox.vision.teaser.R
import kotlinx.android.synthetic.main.fragment_recorder.*
import java.text.SimpleDateFormat
import java.util.*

class RecorderFragment : Fragment(), OnBackPressedListener {

    private var baseSessionsPath:String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        baseSessionsPath = arguments?.getString(ARG_PARAM_SESSIONS_PATH)
        return if (!baseSessionsPath.isNullOrEmpty()) {
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

    override fun onResume() {
        super.onResume()
        VisionManager.startRecording( "$baseSessionsPath/${buildFileName()}")
    }

    override fun onPause() {
        super.onPause()
        VisionManager.stopRecording()
    }

    private fun initStopRecordButton() {
        stop_record.setOnClickListener {
            VisionManager.stopRecording()
            requireActivity().onBackPressed()
        }
    }

    override fun onBackPressed(): Boolean {
        return true
    }

    companion object {

        private const val ARG_PARAM_SESSIONS_PATH = "ARG_PARAM_SESSIONS_PATH"
        val TAG: String = RecorderFragment::class.java.simpleName

        fun newInstance(sessionsPath: String): RecorderFragment {
            val bundle = Bundle()
            bundle.putString(ARG_PARAM_SESSIONS_PATH, sessionsPath)
            val fragment = RecorderFragment()
            fragment.arguments = bundle
            return fragment
        }

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ssZ", Locale.US)
        private fun buildFileName() = dateFormat.format(Date(System.currentTimeMillis()))
    }
}