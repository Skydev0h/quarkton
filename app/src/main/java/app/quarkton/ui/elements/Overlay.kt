package app.quarkton.ui.elements

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.quarkton.ui.LocalBFC
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme

@Composable
fun Overlay(
    visible: Boolean,
    showProgress: Boolean = false,
    progress: Float? = null,
    progressIsBlocking: Boolean = true,
    darker: Boolean = false,
    color: Color = Colors.Primary,
    backdropClicked: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    zIndex: Float = 100f,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    // val interactionSource = remember { MutableInteractionSource() }
    if (visible) {
        Surface(
            modifier = Modifier.fillMaxSize().zIndex(zIndex),
            color = if (darker) Colors.DarkShade else Colors.Shade
        ) {
            if (backdropClicked != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            //indication = null,
                            //interactionSource = interactionSource
                        ) { backdropClicked() },
                    color = Colors.Transparent
                ) {}
            }
            if (showProgress) {
                if (LocalBFC.current.value)
                    BackHandler(enabled = progressIsBlocking) { Log.i("Overlay", "Blocked back") }
                Centrify {
                    Surface(
                        modifier = Modifier.size(130.dp),
                        color = Color.White,
                        shadowElevation = 20.dp,
                    ) {
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                strokeWidth = 5.dp,
                                color = color
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                strokeWidth = 5.dp,
                                color = color
                            )
                        }
                    }
                }
            }
            if (content != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
                    content()
                }
            }
        }
    }
}

@Preview
@Composable
private fun SimpleOverlay() {
    QuarkTONWalletTheme {
        Overlay(visible = true)
    }
}

@Preview
@Composable
private fun ProgressOverlay() {
    QuarkTONWalletTheme {
        Overlay(visible = true, showProgress = true)
    }
}

@Preview
@Composable
private fun ProgressOverlay33pct() {
    QuarkTONWalletTheme {
        Overlay(visible = true, showProgress = true, progress = 0.3f)
    }
}