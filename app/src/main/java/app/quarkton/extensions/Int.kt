package app.quarkton.extensions

import kotlin.math.abs

fun Int.toWc() = if (this < 0) -1 else 0

fun Int.vrStr(full: Boolean = false) =
    "v${abs(this)/256}R${abs(this) % 256}" +
            (if (full) ((if (this < 0) " (MC)" else "" )) else "")