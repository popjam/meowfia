package com.meowfia.app.util

import android.view.HapticFeedbackConstants
import android.view.View

/** Triggers a short haptic tick on confirm actions. */
fun View.hapticTick() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}
