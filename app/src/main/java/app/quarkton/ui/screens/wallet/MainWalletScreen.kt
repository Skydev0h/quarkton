package app.quarkton.ui.screens.wallet

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.db.createMockTransaction
import app.quarkton.extensions.appendStyled
import app.quarkton.extensions.crs
import app.quarkton.extensions.formatBalance
import app.quarkton.extensions.shortAddr
import app.quarkton.extensions.simpleBalance
import app.quarkton.extensions.vibrateError
import app.quarkton.extensions.vibrateLongPress
import app.quarkton.extensions.vrStr
import app.quarkton.extensions.with
import app.quarkton.ton.DataMaster
import app.quarkton.ton.Wallet
import app.quarkton.ton.extensions.ZERO_TX
import app.quarkton.ton.makeExplorerLink
import app.quarkton.ton.nowms
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.elements.TransRow
import app.quarkton.ui.elements.frag.ReceiveFrag
import app.quarkton.ui.elements.frag.TransactionFrag
import app.quarkton.ui.elements.frag.tonconnect.TCConnectFrag
import app.quarkton.ui.elements.frag.tonconnect.TCTransferFrag
import app.quarkton.ui.elements.frag.wallet.WalletCreatedFrag
import app.quarkton.ui.elements.frag.wallet.WalletLoadingFrag
import app.quarkton.ui.elements.frag.wallet.WalletTopPanelFrag
import app.quarkton.ui.screens.other.LockScreen
import app.quarkton.ui.screens.settings.SettingsScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import app.quarkton.utils.ConnectionState
import app.quarkton.utils.connectivityState
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow


@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
class MainWalletScreen : BaseWalletScreen() {

    @Transient
    private lateinit var m: MainWalletModel

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    fun lockClicked() {
        mdl.justLocking = true
        nav?.replaceAll(LockScreen())
    }

    fun scanClicked() {
        mdl.qrFromAddrScr = false
        nav?.push(QRScanScreen())
    }

    fun settingsClicked() {
        nav?.push(SettingsScreen())
    }

    fun receiveClicked() {
        m.showReceive()
    }

    fun sendClicked() {
        mdl.sendReset()
        nav?.push(SendAddressScreen())
    }

    fun transactionSendClicked() {
        mdl.sendReset()
        mdl.sendingToAddress = m.stxAddress
        nav?.push(SendAddressScreen())
    }

