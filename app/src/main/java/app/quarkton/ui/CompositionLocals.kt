package app.quarkton.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

// Back Fix Crutch - recreates BackHandler shortly after Resuming activity
val LocalBFC = staticCompositionLocalOf<MutableState<Boolean>> {
    mutableStateOf(true)
}

@Suppress("SameParameterValue")
private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
