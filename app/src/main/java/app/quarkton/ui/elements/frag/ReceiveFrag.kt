package app.quarkton.ui.elements.frag

import android.content.Intent
import android.util.Log
import android.widget.Toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.core.content.ContextCompat
import app.quarkton.R
import app.quarkton.db.MOCK_ADDR_1
import app.quarkton.extensions.breakMiddle
import app.quarkton.ui.elements.Centrify
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReceiveFrag(
    address: String,
    heightPct: Float = 1f
) {
    val cm = LocalClipboardManager.current
    val lc = LocalContext.current
    val data = remember { QrData.Url("ton://transfer/$address") }
    val options = remember {
        createQrVectorOptions {

            padding = .125f

            background {
                // drawable = ContextCompat.getDrawable(lc, R.drawable.)
            }

            logo {
                drawable = ContextCompat.getDrawable(lc, R.drawable.gem)
                size = .3f
                padding = QrVectorLogoPadding.Natural(.05f)
                shape = QrVectorLogoShape.RoundCorners(.5f)
            }
            colors {
                //dark = QrVectorColor.Solid(0xff345288.toInt())
                //ball = QrVectorColor.Solid(ContextCompat.getColor(context, R.color.your_color))
            }
            shapes {
                darkPixel = QrVectorPixelShape.RoundCorners(.5f)
                ball = QrVectorBallShape.RoundCorners(.25f)
                frame = QrVectorFrameShape.RoundCorners(.25f)
            }
        }
    }
    var largeCode by remember { mutableStateOf(false) }
    val drawable = remember { QrCodeDrawable(data, options) }
    val painter = rememberDrawablePainter(drawable = drawable)
    val animHeight by animateFloatAsState(targetValue = heightPct, label = "Height", animationSpec = tween(500))
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        BoxWithConstraints {
            val codeSize by animateDpAsState(
                targetValue = if (largeCode) (min(maxWidth, maxHeight * 0.7f) - 20.dp) else 200.dp, label = "codeSize",
                animationSpec = tween(500)
            )
            val ipc by animateFloatAsState(
                targetValue = if (largeCode) 0.25f else 1f, label = "interPadding",
                animationSpec = tween(500)
            )
            Surface(
                color = Color.White, shape = Styles.largePanelShapeTop,
                modifier = Modifier
                    .fillMaxWidth()
                    // .heightIn(0.dp, maxHeight * animHeight)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.receive_ton), style = Styles.cardLabel,
                        color = Color.Black, modifier = Modifier
                            .padding(4.dp, 0.dp, 0.dp, ipc * 8.dp + 8.dp)
                            .align(Alignment.Start)
                    )
                    Text(
                        text = stringResource(R.string.share_addr_to_others),
                        style = Styles.mainText,
                        color = Colors.Gray,
                        modifier = Modifier.padding(16.dp, ipc * 16.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .padding(ipc * ipc * 10.dp, ipc * 10.dp)
                            .size(codeSize).aspectRatio(1f)
                            // .fillMaxWidth(1f)
                            // .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(painter = painter, address, modifier = Modifier
                            .fillMaxSize()
                            .clickable { largeCode = !largeCode })
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
                        Text(
                            text = address.breakMiddle(),
                            style = Styles.address,
                            modifier = Modifier
                                .clickable {
                                    cm.setText(AnnotatedString(address))
                                    Toast
                                        .makeText(lc, "Copied to clipboard", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(ipc * 12.dp + 12.dp))
                    Button(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, address)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            lc.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Styles.buttonHeight),
                        shape = Styles.buttonShape
                    ) {
                        Text(
                            text = stringResource(R.string.share_wallet_address),
                            style = Styles.buttonLabel
                        )
                    }
                }
            }
        }
    }
    //}
}

@Preview
@Composable
private fun Preview() {
    QuarkTONWalletTheme {
        Overlay(visible = true, darker = true) {
            ReceiveFrag(address = MOCK_ADDR_1)
        }
    }
}