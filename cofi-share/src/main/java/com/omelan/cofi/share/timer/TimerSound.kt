package com.omelan.cofi.share.timer

import androidx.annotation.StringRes
import com.omelan.cofi.share.R

enum class TimerSound(val rawResId: Int, @StringRes val nameResId: Int) {
    DING(R.raw.ding, R.string.sound_ding),
    BELL(R.raw.bell, R.string.sound_bell),
    BEEP(R.raw.beep, R.string.sound_beep),
    CARRIAGE(R.raw.carriage, R.string.sound_carriage)
}