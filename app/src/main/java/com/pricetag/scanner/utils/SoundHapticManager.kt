package com.pricetag.scanner.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundHapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val toneGen by lazy {
        try { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }
        catch (_: Exception) { null }
    }

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** Single beep — successful scan */
    fun beepSuccess(enabled: Boolean) {
        if (!enabled) return
        try { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 120) }
        catch (_: Exception) {}
    }

    /** Double beep — error / duplicate */
    fun beepError(enabled: Boolean) {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (_: Exception) {}
    }

    /** Short vibration pulse — scan success */
    fun vibrate(enabled: Boolean) {
        if (!enabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (_: Exception) {}
    }
}
