package app.quarkton.ui.elements

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalItem(
    text: String? = null,
    subText: String? = null,
    value: String? = null,
    toggle: Boolean? = null,
    color: Color? = null,
    valueColor: Color? = null,
    subColor: Color? = null,
    header: String? = null,
    last: Boolean = false,
    enabled: Boolean = true,
    style: TextStyle = Styles.mainText,
    valueStyle: TextStyle = Styles.mainText,
    subStyle: TextStyle = Styles.smallHint,
    disableRipple: Boolean = false,
    gemIcon: Boolean = false,
    preGemIcon: Boolean = false,
    progIcon: Boolean = false,
    onLongClick: ((String) -> Unit)? = null,
    onClick: ((String) -> Unit)? = null
) {
    val nullRT = remember { NullRippleTheme() }
    if (header != null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(text = header, color = color ?: Colors.Primary, style = Styles.smallHeader,
                modifier = Modifier.padding(20.dp, 20.dp, 0.dp, 4.dp))
        }
        return
    }
    if (text == null) return
    val realRT = LocalRippleTheme.current
    CompositionLocalProvider(
        LocalRippleTheme provides (if (disableRipple) nullRT else realRT)
    ) {
        Surface(
            color = Color.White,
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = { onClick?.invoke(text) },
                onLongClick = { onLongClick?.invoke(text) }
            )
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.padding(20.dp, 14.dp)) {
                        Row {
                            if (preGemIcon) {
                                Image(painter = painterResource(R.drawable.gem),
                                    contentDescription = "TON",
                                    modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(text = text, color = color ?: Color.Black, style = style)
                        }
                        if (subText != null) {
                            Text(text = subText, color = subColor ?: Color.Gray, style = subStyle)
                        }
                    }


                    Spacer(modifier = Modifier.weight(1f))

                    if (gemIcon) {
                        Image(painter = painterResource(R.drawable.gem),
                            contentDescription = "TON",
                            modifier = Modifier.size(18.dp))
                    }

                    if (value != null) {
                        Text(text = value, color = valueColor ?: Colors.Primary, style = valueStyle,
                            modifier = Modifier.padding(4.dp, 14.dp, 2.dp, 14.dp))
                        if (toggle != null)
                            Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (toggle != null) {
                        Box(modifier = Modifier.padding(0.dp, 14.dp, 0.dp,
                            if (value != null) 13.dp else 14.dp)) {
                            TonToggle(value = toggle, disabled = !enabled) { onClick?.invoke(text) }
                        }
                    }

                    if (progIcon) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Colors.Primary
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))
                }
                if (!last)
                    Spacer(modifier = Modifier
                        .height(0.5.dp)
                        .fillMaxWidth()
                        .background(Colors.TextInactive))
            }
        }
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    QuarkTONWalletTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier
                .height(10.dp)
                .fillMaxWidth()
                .background(Color.LightGray))

            UniversalItem(header = "General")
            UniversalItem(text = "Notifications", toggle = true)
            UniversalItem(text = "Active address", value = "v4R2")
            UniversalItem(text = "Primary currency", value = "USD")
            UniversalItem(text = "List of tokens", last = true)

            UniversalItem(header = "Security")
            UniversalItem(text = "Show recovery phrase")
            UniversalItem(text = "Change passcode")
            UniversalItem(text = "Biometric Auth", toggle = false)
            UniversalItem(text = "Delete Wallet", color = Color.Red, last = true)

            Spacer(modifier = Modifier
                .height(10.dp)
                .fillMaxWidth()
                .background(Color.LightGray))
        }
    }
}

private class NullRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(
        draggedAlpha = 0f,
        focusedAlpha = 0f,
        hoveredAlpha = 0f,
        pressedAlpha = 0f,
    )
}