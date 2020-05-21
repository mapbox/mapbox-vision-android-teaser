package com.mapbox.vision.teaser

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.teaser.utils.classification.SignResources
import com.mapbox.vision.teaser.utils.dpToPx

open class BaseVisionFragment : Fragment() {

    protected var country = Country.Unknown
    protected var calibrationProgress = 0F
    protected var lastSpeed = 0f

    protected var signSize = 0
    protected var margin = 0

    protected lateinit var signResources: SignResources

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signSize = requireContext().dpToPx(64f).toInt()
        margin = requireContext().dpToPx(8f).toInt()
        signResources = SignResources.Impl(requireContext())
    }

    fun updateCountry(country: Country) {
        this.country = country
    }

    fun updateCalibrationProgress(calibrationProgress: Float) {
        this.calibrationProgress = calibrationProgress
    }

    fun updateLastSpeed(lastSpeed: Float) {
        this.lastSpeed = lastSpeed
    }
}
