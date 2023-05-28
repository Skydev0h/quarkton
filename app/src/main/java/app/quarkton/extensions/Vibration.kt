package app.quarkton.extensions

import android.os.Build.VERSION as VER
import android.os.Build.VERSION_CODES.R as R
import android.view.View
import android.view.HapticFeedbackConstants as HFC

@Suppress("DEPRECATION")
fun View.vibrate(type: Int) = performHapticFeedback(type, HFC.FLAG_IGNORE_GLOBAL_SETTING)
fun View.vibrateLongPress() = vibrate(HFC.LONG_PRESS)
fun View.vibrateKeyPress() = vibrate(HFC.KEYBOARD_TAP)
fun View.vibrateError() = vibrate(if (VER.SDK_INT >= R) HFC.REJECT else HFC.LONG_PRESS)
