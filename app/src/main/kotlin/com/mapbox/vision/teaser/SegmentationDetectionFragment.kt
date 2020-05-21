package com.mapbox.vision.teaser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_segmentation_detection.*

class SegmentationDetectionFragment : Fragment() {

    companion object {
        val TAG: String = SegmentationDetectionFragment::class.java.simpleName
        fun newInstance() = SegmentationDetectionFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_segmentation_detection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_segmentation_detection.setOnClickListener { requireActivity().onBackPressed() }
    }
}
