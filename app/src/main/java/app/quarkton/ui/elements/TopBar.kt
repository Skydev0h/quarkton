package app.quarkton.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.quarkton.ui.theme.Styles

@Composable
fun TopBar(
    backIcon: Boolean = false,
    elevate: Float = 0f,
    titleText: String? = null,
    color: Color = Color.White,
    backColor: Color = Color.Black,
    textColor: Color = Color.Black,
    outBarPos: ((DpOffset) -> Unit)? = null,
    graphics: (GraphicsLayerScope.() -> Unit)? = null,
    textGraphics: (GraphicsLayerScope.() -> Unit)? = null,
    alignContent: Boolean = true,
    backClick: (() -> Unit)? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .zIndex(10f)
            .then(
                if (outBarPos != null) {
                    val den = LocalDensity.current
                    Modifier.onGloballyPositioned {
                        val pos = it.positionInWindow()
                        with (den) {
                            outBarPos(DpOffset(pos.x.toDp(), pos.y.toDp()))
                        }
                    }
                } else Modifier
            ).then(
                if (graphics != null)
                    Modifier.graphicsLayer { graphics(this) }
                else Modifier
            ),
        color = color,
        shadowElevation = elevate.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (backIcon) {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    BackButton(color = backColor, onClick = backClick)
                }
            }
            if ((titleText != null) || (content != null)) {
                if (titleText != null) {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = titleText,
                            style = Styles.smallPageTitle,
                            color = textColor,
                            modifier = Modifier.padding(20.dp, 0.dp, 0.dp, 0.dp)
                                .then(
                                    if (textGraphics != null)
                                        Modifier.graphicsLayer { textGraphics(this) }
                                    else Modifier
                                )
                        )
                    }
                }
                if (alignContent)
                    Spacer(modifier = Modifier.weight(1f))
                if (content != null) {
                    content()
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    TopBar(true, 8f, "Test title text")
}