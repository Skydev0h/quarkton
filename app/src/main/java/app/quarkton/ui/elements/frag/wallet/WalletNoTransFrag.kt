package app.quarkton.ui.elements.frag.wallet

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.breakMiddle
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

@Composable
fun WalletNoTransFrag(
    address: String,
    openExplorer: () -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.transactions_not_found),
        style = Styles.pageTitle
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        modifier = Modifier.padding(40.dp, 0.dp),
        text = stringResource(R.string.transactions_not_found_descr),
        style = Styles.mainText
    )
    Spacer(modifier = Modifier.height(16.dp))
    val cm = LocalClipboardManager.current
    val lc = LocalContext.current
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
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(
        onClick = openExplorer,
        modifier = Modifier.height(36.dp),
        shape = Styles.buttonShape
    ) {
        Text(
            text = "Open account in explorer",
            style = Styles.mainText, color = Colors.Primary
        )
    }
}