package app.quarkton.ui.elements.frag.tonconnect

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.quarkton.R
import app.quarkton.db.createMockTransaction
import app.quarkton.extensions.breakMiddle
import app.quarkton.extensions.formatBalance
import app.quarkton.extensions.shortAddr
import app.quarkton.extensions.simpleBalance
import app.quarkton.extensions.vibrateLongPress
import app.quarkton.extensions.with
import app.quarkton.ui.elements.Lottie
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles

@Composable
fun TCTransferFrag(
    outputs: List<Triple<Long, String, Pair<Int, Int>>>,
    loading: Boolean,
    success: Boolean,
    confirmClicked: (() -> Unit),
    cancelClicked: (() -> Unit),
    heightPct: Float = 1f,
    inMasterChain: Boolean = false
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val cm = LocalClipboardManager.current
    fun copy(s: String) {
        cm.setText(AnnotatedString(s))
        Toast
            .makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT)
            .show()
    }

    val animHeight by animateFloatAsState(targetValue = heightPct, label = "Height", animationSpec = tween(500))

    val btnScaleX by animateFloatAsState(
        targetValue = if (success) 0f else 1f, tween(1000),
        label = "btnScaleX"
    )
    val btnScaleY by animateFloatAsState(
        targetValue = if (success) 0.7f else 1f, tween(1000),
        label = "btnScaleY"
    )
    val btnRounder by animateDpAsState(
        targetValue = if (success) 20.dp else 8.dp, tween(500),
        label = "btnRounder"
    )

    val showCheck by remember { derivedStateOf { btnScaleY < 0.77f } }

    val totalAmount = remember(outputs) { outputs.sumOf { it.first } }
    val fmtAmount = remember(totalAmount) { totalAmount.formatBalance() }

    // already divided by 2^16 divisor
    val bitPrice = 1_000L
    val cellPrice = 100_000L
    val messagePrice = 6_804_000L
    // TODO: Maybe find way to *really* estimate the fee
    val fee by remember { derivedStateOf {
        (outputs.sumOf { it.third.first * bitPrice + it.third.second * cellPrice + messagePrice } ) *
                (if (inMasterChain) 1 else 10) // MC fees x10
    } }

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
                        text = stringResource(R.string.ton_transfer) + " UI Demo",
                        style = Styles.cardLabel,
                        color = Color.Black, modifier = Modifier
                            .padding(20.dp, 16.dp, 0.dp, 16.dp)
                            .align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
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
                            text = fmtAmount, color = Color.Black, style = Styles.bigBalanceText
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    if (outputs.size > 1) {
                        UniversalItem(header = stringResource(R.string.receivers))
                    }
                    for (i in outputs.indices) {
                        var expandRecipient by remember { mutableStateOf(false) }
                        UniversalItem(text = if (outputs.size == 1) stringResource(R.string.recipient)
                            else (
                                if (expandRecipient)
                                    outputs[i].first.simpleBalance(9)
                                else
                                    outputs[i].first.simpleBalance()
                            ),
                            valueColor = if (i <= 3) Color.Black else Color.Red,
                            color = if (i <= 3) Color.Black else Color.Red,
                            valueStyle = if (expandRecipient) Styles.smallAddress else Styles.address,
                            value = outputs[i].second.let {
                                if (expandRecipient) it.breakMiddle() else it.shortAddr() },
                            onLongClick = {
                                view.vibrateLongPress()
                                copy(outputs[i].second)
                            },
                            subText = if (expandRecipient) stringResource(
                                R.string.n_bits_n_cells,
                                outputs[i].third.first,
                                outputs[i].third.second
                            ) else null,
                            preGemIcon = outputs.size > 1
                        ) {
                            expandRecipient = !expandRecipient
                        }
                    }
                    if (outputs.isEmpty())
                        UniversalItem(
                            text = stringResource(R.string.error_no_recipients),
                            color = Colors.TextRed,
                            disableRipple = true
                        )
                    UniversalItem(text = stringResource(R.string.fee),
                        valueColor = Color.Black, gemIcon = true,
                        value = "≈ " + fee.simpleBalance(3),
                        last = true,
                        disableRipple = true
                    )
                    Spacer(modifier = Modifier.height(64.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                        contentAlignment = Alignment.Center) {
                        // *************************************************************************
                        if (showCheck)
                            Icon(
                                painter = painterResource(id = R.drawable.bluecheck),
                                contentDescription = "",
                                modifier = Modifier.zIndex(1f),
                                tint = Color.Unspecified
                            )
                        Row(modifier = Modifier
                            .height(Styles.buttonHeight)
                            .fillMaxWidth(btnScaleX),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = cancelClicked,
                                modifier = Modifier
                                    .weight(btnScaleX * btnScaleX * btnScaleX * 0.999f + 0.001f)
                                    .fillMaxHeight(btnScaleX * btnScaleX)
                                    .graphicsLayer { alpha = btnScaleX * btnScaleX },
                                shape = Styles.buttonShape,
                                colors = ButtonDefaults.buttonColors(Colors.BackTranPrim, Colors.Primary)
                            ) {
                                if (!success) {
                                    Text(
                                        text = stringResource(R.string.btn_cancel),
                                        style = Styles.buttonLabel,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (outputs.size in 1..4) {
                                Spacer(modifier = Modifier.width(8.dp * btnScaleX))
                                Button(
                                    onClick = confirmClicked,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(btnScaleY),
                                    shape = RoundedCornerShape(btnRounder),
                                ) {
                                    if (!success) {
                                        if (loading) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                        Text(
                                            text = stringResource(R.string.btn_confirm),
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
                                }
                            }
                        }

                        // *************************************************************************
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val mock = remember { createMockTransaction() }
    QuarkTONWalletTheme {
        Overlay(visible = true, darker = true) {
            TCTransferFrag(
                outputs = listOf(
                    mock.amt!! to mock.dst!! with Pair(mock.cmt?.length ?: 0, 1),
                    mock.amt1!! to mock.dst1!! with Pair(mock.cmt1?.length ?: 0, 1),
                    mock.amt2!! to mock.dst2!! with Pair(mock.cmt2?.length ?: 0, 1),
                    mock.amt3!! to mock.dst3!! with Pair(mock.cmt3?.length ?: 0, 1),
                    // mock.inamt!! to mock.src!! with mock.incmt
                ),
                loading = false,
                success = false,
                confirmClicked = {},
                cancelClicked = {},
            )
        }
    }
}