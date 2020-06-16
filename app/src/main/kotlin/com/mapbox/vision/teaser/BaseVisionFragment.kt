package com.mapbox.vision.teaser

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.teaser.utils.classification.SignResources

open class BaseVisionFragment : Fragment() {

    protected var country = Country.Unknown
    protected var calibrationProgress = 0F
    protected var lastSpeed = 0f

    protected lateinit var signResources: SignResources

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
