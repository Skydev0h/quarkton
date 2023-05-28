package app.quarkton.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.quarkton.R
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.other.StartupScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeleteWalletScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init()
        var overlay by remember { mutableStateOf(false) }
        TopBar(backIcon = true)
        JumboTemplate(
            // imageId = R.drawable.ph_too_bad,
            lottieId = R.raw.too_bad,
            titleText = stringResource(R.string.really_delete_wallet),
            mainText = stringResource(R.string.delete_wallet_warning)
        ) {
            JumboButtons(mainText = stringResource(R.string.go_back_to_settings), mainClicked = {
                nav?.pop()
            }, secText = "Confirm wallet deletion", secClicked = {
                overlay = true
                crs?.launch {
                    per.hardReset()
                    db.walletDao().deleteAll()
                    db.transDao().deleteAll()
                    db.nameDao().deleteAll()
                    delay(500)
                    nav?.replaceAll(StartupScreen())
                }
            }, secColor = Color.Red)
        }
        Overlay(
            visible = overlay, showProgress = true, color = Color.Red
        )
    }

}