    @Composable
    override fun Content() {
        Init(dark = true)
        m = rememberScreenModel { MainWalletModel() }
        val view = LocalView.current

        val scrollPct by remember { derivedStateOf { m.scrollState.value / (m.scrollState.maxValue.toFloat() + 0.001f) } }
        val alpha by remember { derivedStateOf { clamp((scrollPct - 0.25f) / 0.15f, 0f, 1f) } }
        val showLottie by remember { derivedStateOf { alpha < 0.99f } }

        val isConnected by dm.isConnected
        val isRefreshing by dm.isRefreshing

        val uriHandler = LocalUriHandler.current

        var preload by remember { mutableStateOf(true) }

        val currRate by remember(per.selectedCurrency) {
            db.rateDao().observe(per.selectedCurrency)
        }.observeAsState()

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    if (m.scrollState.canScrollForward && delta < 0) {
                        crs?.launch {
                            m.scrollState.scroll { this.scrollBy(-delta) }
                        }
                        return available
                    }
                    return Offset.Zero
                }
            }
        }

        val connectionState by connectivityState()
        val networkConnected by remember { derivedStateOf {
            connectionState === ConnectionState.Available } }

        LifecycleEffect(onStarted = {
            crs?.launch {
                withContext(Dispatchers.Default) {
                    delay(1000)
                    preload = false
                }
            }
            if (mdl.getOverToSendNow) {
                mdl.getOverToSendNow = false
                nav?.push(SendAddressScreen())
            } else {
                if (mdl.sendingToAddress != m.stxAddress)
                    mdl.sendReset()
            }
        })

        LaunchedEffect(networkConnected) {
            if (networkConnected) {
                dm.maybeRefreshCurrentWallet()
                withContext(Dispatchers.IO) {
                    val ismc = dm.getCurrentVerRev() < 0
                    if (db.walletDao().getAll().count { if (ismc) it.verRev < 0 else it.verRev >= 0 } < Wallet.useWallets.size)
                        dm.refreshOtherWallets(workchainId = if (ismc) -1 else 0, multi = 5, offset = 5)
                        // Refresh other wallets in background, so when user will want to switch they most likely will already be refreshed
                }
            }
        }

        val refreshState = rememberPullRefreshState(isRefreshing || !networkConnected, {
            dm.refreshCurrentWallet()
        })
        val pulld = (refreshState.progress >= 1f) || isRefreshing

        val wallet by db.walletDao().observeCurrent().observeAsState()
        val address = remember(wallet?.address) {
            wallet?.address?.shortAddr() ?: ""
        }
        val balance = remember(wallet?.balance) {
            wallet?.balance?.formatBalance() ?: AnnotatedString("")
        }
        val dapp by db.dappDao().observeCurrent().observeAsState()
        val dappName by remember { derivedStateOf { dapp?.name } }
        val tcClosed by remember { derivedStateOf { dapp?.closed ?: false } }
        val tcConnected by dm.tcIsConnected
        val tcConnecting by dm.tcIsConnecting
        val tcCurrentApp by dm.tcCurrentApp
        val tcShowBar by remember { derivedStateOf { (dappName ?: "") != "" } }
        val tcWantConnected by dm.tcWantConnected

        val tcPending by mdl.tcPendingItem.collectAsStateWithLifecycle()
        val tcRequest by mdl.tcPendingCR.collectAsStateWithLifecycle()

        var tcConnectLoading by remember { mutableStateOf(false) }
        var tcConnectSuccess by remember { mutableStateOf(false) }
        var tcTransferLoading by remember { mutableStateOf(false) }
        var tcTransferSuccess by remember { mutableStateOf(false) }

        LaunchedEffect(tcRequest) {
            val (id, req) = tcRequest ?: return@LaunchedEffect
            crs?.launch(Dispatchers.IO) {
                try {
                    val (item, icrs) = dm.parseInitialRequest(id, req)
                    mdl.tcPendingItem.value = item
                    mdl.tcWantedCItems = icrs
                    m.showTCConnect()
                } catch (e: Throwable) {
                    Log.e("MainWalletScreen", "TC processing failed", e)
                    Toast.makeText(act, R.string.failed_processing_tc_request, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        val currBalance = remember(wallet?.balance, currRate?.rate, per.selectedCurrency) {
            DataMaster.currencies[per.selectedCurrency]?.first?.replace("~",
                String.format("%.2f",
                    (wallet?.balance?.simpleBalance(9)?.toDouble() ?: 0.0) *
                    (currRate?.rate ?: 0.0))) ?: ""
        }

        val txs by (remember(wallet?.address)
        { db.transDao().observeByAccount(wallet?.address ?: "") }).observeAsState()
        val ntxs by remember { derivedStateOf { txs?.size ?: 0 } }
        val ftxid by remember { derivedStateOf { if (txs?.isEmpty() != false) "" else txs!!.first().id } }

        LaunchedEffect(ftxid) {
            // On change of amount of transactions scroll up
            if (mdl.sendingToAddress == m.stxAddress) {
                if (ntxs != 0) {
                    if (m.stxAddress != "")
                        m.scrollState.scrollTo(m.scrollState.maxValue)
                    mdl.sendReset()
                }
            }
            else {
                if (!dm.isLoadingMore.value)
                    m.lazyState.scrollToItem(0, 0)
            }
        }



        val infiniteTransition = rememberInfiniteTransition()

        val pulleyColor by animateColorAsState(
            label = "pulleyColor",
            targetValue = if (pulld or isRefreshing) Colors.BackRefresh else Colors.BackRefDark,
            animationSpec = tween(512)
        )

        val pulleySize by animateFloatAsState(
            label = "pulleySize",
            targetValue = refreshState.progress.pow(0.5f) * 70,
            animationSpec = tween(if (isRefreshing) 512 else 32)
        )

        val glowingToGreen by infiniteTransition.animateColor(
            initialValue = Colors.Primary,
            targetValue = Colors.BalGreen,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        val glowingToRed by infiniteTransition.animateColor(
            initialValue = Colors.Primary,
            targetValue = Colors.BalRed,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        LaunchedEffect(pulld) {
            if (pulld) view.vibrateLongPress()
        }

        val rm = LocalContext.current.resources
        val titleText: String? = remember(networkConnected, isRefreshing, isConnected) {
            when {
                !networkConnected -> rm.getString(R.string.waiting_for_network)
                !isRefreshing -> null
                isConnected -> rm.getString(R.string.updating)
                else -> rm.getString(R.string.connecting)
            }
        }

        var lastBackPressed = remember { 0L }
        BackHandler {
            if (nowms() - lastBackPressed < 2000L) {
                // Android 12 like warm start + actually lock screen on back
                // If user exists app using home button or recents then his security is his responsibility
                lastBackPressed = 0L
                lockClicked()
                crs?.launch {
                    delay(300) // Prevent information leak when resuming activity
                    act.moveTaskToBack(true)
                }
            } else {
                lastBackPressed = nowms()
                Toast.makeText(act, R.string.press_back_once_again_to_exit, Toast.LENGTH_SHORT).show()
            }
        }

        //****************************************************************************************//
        //
        //                                           TOP BAR
        //
        //****************************************************************************************//
        TopBar(
            color = Color.Black, textColor = Color.White, alignContent = false,
            titleText = titleText
/*<---*/) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = if (titleText != null) 0f else alpha
                    }
                    .padding(start = 17.dp, top = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.gem), contentDescription = "TON",
                        modifier = Modifier.size(18.dp))
                    Text(text = wallet?.balance?.simpleBalance() ?: "", color = Color.White,
                        style = Styles.topBalanceText, modifier = Modifier.padding(start = 2.dp))
                }
                Row {
                    Text(text = "≈", color = Colors.Gray, style = Styles.topBalanceSubText,
                        modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                    Text(text = currBalance, color = Colors.Gray, style = Styles.topBalanceSubText)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.size(48.dp, 56.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = { m.showTCTransfer() }) {
                    Icon(
                        tint = Color.White,
                        painter = painterResource(id = R.drawable.tonconnect),
                        contentDescription = stringResource(R.string.ton_connect),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            /*
            Box(modifier = Modifier.size(48.dp, 56.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = ::lockClicked) {
                    Icon(
                        tint = Color.White,
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.lock_screen_now)
                    )
                }
            }
            */
            Box(modifier = Modifier.size(48.dp, 56.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = ::scanClicked) {
                    Icon(
                        tint = Color.White,
                        painter = painterResource(id = R.drawable.ic_scan),
                        contentDescription = stringResource(R.string.scan_qr_code)
                    )
                }
            }
            Box(modifier = Modifier.size(48.dp, 56.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = ::settingsClicked) {
                    Icon(
                        tint = Color.White,
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Settings"
                    )
                }
            }
        }

        //****************************************************************************************//
        //
        //                                     MAIN CONTENT
        //
        //****************************************************************************************//
        // Kinda done: Decompose this monstrosity into Elements or Frags
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(refreshState)
                .nestedScroll(nestedScrollConnection),
            color = Color.Black
/*<---*/) { // Use the minimize button, luke!
            BoxWithConstraints(contentAlignment = Alignment.BottomCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(m.scrollState)
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.Black)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pulleySize.dp) // For spring-like effect ^0.5
                            .background(pulleyColor)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(20.dp, 8.dp)
                                .width(16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Colors.PulleyColor,
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    painterResource(
                                        id = R.drawable.ic_pulley_down
                                    ),
                                    contentDescription = "Pull to refresh icon",
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .size(16.dp)
                                        .graphicsLayer(
                                            rotationZ = -clamp(
                                                2 * (refreshState.progress - 0.4f), 0f, 1f
                                            ) * 180, transformOrigin = TransformOrigin.Center
                                        ),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                        Text(
                            text = stringResource(
                                id = if (isRefreshing) R.string.refreshing
                                else if (pulld) R.string.release_to_refresh
                                else R.string.swipe_down_to_refresh
                            ),
                            color = Color.White.copy(
                                if (isRefreshing) 1f else clamp(
                                    4 * (refreshState.progress - 0.1f), 0f, 1f
                                )
                            ),
                            style = Styles.pullRefresh,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(6.dp)
                        )
                    }
                    WalletTopPanelFrag(
                        address = address,
                        balance = balance,
                        hasBalance = wallet?.balance != null,
                        showLottie = showLottie,
                        currBalance = currBalance,
                        receiveClicked = ::receiveClicked,
                        sendClicked = ::sendClicked,
                        alpha = { alpha },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((this@BoxWithConstraints.maxHeight - 56.dp) * 0.35f)
                            .background(Color.Black))
                    // *****************************************************************************
                    val lastTxId = wallet?.lastTxId
                    // no wallet value (null) -> loading
                    // lastTxId not empty, tx list empty -> loading
                    // lastTxId empty -> wallet created
                    if ((lastTxId == null) || (lastTxId != "@" && ntxs == 0))
                        Surface(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxWidth(),
                            color = Color.White,
                            shape = Styles.panelShapeTop
                        ) {
                            val notrans by remember { derivedStateOf { !preload and !isRefreshing } }
                            WalletLoadingFrag(address = wallet?.address ?: "", notrans = notrans,
                                openExplorer = {
                                    uriHandler.openUri(makeExplorerLink(address = wallet!!.address))
                                })
                        }
                    else if (lastTxId == "@" && ntxs == 0)
                        Surface(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxWidth(),
                            color = Color.White,
                            shape = Styles.panelShapeTop
                        ) {
                            WalletCreatedFrag(address = wallet?.address ?: "")
                        }
                    else
                        Surface(
                            modifier = Modifier
                                //.weight(0.65f)
                                .height(this@BoxWithConstraints.maxHeight - 56.dp)
                                .fillMaxWidth(),
                            color = Color.White,
                            shape = Styles.panelShapeTop
                        ) {
                            val tl by remember { derivedStateOf { txs ?: listOf() } }
                            val dateFmt = remember { DateTimeFormatter.ofPattern("MMMM d")
                                .withLocale(Locale.getDefault())
                                .withZone(ZoneId.systemDefault()) }
                            val dateFmtXL = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy")
                                .withLocale(Locale.getDefault())
                                .withZone(ZoneId.systemDefault()) }
                            val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm")
                                .withLocale(Locale.getDefault())
                                .withZone(ZoneId.systemDefault()) }
                            // var stickyDate by remember { mutableStateOf("") }
                            val stickyDate by remember { derivedStateOf {
                                val thisInst = Instant.ofEpochSecond(tl.getOrNull(m.lazyState.firstVisibleItemIndex)?.now ?: 0)
                                if (thisInst.atZone(ZoneId.systemDefault()).year != Instant.now().atZone(ZoneId.systemDefault()).year)
                                    dateFmtXL.format(thisInst) else dateFmt.format(thisInst)
                            }}
                            if (!stickyDate.endsWith("1970")) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(1f),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .zIndex(1f)
                                            .wrapContentSize()
                                            .padding(12.dp, 16.dp, 0.dp, 0.dp),
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = stickyDate, style = Styles.date,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = m.lazyState
                                // userScrollEnabled = txCanScroll.value
                            ) {
                                // Need to know index to check previous item for those damn date rows
                                // items(txs ?: listOf(), key = { it.id }) {
                                // TODO: Show pending or failed outgoing transaction
                                items(tl.size, key = { tl[it].id + "|" +  (if (it > 0) tl[it - 1].id else "NULL") }) {
                                    TransRow(
                                        prev = if (it > 0) tl[it - 1] else null, tx = tl[it],
                                        dateFmt = dateFmt, timeFmt = timeFmt, dateFmtXL = dateFmtXL
                                    ) { tx, inc, adr, amt, cmt ->
                                        m.stxTransaction = tx
                                        m.stxIsIncoming = inc
                                        m.stxAddress = adr
                                        m.stxAmount = amt
                                        m.stxComment = cmt
                                        m.showTransaction()
                                    }
                                }
                                if (tl.isNotEmpty() && tl.last().prevId != ZERO_TX) {
                                    item {
                                        UniversalItem(text = stringResource(R.string.load_more_transactions),
                                            subText = stringResource(R.string.very_old_transactions),
                                            color = Colors.Primary,
                                            progIcon = dm.isLoadingMore.value,
                                            value = if (dm.isLoadingMore.value) {
                                                if (dm.doneServersLM.value == -1)
                                                    stringResource(R.string.not_found)
                                                else
                                                    stringResource(R.string.searching_n_m,
                                                        dm.doneServersLM.value, 9) }
                                            else null)
                                        {
                                            dm.loadMoreTransactions(9)
                                        }
                                    }
                                }
                                if (wallet?.lastTxId != ZERO_TX) {
                                    item {
                                        UniversalItem(text = "Open account in explorer",
                                            color = Colors.Primary,
                                            last = true)
                                        {
                                            uriHandler.openUri(makeExplorerLink(address = wallet!!.address))
                                        }
                                    }
                                }
                                // DONE: Show load more at the end of list (if last.prev != 0)
                                // DONE: Show explorer link at the end of list
                            }
                        }
                    if (tcShowBar)
                        Spacer(modifier = Modifier
                            .height(50.dp)
                            .background(Color.White)
                            .fillMaxWidth())
                }
                if (tcShowBar) {
                    Surface(
                        color = Colors.TranBorder,
                        shape = Styles.smallPanelShapeTop,
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .height(51.dp)
                            .fillMaxWidth()
                            .zIndex(1f)
                    ) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            Surface(
                                color = Color.White,
                                shape = Styles.panelShapeTop,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (tcConnecting) return@combinedClickable
                                            if (tcConnected) {
                                                Toast
                                                    .makeText(
                                                        act,
                                                        R.string.tap_and_hold_to_disconnect_dapp,
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            } else {
                                                if (dapp?.closed == true) {
                                                    view.vibrateError()
                                                    Toast
                                                        .makeText(
                                                            act,
                                                            R.string.connection_removed_dapp,
                                                            Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                } else {
                                                    view.vibrateLongPress()
                                                    dm.tcConnect()
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (tcConnecting) return@combinedClickable
                                            view.vibrateLongPress()
                                            if (tcConnected) {
                                                dm.tcDisconnect()
                                            } else {
                                                crs?.launch {
                                                    db
                                                        .dappDao()
                                                        .setCurrent("")
                                                }
                                            }
                                        }
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(8.dp, 0.dp)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        tint = when {
                                            tcClosed -> Color.Red
                                            tcConnected and !tcWantConnected ->
                                                glowingToRed
                                            tcConnected -> Colors.BalGreen
                                            tcConnecting -> glowingToGreen
                                            else -> Colors.BalRed
                                        },
                                        painter = painterResource(id = R.drawable.tonconnect),
                                        contentDescription = stringResource(R.string.ton_connect),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = (dappName ?: stringResource(R.string.ton_connect))
                                            .appendStyled(
                                                when {
                                                    tcClosed -> stringResource(R.string.is_closed)
                                                    tcConnected and !tcWantConnected ->
                                                        stringResource(R.string.is_disconnecting)
                                                    tcConnected -> stringResource(R.string.is_connected)
                                                    tcConnecting -> stringResource(R.string.is_connecting)
                                                    else -> stringResource(R.string.is_not_connected)
                                                }, SpanStyle(
                                                    color = when {
                                                        tcClosed -> Color.Red
                                                        tcConnected and !tcWantConnected ->
                                                            glowingToRed
                                                        tcConnected -> Colors.BalGreen
                                                        tcConnecting -> glowingToGreen
                                                        else -> Colors.BalRed
                                                    }, fontWeight = FontWeight.Normal
                                                )
                                            ),
                                        color = Color.Black,
                                        style = Styles.tonConnect
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        //****************************************************************************************//
        //
        //                               TON CONNECT OVERLAYS
        //
        //****************************************************************************************//
        Overlay(visible = m.isShowTCConnect.value, darker = true, backdropClicked = {}) {
            BackHandler(onBack = {})
            val pi = tcPending
            if (pi != null)
                TCConnectFrag(
                    name = pi.name,
                    hostname = pi.domain(),
                    walletAddress = wallet?.address ?: "NULL",
                    walletVerRev = wallet?.verRev?.vrStr() ?: "NULL",
                    loading = tcConnectLoading,
                    success = tcConnectSuccess,
                    imageProvider = {
                        if (pi.iconUrl != null)
                            SubcomposeAsyncImage(
                                model = pi.iconUrl,
                                loading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 5.dp,
                                        color = Colors.Primary
                                    )
                                },
                                contentDescription = pi.name
                            )
                    },
                    connectClicked = {
                        tcConnectLoading = true
                        crs?.launch(Dispatchers.IO) {
                            val da = mdl.tcPendingItem.value!!
                            db.dappDao().set(da)
                            db.dappDao().setCurrent(da.id)
                            dm.tcConnect()
                            for (i in 0..3000) {
                                delay(200)
                                if (dm.tcCurrentApp.value == da.id)
                                    break
                            }
                            dm.tcSayHello(da, mdl.tcWantedCItems)
                            mdl.tcPendingCR.value = null
                            mdl.tcWantedCItems = mapOf()
                            tcConnectSuccess = true
                            tcConnectLoading = false
                            delay(2000)
                            m.hideTCConnect()
                            delay(1000)
                            mdl.tcPendingItem.value = null
                        }
                    },
                    closeClicked = {
                        if (tcConnectLoading || tcConnectSuccess) return@TCConnectFrag
                        crs?.launch {
                            // TODO: Should send rejection? It it just a close button, not a cancel
                            // It seems that "ethalon" TonKeeper does not send a rejection
                            m.hideTCConnect()
                            delay(520)
                            mdl.tcPendingCR.value = null
                            mdl.tcPendingItem.value = null
                            mdl.tcWantedCItems = mapOf()
                        }
                    },
                    heightPct = m.showTCConnectHeight.value
                )
        }
        //****************************************************************************************//

        val mock = remember { createMockTransaction() }
        Overlay(visible = m.isShowTCTransfer.value, darker = true, backdropClicked = {}) {
            BackHandler(onBack = {})
            TCTransferFrag(
                outputs = listOf(
                    mock.amt!! to mock.dst!! with Pair(mock.cmt?.length ?: 0, 1),
                    mock.amt1!! to mock.dst1!! with Pair(mock.cmt1?.length ?: 0, 1),
                    mock.amt2!! to mock.dst2!! with Pair(mock.cmt2?.length ?: 0, 1),
                    mock.amt3!! to mock.dst3!! with Pair(mock.cmt3?.length ?: 0, 1),
                    // mock.inamt!! to mock.src!! with mock.incmt
                ),
                loading = tcTransferLoading,
                success = tcTransferSuccess,
                confirmClicked = {
                    crs?.launch {
                        tcTransferLoading = true
                        delay(1000)
                        tcTransferSuccess = true
                        delay(2000)
                        m.hideTCTransfer()
                        delay(1000)
                        tcTransferLoading = false
                        tcTransferSuccess = false
                    }
                },
                cancelClicked = { m.hideTCTransfer() },
                heightPct = m.showTCTransferHeight.value
            )
        }
        //****************************************************************************************//
        //
        //                                 RECEIVE OVERLAY
        //
        //****************************************************************************************//
        Overlay(visible = m.isShowReceive.value, darker = true, backdropClicked = m::hideReceive) {
            BackHandler(onBack = m::hideReceive)
            ReceiveFrag(address = wallet?.address ?: "", heightPct = m.showReceiveHeight.value)
        }
        //****************************************************************************************//
        //
        //                               TRANSACTION OVERLAY
        //
        //****************************************************************************************//
        // BUG: https://issuetracker.google.com/issues/279118447
        // When pausing and resuming activity BackHandler does not work anymore
        // Can't be fixed here, need to wait for fix from Google in 2.7 compose navigation lib
        // DONE: Maybe try to find some workaround about that ...
        // WOW: Fixed with a backFix crutch! And overriding BackHandler in BaseScreen!
        Overlay(visible = m.isShowTransaction.value, darker = true, backdropClicked = m::hideTransaction) {
            BackHandler(onBack = m::hideTransaction)
            TransactionFrag(
                transaction = m.stxTransaction,
                isIncoming = m.stxIsIncoming,
                address = m.stxAddress,
                amount = m.stxAmount,
                comment = m.stxComment,
                heightPct = m.showTransactionHeight.value,
                resolveAddr = { it -> db.nameDao().getByAddress(it)?.name },
                sendClicked = ::transactionSendClicked
            )
        }
    }
}

private class MainWalletModel : ScreenModel {

    var scrollState = ScrollState(0)
    val lazyState = LazyListState()

    var isShowReceive = mutableStateOf(false)
    var showReceiveHeight = mutableStateOf(0f)
    fun showReceive() = showOrHide(true,  isShowReceive, showReceiveHeight)
    fun hideReceive() = showOrHide(false, isShowReceive, showReceiveHeight)

    var isShowTransaction = mutableStateOf(false)
    var showTransactionHeight = mutableStateOf(0f)
    fun showTransaction() = showOrHide(true,  isShowTransaction, showTransactionHeight)
    fun hideTransaction() = showOrHide(false, isShowTransaction, showTransactionHeight)

    var isShowTCConnect = mutableStateOf(false)
    var showTCConnectHeight = mutableStateOf(0f)
    fun showTCConnect() = showOrHide(true,  isShowTCConnect, showTCConnectHeight)
    fun hideTCConnect() = showOrHide(false, isShowTCConnect, showTCConnectHeight)

    var isShowTCTransfer = mutableStateOf(false)
    var showTCTransferHeight = mutableStateOf(0f)
    fun showTCTransfer() = showOrHide(true,  isShowTCTransfer, showTCTransferHeight)
    fun hideTCTransfer() = showOrHide(false, isShowTCTransfer, showTCTransferHeight)

    var isShowTCSettings = mutableStateOf(false)
    var showTCSettingsHeight = mutableStateOf(0f)
    fun showTCSettings() = showOrHide(true,  isShowTCSettings, showTCSettingsHeight)
    fun hideTCSettings() = showOrHide(false, isShowTCSettings, showTCSettingsHeight)

    var stxTransaction by mutableStateOf(createMockTransaction(0))
    var stxIsIncoming by mutableStateOf(false)
    var stxAddress by mutableStateOf("")
    var stxAmount by mutableStateOf(0L)
    var stxComment by mutableStateOf<String?>(null)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            if (scrollState.canScrollForward && delta < 0) {
                crs.launch {
                    scrollState.scroll { this.scrollBy(-delta) }
                }
                return available
            }
            return Offset.Zero
        }
    }

    // DONE: Remove repetetive code
    fun showOrHide(isShow: Boolean, showState: MutableState<Boolean>, heightState: MutableState<Float>) {
        if (isShow) showState.value = true
        heightState.value = 0f
        crs.launch {
            delay(if (isShow) 10 else 510)
            if (isShow)
                heightState.value = 1f
            else
                showState.value = false
        }
    }





}