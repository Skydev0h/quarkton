package app.quarkton.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles

@Composable
fun JumboButtons(
    mainText: String,
    mainClicked: () -> Unit,
    secText: String? = null,
    secClicked: (() -> Unit)? = null,
    mainWidth: Int = 200,
    secWidth: Int = 200,
    topSpacing: Int = 48,
    bottomSpacing: Int = -1,
    mainColor: Color = Colors.Primary,
    secColor: Color = Colors.Primary
) {
    Spacer(modifier = Modifier.height(topSpacing.dp))
    Button(
        onClick = mainClicked,
        modifier = Modifier
            .widthIn(mainWidth.dp)
            .height(Styles.buttonHeight),
        shape = Styles.buttonShape,
        colors = ButtonDefaults.buttonColors(mainColor)
    ) {
        Text(
            text = mainText, style = Styles.buttonLabel
        )
    }
    if (secText != null && secClicked != null) {
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = secClicked,
            modifier = Modifier
                .widthIn(secWidth.dp)
                .height(Styles.buttonHeight),
            shape = Styles.buttonShape
        ) {
            Text(
                text = secText, style = Styles.textButtonLabel, color = secColor
            )
        }
    } else if (bottomSpacing == -1) {
        Spacer(Modifier.height(Styles.buttonHeight + 8.dp))
    }
    Spacer(Modifier.height(if (bottomSpacing == -1) 44.dp else bottomSpacing.dp))
}

@Preview
@Composable
private fun Preview() {
    QuarkTONWalletTheme {
        Column {
            JumboButtons(
                mainText = stringResource(R.string.create_my_wallet),
                mainClicked = {},
                secText = stringResource(R.string.import_existing_wallet),
                secClicked = {})
        }
    }
}