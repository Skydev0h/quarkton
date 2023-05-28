package app.quarkton.ui.screens.wallet

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.quarkton.R
import app.quarkton.extensions.delNL
import app.quarkton.extensions.shortAddr
import app.quarkton.extensions.simpleBalance
import app.quarkton.extensions.vibrateError
import app.quarkton.extensions.vibrateKeyPress
import app.quarkton.extensions.vibrateLongPress
import app.quarkton.extensions.vrStr
import app.quarkton.ton.Wallet
import app.quarkton.ui.elements.TextFieldPad
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.launch
import org.ton.block.AddrStd
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

class SendAddressScreen : BaseWalletScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        Init(dark = true)
        val view = LocalView.current

        var text by remember { mutableStateOf(TextFieldValue(mdl.sendingToAddress)) } // Photocopy. Cheese!
        val lcm = LocalClipboardManager.current

        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf(false) }

        val textFieldColors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedIndicatorColor = Colors.TextInactive,
            focusedIndicatorColor = Colors.Primary,
            disabledContainerColor = Color.White,
            disabledIndicatorColor = Colors.TextInactive
        )

        val wallet by db.walletDao().observeCurrent().observeAsState()
        val txs by (remember(wallet?.address)
        { db.transDao().observeByAccount(wallet?.address ?: "") })
            .observeAsState()
        // NOTE: It is possible to get transactions from all accounts if needed

        val dateFmt = remember {
            DateTimeFormatter.ofPattern("MMMM d")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
        }

        val showmywalre = remember { per.showMyInRecents }

        val wallets = db.walletDao().observeAll().observeAsState()
        val actwals0 = remember { derivedStateOf { wallets.value?.filter {
            (it.verRev >= 0) and !Wallet.vrdeprecated(it.verRev) } } }
        val depwals0 = remember { derivedStateOf { wallets.value?.filter {
            (it.verRev >= 0) and Wallet.vrdeprecated(it.verRev) } } }

        val actwals1 = remember { derivedStateOf { wallets.value?.filter {
            (it.verRev < 0) and !Wallet.vrdeprecated(it.verRev) } } }
        val depwals1 = remember { derivedStateOf { wallets.value?.filter {
            (it.verRev < 0) and Wallet.vrdeprecated(it.verRev) } } }

        val recents by remember {
            derivedStateOf {
                ((txs ?: listOf()).map {
                    listOf(
                        Pair(it.dst, it.now), Pair(it.dst1, it.now),
                        Pair(it.dst2, it.now), Pair(it.dst3, it.now), Pair(it.src, it.now)
                    )
                }
                    .flatten()
                    .filter { it.first != null }
                    .distinctBy { it.first }
                    .apply { subList(0, min(15, size)) }
                    .map { Pair(it.first!!, dateFmt.format(Instant.ofEpochSecond(it.second))) }) +
                    (if (showmywalre)
                        ((actwals0.value ?: listOf()) + (depwals0.value ?: listOf()) +
                         (actwals1.value ?: listOf()) + (depwals1.value ?: listOf()))
                            .map{ Pair(it.address,
                                (if (Wallet.vrdeprecated(it.verRev)) "\uD83D\uDDDD" else "\uD83D\uDD11") +
                                " " + (if (it.address == wallet?.address) "Current" else "My") +
                                " Wallet " + it.verRev.vrStr(true) +
                            " (\uD83D\uDC8E ${it.balance.simpleBalance()})") }
                    else listOf())
            }
        }

        val keyboardController = LocalSoftwareKeyboardController.current
        fun submit() {
            if (loading or error) return
            keyboardController?.hide()
            loading = true
            // Must be parsable by AddrStd OR be successfully found in DNS
            // Do NOT use DNS cache for this kind of lookup!!!
            // Address MIGHT change since last DNS lookup, so always recheck!
            try {
                AddrStd.parse(text.text)
                // If address changed from provisioned one (from QR code)
                // then reset amount and comment for safety!
                if (mdl.sendingToAddress != text.text)
                    mdl.sendReset()
                mdl.sendingToAddress = text.text
                loading = false
                nav?.push(SendAmountScreen())
                return
            } catch (_: Throwable) {
            }
            if (text.text.endsWith(".ton")) {
                crs?.launch {
                    val addr = dm.dnsLookup(text.text.lowercase())
                    if (addr == null) {
                        loading = false
                        error = true
                        view.vibrateError()
                        return@launch
                    }
                    mdl.sendReset()
                    mdl.sendingToAddress = addr
                    loading = false
                    nav?.push(SendAmountScreen())
                }
            } else {
                loading = false
                error = true
                view.vibrateError()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Surface(
                color = Color.White, shape = Styles.largePanelShapeTop,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 16.dp, 16.dp, 0.dp)
                    )
                    {
                        Text(
                            text = stringResource(R.string.send_ton), style = Styles.cardLabel,
                            color = Color.Black, modifier = Modifier.padding(bottom = 24.dp)
                        )
                        Text(
                            text = stringResource(R.string.wallet_address_or_domain),
                            style = Styles.primlabel, color = Colors.Primary
                        )
                        TextFieldPad(
                            value = text, onValueChange = {
                                error = false
                                text = it.copy(it.text.delNL())
                                if (it.text.contains("\n")) {
                                    // Enter pressed
                                    submit()
                                }
                            },
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp, 12.dp),
                            minHeight = 44.dp,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.enter_wallet_address_or_domain),
                                    style = Styles.mainText, color = Colors.Gray
                                )
                            },
                            enabled = !loading
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.paste_send_hint),
                            style = Styles.smallHint, color = Colors.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!loading) {
                            Row(modifier = Modifier.offset((-12).dp, 0.dp)) {
                                TextButton(
                                    onClick = {
                                        view.vibrateLongPress()
                                        error = false
                                        lcm.getText()
                                            ?.let { text = text.copy(it.toString().delNL()) }
                                    },
                                    modifier = Modifier.height(Styles.smallButtonHeight),
                                    shape = Styles.smallButtonShape
                                ) {
                                    Icon(
                                        tint = Colors.Primary,
                                        painter = painterResource(id = R.drawable.ic_mini_paste),
                                        contentDescription = "Paste"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.btn_paste),
                                        style = Styles.smallButtonText, color = Colors.Primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        error = false
                                        mdl.sendingToAddress = text.text
                                        mdl.qrFromAddrScr = true
                                        nav?.push(QRScanScreen())
                                    },
                                    modifier = Modifier.height(Styles.smallButtonHeight),
                                    shape = Styles.smallButtonShape
                                ) {
                                    Icon(
                                        tint = Colors.Primary,
                                        painter = painterResource(id = R.drawable.ic_mini_scan),
                                        contentDescription = "Scan"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.btn_scan),
                                        style = Styles.smallButtonText, color = Colors.Primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        view.vibrateLongPress()
                                        error = false
                                        text = text.copy("")
                                    },
                                    modifier = Modifier.height(Styles.smallButtonHeight),
                                    shape = Styles.smallButtonShape
                                ) {
                                    Icon(
                                        tint = Colors.Primary,
                                        painter = painterResource(id = R.drawable.ic_mini_clear),
                                        contentDescription = "Clear"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.btn_clear),
                                        style = Styles.smallButtonText, color = Colors.Primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(if (showmywalre)
                                    R.string.recents_and_my_wallets else R.string.recents),
                                style = Styles.smallCategory, color = Colors.Primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(if (loading) listOf() else recents) { item ->
                            val a = item.first
                            val d = item.second
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    view.vibrateKeyPress()
                                    error = false
                                    text = text.copy(a)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White
                            )
                            {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp, 0.dp)
                                )
                                {
                                    Spacer(modifier = Modifier.height(13.dp))
                                    Text(
                                        text = a.shortAddr(),
                                        color = Color.Black,
                                        style = Styles.address,
                                        modifier = Modifier.padding(16.dp, 0.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = d,
                                        color = Colors.Gray,
                                        style = Styles.recentsSubText,
                                        modifier = Modifier.padding(16.dp, 0.dp)
                                    )
                                    Spacer(modifier = Modifier.height(11.dp))
                                    Spacer(
                                        modifier = Modifier
                                            .height(0.5.dp)
                                            .fillMaxWidth()
                                            .background(Colors.TextInactive)
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (error) {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp, 0.dp, 8.dp, 0.dp)
                                    .zIndex(5f)
                            ) {
                                Surface(color = Colors.BackBox, shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .offset(0.dp, 12.dp),
                                    onClick = { error = false }) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            tint = Color.White,
                                            painter = painterResource(id = R.drawable.ic_warning),
                                            contentDescription = "Warning",
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.invalid_address),
                                                color = Color.White, style = Styles.popupBoxTitle
                                            )
                                            Text(
                                                text = stringResource(R.string.address_entered_not_ton),
                                                color = Color.White, style = Styles.popupBoxText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.padding(16.dp)) {
                            // *********************************************************************
                            Button(
                                onClick = ::submit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Styles.buttonHeight),
                                shape = Styles.buttonShape
                            ) {
                                if (loading) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Text(
                                    text = stringResource(R.string.btn_continue),
                                    style = Styles.buttonLabel,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }
                            }
                            // *********************************************************************
                        }
                    }
                }
            }
        }
    }
}