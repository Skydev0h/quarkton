package app.quarkton.ui.elements


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.quarkton.R
import app.quarkton.ui.LocalBFC
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles

const val ALERT_BACKDROP_CLICKED = -1
const val ALERT_BACK_BUTTON_PRESSED = -2

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Alert(
    enabled: Boolean,
    titleText: String,
    mainText: String,
    buttons: IntArray,
    clickHandler: (Int) -> Unit
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .zIndex(20f)) {
        if (LocalBFC.current.value)
            BackHandler(enabled = enabled) {
                clickHandler(
                    ALERT_BACK_BUTTON_PRESSED
                )
            }
        AnimatedVisibility(visible = enabled,
            enter = fadeIn(), exit = fadeOut()
        ) {
            Overlay(visible = true, darker = true, backdropClicked = {
                clickHandler(
                    ALERT_BACKDROP_CLICKED
                )
            })
        }
        AnimatedVisibility(visible = enabled,
            enter = scaleIn() + fadeIn(initialAlpha = 0.5f),
            exit = scaleOut() + fadeOut(targetAlpha = 0.5f)
        ) {
            Box(modifier = Modifier.zIndex(110f)) {
                Centrify {
                    Surface(
                        color = Color.White,
                        shadowElevation = 20.dp,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(320.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp, 22.dp, 8.dp, 12.dp)
                        ) {
                            Text(
                                text = titleText,
                                style = Styles.modalTitle
                            )
                            Text(
                                text = mainText,
                                style = Styles.mainText,
                                modifier = Modifier
                                    .width(272.dp)
                                    .padding(0.dp, 12.dp, 0.dp, 16.dp)
                            )
                            Row(modifier = Modifier.width(288.dp)) {
                                Spacer(modifier = Modifier.weight(1f))
                                for (btnId in buttons) {
                                    TextButton(
                                        onClick = { clickHandler(btnId) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(20.dp, 12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = btnId),
                                            style = Styles.modalButton
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    QuarkTONWalletTheme {
        Alert(
            enabled = true,
            titleText = stringResource(R.string.sure_done),
            mainText = stringResource(R.string.write_words_too_fast),
            buttons = intArrayOf(R.string.btn_skip, R.string.btn_ok_sorry),
            clickHandler = {}
        )
    }
}