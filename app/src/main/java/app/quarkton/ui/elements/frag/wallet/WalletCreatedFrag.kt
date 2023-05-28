package app.quarkton.ui.elements.frag.wallet

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.breakMiddle
import app.quarkton.ui.elements.Lottie
import app.quarkton.ui.theme.Styles

@Composable
fun WalletCreatedFrag(address: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Lottie(
                modifier = Modifier.size(100.dp),
                // imageId = R.drawable.ph_created,
                lottieId = R.raw.created
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.wallet_created),
                style = Styles.pageTitle
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.your_wallet_address),
                style = Styles.mainText, color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            val cm = LocalClipboardManager.current
            val lc = LocalContext.current
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
                Text(
                    text = address.breakMiddle(),
                    style = Styles.address,
                    modifier = Modifier
                        .clickable {
                            cm.setText(AnnotatedString(address ?: ""))
                            Toast.makeText(lc,"Copied to clipboard",Toast.LENGTH_SHORT).show()
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}