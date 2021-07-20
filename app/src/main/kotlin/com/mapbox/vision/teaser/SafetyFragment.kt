package com.mapbox.vision.teaser

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLanguage
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.core.Rounding
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.vision.mobile.core.models.detection.DetectionClass
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.safety.core.VisionSafetyListener
import com.mapbox.vision.safety.core.models.CollisionDangerLevel
import com.mapbox.vision.safety.core.models.CollisionObject
import com.mapbox.vision.safety.core.models.RoadRestrictions
import com.mapbox.vision.teaser.models.UiSign
import com.mapbox.vision.teaser.utils.SoundsPlayer
import com.mapbox.vision.teaser.utils.requireVisionManager
import com.mapbox.vision.teaser.utils.runOnUiThreadIfResumed
import com.mapbox.vision.teaser.view.hide
import com.mapbox.vision.teaser.view.show
import com.mapbox.vision.utils.VisionLogger
import kotlinx.android.synthetic.main.fragment_safety.*

class SafetyFragment : BaseVisionFragment() {

    companion object {
        val TAG: String = SafetyFragment::class.java.simpleName
        private const val CALIBRATION_READY_VALUE = 1f
        fun newInstance() = SafetyFragment()
    }

    private lateinit var soundsPlayer: SoundsPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        soundsPlayer = SoundsPlayer(requireContext())
        return inflater.inflate(R.layout.fragment_safety, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProgress(0)
        back_safety.setOnClickListener { requireActivity().onBackPressed() }
        VisionLogger.e(TAG, "OnViewCreated()")
    }

    override fun onResume() {
        super.onResume()
        val visionManager = requireVisionManager()
        if (visionManager == null) {
            requireActivity().onBackPressed()
            return
        }
        VisionSafetyManager.create(visionManager)
        VisionSafetyManager.visionSafetyListener = visionSafetyListener
    }

    override fun onPause() {
        super.onPause()
        soundsPlayer.stop()
        VisionSafetyManager.destroy()
    }

    private fun setProgress(value: Int) {
        calibration_progress.text = getString(R.string.calibration_progress, value)
    }

    private val visionSafetyListener = object : VisionSafetyListener {

        private var currentDangerLevel: CollisionDangerLevel = CollisionDangerLevel.None

        private val distanceFormatter by lazy {
            MapboxDistanceFormatter.Builder(
                requireContext(),
            )
                .locale(
                    requireActivity().inferDeviceLocale(),
                )
                .roundingIncrement(
                    Rounding.INCREMENT_FIVE
                )
                .unitType(
                    requireActivity().inferDeviceLocale().getUnitTypeForLocale(),
                )
                .build()
        }

        override fun onCollisionsUpdated(collisions: Array<CollisionObject>) {

            fun updateCollisionDangerLevelIfChanged(collision: CollisionObject) {
                if (currentDangerLevel != collision.dangerLevel) {
                    soundsPlayer.stop()
                    when (collision.dangerLevel) {
                        CollisionDangerLevel.None -> Unit
                        CollisionDangerLevel.Warning -> soundsPlayer.playWarning()
                        CollisionDangerLevel.Critical -> soundsPlayer.playCritical()
                    }
                    currentDangerLevel = collision.dangerLevel
                }
            }

            fun showCollision(collision: CollisionObject) {
                distance_to_car_label.show()
                safety_mode.show()
                distance_to_car_label.text =
                        distanceFormatter.formatDistance(collision.`object`.position.y)

                when (currentDangerLevel) {
                    CollisionDangerLevel.None -> safety_mode.clean()
                    CollisionDangerLevel.Warning -> safety_mode.drawWarnings(collisions)
                    CollisionDangerLevel.Critical -> safety_mode.drawCritical()
                }
            }

            fun hideCollision() {
                soundsPlayer.stop()
                currentDangerLevel = CollisionDangerLevel.None
                distance_to_car_label.hide()
                safety_mode.hide()
            }

            fun calibrationReady() {
                distance_to_car_label.show()
                safety_mode.show()
                calibration_progress.hide()

                val collision = collisions.firstOrNull { it.`object`.objectClass == DetectionClass.Car }
                if (collision == null) {
                    hideCollision()
                } else {
                    updateCollisionDangerLevelIfChanged(collision)
                    showCollision(collision)
                }
            }

            fun calibrationInProgress() {
                distance_to_car_label.hide()
                safety_mode.hide()
                calibration_progress.show()
                val progress = (calibrationProgress * 100).toInt()
                setProgress(progress)
            }

            runOnUiThreadIfResumed {
                if (calibrationProgress == CALIBRATION_READY_VALUE) {
                    calibrationReady()
                } else {
                    calibrationInProgress()
                }
            }
        }

        val speedLimitTranslation by lazy {
            resources.getDimension(R.dimen.speed_limit_translation)
        }

        override fun onRoadRestrictionsUpdated(roadRestrictions: RoadRestrictions) {

            fun getImageResource() = signResources.getSpeedSignResource(
                    UiSign.WithNumber(
                            signType = UiSign.SignType.SpeedLimit,
                            signNumber = UiSign.SignNumber.fromNumber(roadRestrictions.speedLimits.car.max)
                    ),
                    speed = lastSpeed,
                    country = country
            )

            fun cancelAnimations() {
                speed_limit_current.animate().cancel()
                speed_limit_next.animate().cancel()
            }

            fun showCurrentSpeedLimit(imageResource: Int) {
                speed_limit_current.apply {
                    show()
                    translationY = 0f
                    alpha = 1f
                    animate()
                            .translationY(speedLimitTranslation / 2)
                            .alpha(0f)
                            .scaleX(0.5f)
                            .scaleY(0.5f)
                            .setDuration(500L)
                            .setListener(
                                    object : Animator.AnimatorListener {
                                        override fun onAnimationRepeat(animation: Animator?) {}

                                        override fun onAnimationEnd(animation: Animator?) {
                                            setImageResource(imageResource)
                                            translationY = 0f
                                            alpha = 1f
                                            scaleX = 1f
                                            scaleY = 1f
                                            speed_limit_next.hide()
                                        }

                                        override fun onAnimationCancel(animation: Animator?) {}

                                        override fun onAnimationStart(animation: Animator?) {}
                                    }
                            )
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                }
            }

            fun showSpeedLimitNextIfRequired(imageResource: Int) {
                if (roadRestrictions.speedLimits.car.max != 0f) {
                    speed_limit_next.apply {
                        translationY = -speedLimitTranslation
                        setImageResource(imageResource)
                        show()
                        animate().translationY(0f)
                                .setDuration(500L)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                    }
                } else {
                    speed_limit_next.hide()
                }
            }

            runOnUiThreadIfResumed {
                val imageResource = getImageResource()
                cancelAnimations()
                showCurrentSpeedLimit(imageResource)
                showSpeedLimitNextIfRequired(imageResource)
            }
        }
    }
}
