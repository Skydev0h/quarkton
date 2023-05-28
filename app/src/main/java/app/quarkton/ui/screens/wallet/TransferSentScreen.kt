package app.quarkton.ui.screens.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.breakMiddle
import app.quarkton.extensions.simpleBalance
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.theme.Styles
import com.airbnb.lottie.compose.LottieConstants

class TransferSentScreen : BaseWalletScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)

        val isSending by dm.isSending

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Surface(
                color = Color.White, shape = Styles.largePanelShapeTop,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { nav?.pop() }, modifier = Modifier.padding(4.dp)) {
                            Icon(
                                tint = Color.Black,
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    JumboTemplate(
                        balloon = false, shrink = true,
                        lottieId = if (isSending) R.raw.waiting_ton else R.raw.success,
                        lottieIterations = if (isSending) LottieConstants.IterateForever else 1,
                        titleText = stringResource(if (isSending) R.string.sending_ton else R.string.done),
                        mainText = if (isSending) stringResource(id = R.string.please_wait_seconds)
                            else stringResource(R.string.toncoin_have_been_sent_to, mdl.sendingAmount.simpleBalance(9))
                    ) {
                        if (!isSending) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = mdl.sendingToAddress.breakMiddle(),
                                style = Styles.address
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp, 16.dp, 16.dp)) {
                        // *************************************************************************
                        Button(
                            onClick = {
                                nav?.pop()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Styles.buttonHeight),
                            shape = Styles.buttonShape
                        ) {
                            Text(
                                text = stringResource(R.string.view_my_wallet),
                                style = Styles.buttonLabel,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // *************************************************************************
                    }
                }
            }
        }
    }
}