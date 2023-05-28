package app.quarkton.ui.elements.frag.tonconnect

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.quarkton.MainViewModel
import app.quarkton.R
import app.quarkton.db.AppDatabase
import app.quarkton.extensions.vibrateLongPress
import app.quarkton.ton.DataMaster
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TCSettingsFrag(
    db: AppDatabase,
    dm: DataMaster,
    mdl: MainViewModel,
    crs: CoroutineScope,
    walletAddress: String,
    closeClicked: (() -> Unit),
    heightPct: Float = 1f
) {
    val view = LocalView.current

    val animHeight by animateFloatAsState(targetValue = heightPct, label = "Height", animationSpec = tween(500))

    val apps = db.dappDao().observeAllByWallet(walletAddress).observeAsState()

    val fullFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy @ HH:mm")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault()) }

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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = closeClicked, modifier = Modifier.padding(4.dp)) {
                            Icon(
                                tint = Color.Black,
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close"
                            )
                        }
                        Text(
                            text = stringResource(R.string.ton_connect),
                            style = Styles.cardLabel,
                            color = Color.Black, modifier = Modifier
                                .padding(16.dp, 0.dp, 0.dp, 0.dp)
                        )
                    }
                    Text(
                        text = "Tap to connect or disconnect a DApp\nLong tap to irreversibly close or delete",
                        color = Color.Gray,
                        style = Styles.mainText,
                        modifier = Modifier
                            .padding(40.dp, 0.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)) {
                        items(apps.value ?: listOf()) { da ->
                            val col = when {
                                da.id == dm.tcCurrentApp.value -> when {
                                    dm.tcIsConnected.value -> Colors.BalGreen
                                    dm.tcIsConnecting.value -> Colors.TextOrange
                                    else -> Colors.Primary
                                }
                                da.active -> Colors.Primary
                                da.closed -> Color.Red
                                else -> Colors.Gray
                            }
                            // var dd by remember { mutableStateOf(false) }
                            UniversalItem(
                                text = da.name,
                                subText = da.domain() + "\n" + fullFormatter.format(Instant.ofEpochMilli(da.lastAct)),
                                value = when {
                                    da.id == dm.tcCurrentApp.value -> when {
                                        dm.tcIsConnected.value -> stringResource(R.string.tc_connected)
                                        dm.tcIsConnecting.value -> stringResource(R.string.tc_connecting)
                                        else -> stringResource(R.string.tc_standby)
                                    }
                                    da.active -> stringResource(R.string.tc_active)
                                    da.closed -> stringResource(R.string.tc_closed)
                                    else -> stringResource(R.string.tc_paused)
                                },
                                color = col,
                                valueColor = col,
                                onClick = {
                                    view.vibrateLongPress()
                                    if (da.id == dm.tcCurrentApp.value && dm.tcIsConnected.value)
                                        dm.tcDisconnect()
                                    else
                                        if (!da.closed) {
                                            crs.launch(Dispatchers.IO) {
                                                db.dappDao().setCurrent(da.id)
                                                dm.tcConnect()
                                            }
                                        }
                                },
                                onLongClick = {
                                    view.vibrateLongPress()
                                    crs.launch(Dispatchers.IO) {
                                        if (da.active) {
                                            db.dappDao().setCurrent("")
                                        } else {
                                            if (da.closed) {
                                                db.dappDao().deleteOneClosed(da.id)
                                            } else {
                                                dm.tcSayGoodbye(da.id)
                                                db.dappDao().disconnect(da.id)
                                            }
                                        }
                                    }
                                }
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
    QuarkTONWalletTheme {
        Overlay(visible = true, darker = true) {
            TCConnectFrag(
                name = "Fragment",
                hostname = "fragment.io",
                walletAddress = "UQBFblablablaAoKP",
                walletVerRev = "v4R2",
                loading = false,
                success = false,
                imageProvider = {
                    ContextCompat.getDrawable(LocalContext.current, R.mipmap.ic_launcher)?.let {
                        Image(painter = rememberDrawablePainter(drawable = it), contentDescription = "")
                    }
                },
                connectClicked = {},
                closeClicked = {})
        }
    }
}