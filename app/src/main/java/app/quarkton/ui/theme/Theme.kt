package app.quarkton.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun QuarkTONWalletTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = Colors.Primary,
        secondary = Colors.ModalText,
        tertiary = Colors.BackRefresh,
        background = Color.White
    ) // Always light because of contest UI requirements
    val view = LocalView.current

    if (!view.isInEditMode) {
        val currentWindow = (view.context as? Activity)?.window
        if (currentWindow != null) {
            SideEffect {
                currentWindow.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(currentWindow, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Styles.Typography,
        content = content
    )
}