package com.mapbox.vision.examples.utils

import android.content.Context
import android.media.SoundPool
import com.mapbox.vision.examples.R

class SoundsPlayer(context: Context) {
    private var soundPoolActiveStreamId: Int = 0

    private var criticalSoundId: Int = 0
    private var warningSoundId: Int = 0
    private var soundPool: SoundPool? = null
    private var criticalSoundLoaded: Boolean = false
    private var warningSoundLoaded: Boolean = false

    init {
        soundPool = SoundPool.Builder().build().apply {
            setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    when (sampleId) {
                        criticalSoundId -> criticalSoundLoaded = true
                        warningSoundId -> warningSoundLoaded = true
                    }
                }
            }
            criticalSoundId = load(context, R.raw.collision_alert_critical, 1)
            warningSoundId = load(context, R.raw.collision_alert_warning, 1)
        }
    }

    fun playCritical() {
        if (criticalSoundLoaded) {
            soundPoolActiveStreamId = soundPool!!.play(criticalSoundId, 1f, 1f, 1, -1, 1f)
        }
    }

    fun playWarning() {
        if (warningSoundLoaded) {
            soundPoolActiveStreamId = soundPool!!.play(warningSoundId, 1f, 1f, 1, -1, 1f)
        }
    }

    fun stop() = soundPool!!.stop(soundPoolActiveStreamId)
}
