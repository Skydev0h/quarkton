package app.quarkton.ui.elements.frag

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.db.TransItem
import app.quarkton.db.createMockTransaction
import app.quarkton.extensions.breakMiddle
import app.quarkton.extensions.formatBalance
import app.quarkton.extensions.shortAddr
import app.quarkton.extensions.simpleBalance
import app.quarkton.extensions.vibrateLongPress
import app.quarkton.ton.extensions.ZERO_TX
import app.quarkton.ton.makeExplorerLink
import app.quarkton.ui.elements.Lottie
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ton.bitstring.BitString
import org.ton.crypto.encoding.base64url
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionFrag(
    transaction: TransItem,
    isIncoming: Boolean,
    address: String,
    amount: Long,
    comment: String?,
    resolveAddr: suspend ((String) -> String?),
    sendClicked: (() -> Unit)?,
    heightPct: Float = 1f,
) {
    val cm = LocalClipboardManager.current
    val lc = LocalContext.current
    val view = LocalView.current
    fun copy(s: String) {
        cm.setText(AnnotatedString(s))
        Toast
            .makeText(lc, "Copied to clipboard", Toast.LENGTH_SHORT)
            .show()
    }
    val fullFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy @ HH:mm")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault()) }
    val thisInst = remember { Instant.ofEpochSecond(transaction.now) }
    val dateTimeAtSep = stringResource(R.string.date_time_at_sep)
    val thisDate = remember { fullFormatter.format(thisInst).replace("@", dateTimeAtSep) }
    val balance = remember(amount) { amount.formatBalance() }
    val failed = remember { !transaction.actOk or !transaction.compOk }
    val humantran = remember {
        if (!transaction.id.contains("@")) "" else
        base64url(BitString(transaction.id.split("@")[0]).toByteArray())
    }
    val animHeight by animateFloatAsState(targetValue = heightPct, label = "Height", animationSpec = tween(500))
    var dns by remember { mutableStateOf<String?>(null) }
    val crs = rememberCoroutineScope()

    var expandAddress by remember { mutableStateOf(false) }
    var expandTransaction by remember { mutableStateOf(false) }

    LaunchedEffect(address) {
        crs.launch {
            with (Dispatchers.IO) {
                dns = resolveAddr(address)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        BoxWithConstraints {
            Surface(
                color = Color.White, shape = Styles.largePanelShapeTop,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            0,
                            (maxHeight * (1f - animHeight))
                                .toPx()
                                .toInt()
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.transaction), style = Styles.cardLabel,
                        color = Color.Black, modifier = Modifier
                            .padding(20.dp, 16.dp, 0.dp, 16.dp)
                            .align(Alignment.Start)
                    )

                    Row(modifier = Modifier.height(56.dp)) {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Lottie(
                                modifier = Modifier.size(44.dp),
                                // imageId = R.drawable.ph_main,
                                lottieId = R.raw.main,
                                iterations = 3
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = balance, color = if (isIncoming) Colors.BalGreen
                            else Colors.BalRed, style = Styles.bigBalanceText
                        )
                    }

                    Text(
                        text = stringResource(R.string.transaction_fee, transaction.totalFee.simpleBalance(9)),
                        style = Styles.transSubText,
                        color = Colors.Gray,
                        modifier = Modifier.padding(4.dp),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = thisDate,
                        style = Styles.transSubText,
                        color = Colors.Gray,
                        textAlign = TextAlign.Center
                    )

                    // ZERO_TX transactions are pending/failed send ones not yet in BC
                    // Therefore they can be CANCELLED, other TXs in BC are FAILED
                    // Also if TX has ID = ZERO_TX it is not yet sent and is PENDING
                    if (failed) {
                        Text(
                            text = stringResource(if (transaction.id == ZERO_TX)
                                R.string.cancelled else R.string.failed),
                            style = Styles.transSubText,
                            color = Colors.BalRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    else if (transaction.id == ZERO_TX) {
                        Row(modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.dp,
                                color = Colors.Primary
                            )
                            Text(
                                text = stringResource(R.string.pending),
                                style = Styles.transSubText,
                                color = Colors.Primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    if (comment != null) {
                        Surface(
                            color = Colors.BackGray,
                            shape = Styles.commentShape,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = comment,
                                style = Styles.mainText,
                                color = Color.Black,
                                modifier = Modifier.padding(12.dp, 10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    UniversalItem(header = stringResource(R.string.details))

                    if (dns != null) {
                        UniversalItem(text = stringResource(if (isIncoming)
                            R.string.sender else R.string.recipient),
                            valueColor = Color.Black, valueStyle = Styles.mainText,
                            value = dns)
                        { copy(dns!!) }
                    }

                    UniversalItem(text = stringResource(if (isIncoming)
                        R.string.sender_address else R.string.recipient_address),
                        valueColor = Color.Black,
                        valueStyle = if (expandAddress) Styles.smallAddress else Styles.address,
                        value = if (expandAddress) address.breakMiddle() else address.shortAddr(),
                        last = transaction.id == ZERO_TX,
                        onLongClick = {
                            view.vibrateLongPress()
                            copy(address)
                        })
                    { expandAddress = !expandAddress }

                    if (transaction.id != ZERO_TX) {
                        UniversalItem(text = stringResource(R.string.transaction),
                            valueColor = Color.Black,
                            valueStyle = if (expandTransaction) Styles.smallAddress else Styles.mainText,
                            value = if (expandTransaction) humantran.breakMiddle() else
                            humantran.let {
                                if (it.length <= 12) it else
                                    it.substring(0, 6) + "…" + it.substring(it.length - 6)
                            },
                            onLongClick = {
                                view.vibrateLongPress()
                                copy(humantran)
                            })
                        { expandTransaction = !expandTransaction }


                        val uriHandler = LocalUriHandler.current
                        UniversalItem(
                            text = stringResource(R.string.view_in_explorer),
                            color = Colors.Primary, last = true
                        )
                        { uriHandler.openUri(makeExplorerLink(transaction = humantran)) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)) {
                        Button(
                            onClick = {
                                sendClicked?.invoke()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Styles.buttonHeight),
                            shape = Styles.buttonShape
                        ) {
                            Text(
                                text = stringResource(if (failed and !isIncoming)
                                    R.string.retry_to_send_ton_to_this_address
                                else
                                    R.string.send_ton_to_this_address),
                                style = Styles.buttonLabel
                            )
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
    val tx = createMockTransaction(1613081906)
    QuarkTONWalletTheme {
        Overlay(visible = true, darker = true) {
            TransactionFrag(tx, true, tx.src!!, tx.inamt!!, tx.incmt!!, { "andrew.ton" }, {})
        }
    }
}

@Preview
@Composable
private fun PreviewFailed() {
    val tx = createMockTransaction(1613081906, false, false)
    QuarkTONWalletTheme {
        Overlay(visible = true, darker = true) {
            TransactionFrag(tx, false, tx.dst!!, tx.amt!!, tx.cmt!!, { "andrew.ton" }, {})
        }
    }
}