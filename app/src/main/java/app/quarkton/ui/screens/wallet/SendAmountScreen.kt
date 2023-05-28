package app.quarkton.ui.screens.wallet

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.formatBalance
import app.quarkton.extensions.fromBalance
import app.quarkton.extensions.shortAddr
import app.quarkton.extensions.simpleBalance
import app.quarkton.extensions.vibrateError
import app.quarkton.extensions.vrStr
import app.quarkton.ui.elements.BackButton
import app.quarkton.ui.elements.Keypad
import app.quarkton.ui.elements.Lottie
import app.quarkton.ui.elements.TonToggle
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import kotlinx.coroutines.launch

class SendAmountScreen : BaseWalletScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)
        val view = LocalView.current

        var amountText by remember { mutableStateOf(mdl.sendingAmount
            .simpleBalance(9).replace("0.","#").trimStart('0').replace("#","0.")) }
        val amountFmt by remember { derivedStateOf { amountText.formatBalance() } }

        val wallet by db.walletDao().observeCurrent().observeAsState()
        val txtaddr = remember { mdl.sendingToAddress.shortAddr() }
        var dns by remember { mutableStateOf("") }
        val balance = remember(wallet?.balance) { wallet?.balance?.simpleBalance() ?: "" }
        var sendAllBalance by remember { mutableStateOf(mdl.sendAllBalance) }
        var error by remember { mutableStateOf(false) }

        val isRefreshing by dm.isRefreshing

        LaunchedEffect(sendAllBalance, wallet?.balance) {
            if (sendAllBalance)
                amountText = wallet?.balance?.simpleBalance() ?: ""
        }

        LaunchedEffect(amountText, wallet?.balance) {
            val bal = wallet?.balance
            error = if (bal == null) true else (Long.fromBalance(amountText) > bal)
        }

        val infiniteTransition = rememberInfiniteTransition(label = "blink")
        val infinitelyAnimatedFloat by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                // The value will infinitely repeat from 0 to 1 and 1 to 0
                repeatMode = RepeatMode.Restart
            ), label = "blink"
        )
        val cursorAlpha by remember { derivedStateOf {
            if (infinitelyAnimatedFloat > 0.5f) 1f else 0f } }

        LifecycleEffect(onStarted = {
            crs?.launch {
                val mywal = db.walletDao().getByAddress(mdl.sendingToAddress)?.verRev ?: 0
                dns = if (mywal != 0)
                    "(My ${mywal.vrStr(true)})"
                else
                    db.nameDao().getByAddress(mdl.sendingToAddress)?.name ?: ""
            }
        })

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Surface(
                color = Color.White, shape = Styles.largePanelShapeTop,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxWidth())
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BackButton()
                            Spacer(modifier = Modifier.width(20.dp).height(56.dp))
                            Text(
                                text = stringResource(R.string.send_ton),
                                style = Styles.cardLabel,
                                color = Color.Black
                            )
                        }
                        Row(
                            modifier = Modifier.height(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        )
                        {
                            Spacer(modifier = Modifier.width(20.dp))
                            Text(
                                text = stringResource(R.string.send_to) + " ",
                                style = Styles.mainText, color = Colors.Gray
                            )
                            Text(text = txtaddr, style = Styles.mainText, color = Color.Black)
                            if (dns != "") {
                                Text(text = " $dns", style = Styles.mainText, color = Colors.Gray)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { nav?.pop() },
                                modifier = Modifier.height(36.dp),
                                shape = Styles.buttonShape
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_edit),
                                    style = Styles.mainText, color = Colors.Primary
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Lottie(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .offset(0.dp, (-2).dp),
                                    // imageId = R.drawable.ph_main,
                                    lottieId = R.raw.main,
                                    iterations = 3
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = amountFmt,
                                    color = if (error) Colors.BalRed else Color.Black,
                                    style = Styles.bigBalanceText
                                )
                                Surface(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .width(1.5.dp)
                                        .graphicsLayer { alpha = cursorAlpha },
                                    shape = RoundedCornerShape(2.dp),
                                    color = Colors.Primary
                                ) {}
                                if (amountText == "") {
                                    Text(
                                        text = "0",
                                        color = Colors.Gray,
                                        style = Styles.bigBalanceText
                                    )
                                }
                            }
                            Text(text = stringResource(R.string.insufficient_funds),
                                style = Styles.transSubText, color = Colors.BalRed,
                                modifier = Modifier.graphicsLayer { alpha = if (error) 1f else 0f })
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            modifier = Modifier.height(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        )
                        {
                            Spacer(modifier = Modifier.width(20.dp))
                            Text(
                                text = stringResource(R.string.send_all),
                                style = Styles.mainText, color = Color.Black
                            )
                            TextButton(
                                onClick = { dm.refreshCurrentWallet() },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(4.dp),
                                shape = Styles.buttonShape
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.gem),
                                    contentDescription = "TON",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = " $balance",
                                    style = Styles.mainText, color = Color.Black
                                )
                            }
                            if (isRefreshing) {
                                Spacer(modifier = Modifier.width(2.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Colors.Primary
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TonToggle(value = sendAllBalance, onToggle = {
                                sendAllBalance = !sendAllBalance
//                                if (sendAllBalance) {
//                                    amountText = wallet?.balance?.simpleBalance() ?: ""
//                                }
                            })
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 8.dp, 16.dp, 16.dp)) {
                            // *************************************************************************
                            Button(
                                onClick = {
                                    mdl.sendingAmount = Long.fromBalance(amountText)
                                    if (error or (mdl.sendingAmount == 0L)) {
                                        view.vibrateError()
                                        return@Button
                                    }
                                    mdl.sendAllBalance = sendAllBalance
                                    nav?.push(SendCommentScreen())
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Styles.buttonHeight),
                                shape = Styles.buttonShape
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_continue),
                                    style = Styles.buttonLabel,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // *************************************************************************
                        }
                        Keypad(onPressed = {
                            if (it == '<') {
                                if (amountText.isNotEmpty()) {
                                    sendAllBalance = false
                                    amountText = amountText.substring(0, amountText.length - 1)
                                }
                                return@Keypad
                            }
                            if (it == '.') {
                                if (amountText == "") {
                                    sendAllBalance = false
                                    amountText = "0."
                                }
                                else if (!amountText.contains('.')) {
                                    sendAllBalance = false
                                    amountText += '.'
                                }
                                return@Keypad
                            }
                            val d = amountText.indexOf('.')
                            if ((d > -1) and (amountText.length - d > 9))
                                return@Keypad
                            val nt = (amountText + it).replace("0.","#")
                                .trimStart('0').replace("#","0.")
                            try {
                                Long.fromBalance(nt) // Too long string that crashes String->Long
                                sendAllBalance = false
                                amountText = nt
                            } catch (_: Throwable) {}
                        }, dot = true)
                    }
                }
            }
        }
    }
}