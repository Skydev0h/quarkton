package app.quarkton.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.extensions.simpleBalance
import app.quarkton.extensions.vrStr
import app.quarkton.ton.Wallet
import app.quarkton.ton.now
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ChooseWalletScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)

        val scrollState = rememberScrollState()

        val masterchain = remember { mutableStateOf(false) }
        var showmywalre by remember { mutableStateOf(per.showMyInRecents) }

        LifecycleEffect(onStarted = {
            crs?.launch {
                withContext(Dispatchers.IO) {
                    masterchain.value = dm.getCurrentVerRev() < 0
                    if (db.walletDao().getAll().count { if (masterchain.value) it.verRev < 0 else it.verRev >= 0 } < Wallet.useWallets.size)
                        dm.refreshOtherWallets(workchainId = if (masterchain.value) -1 else 0 )
                }
            }
        })

        val updSel = { vr: Int ->
            crs?.launch {
                withContext(Dispatchers.IO) {
                    db.walletDao().setCurrent(vr)
                }
            }
        }

        val wallets = db.walletDao().observeAll().observeAsState()
        val actwals = remember { derivedStateOf { wallets.value?.filter { !Wallet.vrdeprecated(it.verRev) } } }
        val depwals = remember { derivedStateOf { wallets.value?.filter { Wallet.vrdeprecated(it.verRev) } } }
        val nowRelaxed by mdl.nowRelaxed.collectAsStateWithLifecycle(initialValue = now() * 1000L)

        val updated = stringResource(R.string.updated_)
        val behindRightNow = stringResource(R.string.behind_right_now)
        val behindRecently = stringResource(R.string.behind_recently)
        val behindSecondsAgo = stringResource(R.string.behind_seconds_ago)
        val behindMinuteAgo = stringResource(R.string.behind_minute_ago)
        val behindMinutesAgo = stringResource(R.string.behind_minutes_ago)
        val behindHourAgo = stringResource(R.string.behind_hour_ago)
        val behindHoursAgo = stringResource(R.string.behind_hours_ago)
        val behindLongTimeAgo = stringResource(R.string.behind_long_time_ago)

        val refreshingSince by dm.refreshingOtherSince
        val isRefreshing by dm.isRefreshingOther
        val refProgress by dm.otherRefrProg

        val lastUpdated = remember { derivedStateOf {
            if (wallets.value == null) return@derivedStateOf null
            if (isRefreshing) return@derivedStateOf null
            val behind = (nowRelaxed -
                    (wallets.value
                        ?.filter { if (masterchain.value) it.verRev < 0 else it.verRev >= 0  }
                        ?.minOf { it.updated } ?: nowRelaxed)) / 1000L
            updated + when {
                behind < 10L -> behindRightNow
                behind < 60L -> behindRecently
                behind < 3600L -> "${behind/60} ${if (behind >= 120) behindMinutesAgo else behindMinuteAgo}"
                behind < 86400L -> "${behind/3600} ${if (behind >= 7200) behindHoursAgo else behindHourAgo}"
                else -> behindLongTimeAgo
            }
        }}

        TopBar(color = Color.Black, textColor = Color.White, backColor = Color.White,
            titleText = stringResource(R.string.settings_active_address), backIcon = true)

        Surface(
            modifier = Modifier.fillMaxSize(), color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize())
            {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = Styles.panelShapeTop
                ) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState))
                    {
                        UniversalItem(text = stringResource(if (isRefreshing)
                                R.string.loading_and_scanning_wallets else
                                    R.string.tap_here_to_refresh_them),
                            value = refProgress,
                            progIcon = isRefreshing,
                            subText = lastUpdated.value
                        )
                        {
                            dm.refreshOtherWallets(workchainId = if (masterchain.value) -1 else 0 )
                        }
                        UniversalItem(text = stringResource(R.string.show_my_wallets_in_recents),
                                    toggle = showmywalre) {
                            showmywalre = !showmywalre
                            per.showMyInRecents = showmywalre
                        }
                        UniversalItem(header = stringResource(R.string.actual_wallet_versions),
                            color = if (masterchain.value) Colors.TextOrange else null)
                        actwals.value?.filter { if (masterchain.value) it.verRev < 0 else it.verRev >= 0  }?.forEach {
                            UniversalItem(text = "Wallet " + it.verRev.vrStr(),
                                subText = "${it.address.substring(0,12)}…${it.address.substring(it.address.length-12)}",
                                value = it.balance.simpleBalance(2),
                                color = if (it.current) Colors.Primary else Color.Black,
                                valueColor = if (it.current) Colors.Primary else Color.Black,
                                subColor = if (it.current) Color.Black else Color.Gray,
                                style = if (it.current) Styles.primlabel else Styles.mainText,
                                valueStyle = if (it.current) Styles.primlabel else Styles.mainText,
                                progIcon = isRefreshing and (it.updated < refreshingSince)
                            ) { _ -> updSel(it.verRev) }
                        }
                        UniversalItem(header = stringResource(R.string.legacy_wallet_versions),
                            color = if (masterchain.value) Colors.TextOrange else null)
                        UniversalItem(header = stringResource(R.string.not_recommended_wallets), color = Colors.Gray)
                        depwals.value?.filter { if (masterchain.value) it.verRev < 0 else it.verRev >= 0  }?.forEach {
                            UniversalItem(text = "Wallet " + it.verRev.vrStr(),
                                subText = "${it.address.substring(0,6)}…${it.address.substring(it.address.length-6)}",
                                value = it.balance.simpleBalance(2),
                                color = if (it.current) Colors.Primary else Color.Gray,
                                valueColor = if (it.current) Colors.Primary else Color.Gray,
                                subColor = if (it.current) Color.Black else Color.Gray,
                                style = if (it.current) Styles.primlabel else Styles.mainText,
                                valueStyle = if (it.current) Styles.primlabel else Styles.mainText,
                                progIcon = isRefreshing and (it.updated < refreshingSince)
                            ) { _ -> updSel(it.verRev) }
                        }
                        UniversalItem(header = stringResource(R.string.advanced_settings),
                            color = if (masterchain.value) Colors.TextOrange else null)
                        UniversalItem(header = stringResource(R.string.masterchain_cost_warning), color = Colors.TextOrange)
                        UniversalItem(text = stringResource(R.string.use_masterchain), toggle = masterchain.value,
                            color =  Colors.TextOrange) {
                            masterchain.value = !masterchain.value
                            crs?.launch {
                                scrollState.scrollTo(0)
                                if (db.walletDao().getAll().count { if (masterchain.value) it.verRev < 0 else it.verRev >= 0 } < Wallet.useWallets.size)
                                    dm.refreshOtherWallets(workchainId = if (masterchain.value) -1 else 0 )
                            }
                        }
                    }
                }
            }
        }
    }

}