package app.quarkton.ui.elements.frag.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.ui.elements.Lottie
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

@Composable
fun WalletTopPanelFrag(
    address: String,
    balance: AnnotatedString,
    hasBalance: Boolean,
    showLottie: Boolean,
    currBalance: String,
    alpha: () -> Float,
    receiveClicked: () -> Unit,
    sendClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(0.75f))
        Text(
            text = address,
            color = Color.White,
            style = Styles.shortAddress,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { this.alpha = 1f - alpha() },
            textAlign = TextAlign.Center
        )
        Row(modifier = Modifier
            .height(56.dp)
            .graphicsLayer { this.alpha = 1f - alpha() }) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (showLottie)
                    Lottie(
                        modifier = Modifier.size(44.dp),
                        // imageId = R.drawable.ph_main,
                        lottieId = R.raw.main,
                        iterations = 3
                    )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = balance, color = Color.White, style = Styles.bigBalanceText
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        if (hasBalance)
            Row(modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { this.alpha = 1f - alpha() }) {
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "≈", color = Colors.Gray, style = Styles.topBalanceSubText,
                    modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                Text(text = currBalance, color = Colors.Gray, style = Styles.topBalanceSubText)
                Spacer(modifier = Modifier.weight(1f))
            }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = receiveClicked,
                modifier = Modifier
                    .weight(1f)
                    .height(Styles.buttonHeight),
                shape = Styles.buttonShape
            ) {
                Icon(
                    tint = Color.White,
                    painter = painterResource(id = R.drawable.ic_receive),
                    contentDescription = "Receive"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.receive),
                    style = Styles.buttonLabel
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = sendClicked,
                modifier = Modifier
                    .weight(1f)
                    .height(Styles.buttonHeight),
                shape = Styles.buttonShape
            ) {
                Icon(
                    tint = Color.White,
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = "Send"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.send),
                    style = Styles.buttonLabel
                )
            }
        }
    }
}