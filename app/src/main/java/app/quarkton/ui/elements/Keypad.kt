package app.quarkton.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.vibrateKeyPress
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles

@Composable
fun Keypad(
    onPressed: (Char) -> Unit,
    dark: Boolean = false,
    fp: Boolean = false,
    dot: Boolean = false
) {
    val backColor = if (!dark) Color.White else Color.Black
    val textColor = if (!dark) Color.Black else Color.White
    val buttonColor = if (!dark) Colors.LightPadColor else Colors.DarkPadColor
    val hintText = Colors.AlphaPadColor
    var numbers = "123456789~0<"
    if (dot) numbers = numbers.replace('~', '.')
    val hints = remember { " ABC DEF GHI JKL MNO PQRS TUV WXYZ  + ".split(" ") }
    val view = LocalView.current
    // TODO: Larger buttons for keypad (fill more part of screen when possible)
    // Use AspectRatio maybe?
    Surface(color = backColor, modifier = Modifier.fillMaxWidth()) {
        Column (modifier = Modifier.fillMaxWidth()) {
            var i = -1
            for (r in 0..3) {
                Row() {
                    Spacer(modifier = Modifier.width(10.dp))
                    for (c in 0..2) {
                        i += 1
                        val n = numbers[i]
                        Surface(color = if (n != ' ') buttonColor else backColor,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                        ) {
                            if ((n != '~' || fp) and (n != '.' || dot)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        view.vibrateKeyPress()
                                        onPressed(n)
                                    }) {
                                    if (n != '<' && n != '~') {
                                        Text(
                                            text = n.toString(), style = Styles.keyPadNumber,
                                            color = textColor, textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(0.dp, 8.dp)
                                        )
                                        if (n != '.') {
                                            Text(
                                                text = hints[i], style = Styles.keyPadHints,
                                                color = hintText, modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.width(9.dp))
                                        }
                                    } else {
                                        Text( // Vertical size
                                            text = " ", style = Styles.keyPadNumber,
                                            modifier = Modifier
                                                .width(0.dp)
                                                .padding(0.dp, 8.dp)
                                        )
                                        Icon(painter = painterResource(id =
                                        if (n == '<')
                                            R.drawable.backspace
                                        else
                                            R.drawable.ic_fp
                                        ),
                                            contentDescription = "Backspace",
                                            modifier = Modifier.weight(1f).then(
                                                if (n == '~') Modifier.height(34.dp) else Modifier
                                            ),
                                            tint = textColor
                                        )
                                    }
                                }
                            }
                        }
                        if (c < 2)
                            Spacer(modifier = Modifier.width(6.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                if (r < 3)
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp))
            }
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(15.dp))
        }
    }
}

@Preview
@Composable
private fun LightKeypad() {
    QuarkTONWalletTheme {
        Keypad(onPressed = {}, false)
    }
}

@Preview
@Composable
private fun LightKeypadDot() {
    QuarkTONWalletTheme {
        Keypad(onPressed = {}, false, dot = true)
    }
}

@Preview
@Composable
private fun DarkKeypad() {
    QuarkTONWalletTheme {
        Keypad(onPressed = {}, true)
    }
}

@Preview
@Composable
private fun DarkKeypadFP() {
    QuarkTONWalletTheme {
        Keypad(onPressed = {}, true, true)
    }
}