package app.quarkton.ui.screens.onboarding


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.ton.nowms
import app.quarkton.ui.LocalBFC
import app.quarkton.ui.elements.Alert
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.other.RecoveryPhraseScreen
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect

class WalletCreatedScreen : BaseScreen() {

    @Preview @Composable fun P() { Preview() }

    @Composable
    override fun Content() {
        var overlay by remember { mutableStateOf(false) }
        Init(secure = if (!overlay) false else null, delaySecure = 300)
        val alertExists by mdl.alertExists.collectAsStateWithLifecycle()
        val alertShown by mdl.alertShown.collectAsStateWithLifecycle()
        LifecycleEffect(onStarted = {mdl.setupIsImporting = false})

        TopBar(backIcon = true, backClick = {
            act.updateStatusBar(black = false, dim = true)
            mdl.showAlert()
        })
        JumboTemplate(
            // imageId = R.drawable.ph_congratulations,
            lottieId = R.raw.congratulations,
            titleText = stringResource(R.string.congratulations),
            mainText = stringResource(R.string.wallet_was_created)
        ) {
            JumboButtons(
                mainText = stringResource(R.string.btn_proceed),
                mainClicked = {
                    nav?.push(RecoveryPhraseScreen())
                    overlay = true
                    act.secure()
                    mdl.seedPhraseShown = nowms()
                    mdl.seedPhraseWarningShown = false
                }
            )
            BackHandler(enabled = true) {
                act.updateStatusBar(black = false, dim = true)
                mdl.showAlert()
            }
        }
        Overlay(visible = overlay)
        if (alertExists) { // Alert is recomposed for some reason each frame when scrolling
            Alert(enabled = alertShown == 1,
                titleText = stringResource(R.string.are_you_sure),
                mainText = stringResource(R.string.words_lost_if_return),
                buttons = intArrayOf(R.string.btn_yes, R.string.btn_no),
                clickHandler = {
                    act.updateStatusBar(black = false, dim = false)
                    mdl.hideAlert()
                    if (it == R.string.btn_yes) {
                        mdl.clearSeedPhrase()
                        nav?.pop()
                    }
                })
        }
    }

}
