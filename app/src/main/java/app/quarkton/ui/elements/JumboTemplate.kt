package app.quarkton.ui.elements

import androidx.annotation.RawRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles

@Composable
fun JumboTemplate(
    // @DrawableRes imageId: Int,
    @RawRes lottieId: Int,
    titleText: String,
    mainText: String,
    balloon: Boolean = true,
    shrink: Boolean = false,
    outTextPos: ((DpOffset) -> Unit)? = null,
    titleColor: Color = Color.Black,
    titleGraphics: (GraphicsLayerScope.() -> Unit)? = null,
    scrollState: ScrollState? = null,
    dark: Boolean = false,
    lottieIterations: Int = 1,
    lottieAnimStart: Float = 0f,
    lottieAnimEnd: Float = 1f,
    content: @Composable ColumnScope.() -> Unit
) {
    val front = if (dark) Color.White else Color.Black
    val back = if (dark) Color.Black else Color.White
    val baseMod = if (shrink) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
    Surface(
        modifier = baseMod, color = back
    ) {
        Column(
            modifier = baseMod.then(
                if (scrollState != null) Modifier.verticalScroll(scrollState) else Modifier
            ), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = if (balloon) Modifier.weight(1f).fillMaxWidth()
                else Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (balloon) {
                    Spacer(Modifier.weight(1f))
                }
                Lottie(
                    modifier = Modifier.size(120.dp), // imageId = imageId,
                    lottieId = lottieId, iterations = lottieIterations,
                    start = lottieAnimStart, end = lottieAnimEnd
                )
                Spacer(Modifier.height(12.dp))
            }
            Column(
                modifier = Modifier.fillMaxWidth(7f/9f)
            ) {
                Text(
                    text = titleText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().then(
                        if (outTextPos != null) {
                            val den = LocalDensity.current
                            Modifier.onGloballyPositioned() {
                                val pos = it.positionInWindow()
                                with (den) {
                                    outTextPos(DpOffset(pos.x.toDp(), pos.y.toDp()))
                                }
                            }
                        } else Modifier
                    ).then(
                        if (titleGraphics != null)
                            Modifier.graphicsLayer { titleGraphics(this) }
                        else Modifier
                    ),
                    style = Styles.wizardTitle,
                    color = titleColor
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = mainText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = Styles.mainText,
                    color = front
                )
            }
            Column(
                modifier = if (balloon) Modifier
                    .weight(1f)
                    .fillMaxWidth()
                else Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (balloon) {
                    Spacer(Modifier.weight(1f))
                }
                content()
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    QuarkTONWalletTheme {
        JumboTemplate(
            // imageId = R.drawable.ph_main,
            lottieId = R.raw.main,
            titleText = stringResource(R.string.ton_wallet),
            mainText = stringResource(R.string.ton_wallet_allows)
        ) {
            JumboButtons(
                mainText = stringResource(R.string.create_my_wallet),
                mainClicked = {},
                secText = stringResource(R.string.import_existing_wallet),
                secClicked = {})
        }
    }
}