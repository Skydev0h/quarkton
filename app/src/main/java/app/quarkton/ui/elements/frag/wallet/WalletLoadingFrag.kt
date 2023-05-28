package app.quarkton.ui.elements.frag.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.ui.elements.Lottie
import com.airbnb.lottie.compose.LottieConstants

@Composable
fun WalletLoadingFrag(
    address: String,
    notrans: Boolean,
    openExplorer: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            key(notrans) {
                Lottie(
                    modifier = Modifier.size(100.dp),
                    // imageId = R.drawable.ph_loading,
                    lottieId = if (!notrans) R.raw.loading else R.raw.test_time,
                    iterations = if (!notrans) LottieConstants.IterateForever else 1
                )
            }
            if (notrans) {
                WalletNoTransFrag(address = address, openExplorer = openExplorer)
            }
        }
    }
}