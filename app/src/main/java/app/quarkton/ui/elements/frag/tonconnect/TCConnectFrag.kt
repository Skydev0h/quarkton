package app.quarkton.ui.elements.frag.tonconnect

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import app.quarkton.R
import app.quarkton.extensions.shortAddr
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.ui.theme.Styles
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun TCConnectFrag(
    name: String,
    hostname: String,
    walletAddress: String,
    walletVerRev: String,
    loading: Boolean,
    success: Boolean,
    imageProvider: @Composable (BoxScope.() -> Unit),
    connectClicked: (() -> Unit),
    closeClicked: (() -> Unit),
    heightPct: Float = 1f
) {
    val animHeight by animateFloatAsState(targetValue = heightPct, label = "Height", animationSpec = tween(500))

    val baseText = stringResource(R.string.tc_access_request)
    val complexText = remember(hostname, walletAddress, walletVerRev) {
        val b = AnnotatedString.Builder(60 + hostname.length)
        b.append("$hostname $baseText ")
        b.append(AnnotatedString(walletAddress.shortAddr(),
            SpanStyle(color = Color.Gray, fontFamily = FontFamily.Monospace)))
        b.append(" $walletVerRev.")
        b.toAnnotatedString()
    }

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

    val zIndex by remember { derivedStateOf { if (btnScaleY > 0.75f) -1f else 1f } }

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
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = closeClicked, modifier = Modifier.padding(4.dp)) {
                        Icon(
                            tint = Color.Black,
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(44.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        imageProvider()
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.connect_to, name),
                        color = Color.Black,
                        style = Styles.pageTitle,
                        modifier = Modifier
                            .padding(40.dp, 0.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = complexText,
                        color = Color.Black,
                        style = Styles.mainText,
                        modifier = Modifier
                            .padding(40.dp, 0.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Text(
                        text = stringResource(R.string.tc_be_sure_check_address),
                        color = Color.Gray,
                        style = Styles.mainText,
                        modifier = Modifier
                            .padding(40.dp, 0.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(Styles.buttonHeight),
                        contentAlignment = Alignment.Center) {
                        // *************************************************************************
                        Icon(
                            painter = painterResource(id = R.drawable.bluecheck),
                            contentDescription = "",
                            modifier = Modifier.zIndex(zIndex),
                            tint = Color.Unspecified
                        )
                        Button(
                            onClick = connectClicked,
                            modifier = Modifier
                                .fillMaxWidth(btnScaleX)
                                .height(Styles.buttonHeight * btnScaleY),
                            shape = RoundedCornerShape(btnRounder),
                        ) {
                            if (!success) {
                                if (loading) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Text(
                                    text = stringResource(R.string.connect_wallet),
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