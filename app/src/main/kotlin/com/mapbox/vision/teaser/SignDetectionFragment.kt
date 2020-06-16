package com.mapbox.vision.teaser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications
import com.mapbox.vision.teaser.models.UiSign
import com.mapbox.vision.teaser.utils.classification.Tracker
import com.mapbox.vision.teaser.utils.dpToPx
import kotlinx.android.synthetic.main.fragment_sign_detection.*

class SignDetectionFragment : BaseVisionFragment() {

    companion object {
        private const val ARG_INPUT_COUNTRY = "country"
        val TAG: String = SignDetectionFragment::class.java.simpleName
        fun newInstance(country: Country) = SignDetectionFragment().apply {
            arguments = Bundle().apply { putInt(ARG_INPUT_COUNTRY, country.ordinal) }
        }
        private const val TRACKER_DEFAULT_COUNT = 5
    }

    private var tracker: Tracker<UiSign> = Tracker(TRACKER_DEFAULT_COUNT)
    private var signSize = 0
    private var margin = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        country = Country.values()[arguments?.getInt(ARG_INPUT_COUNTRY) ?: Country.Unknown.ordinal]
        return inflater.inflate(R.layout.fragment_sign_detection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signSize = requireContext().dpToPx(64f).toInt()
        margin = requireContext().dpToPx(8f).toInt()
        back_sign_detection.setOnClickListener { requireActivity().onBackPressed() }
    }

    fun drawSigns(frameSignClassifications: FrameSignClassifications) {
        sign_info_container.removeAllViews()
        tracker.update(UiSign.getUiSigns(frameSignClassifications))
        val uiSigns = tracker.getCurrent()
        for (uiSign in uiSigns) {
            sign_info_container.addView(
                    ImageView(requireContext()).apply {
                        layoutParams =
                                ViewGroup.MarginLayoutParams(signSize, ViewGroup.LayoutParams.WRAP_CONTENT)
                                        .apply {
                                            leftMargin = margin
                                        }
                        setImageResource(signResources.getSignResource(uiSign, country))
                    }
            )
        }
    }
}
