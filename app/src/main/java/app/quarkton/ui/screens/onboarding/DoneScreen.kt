package app.quarkton.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import app.quarkton.R
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.wallet.MainWalletScreen

class DoneScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init()
        val imp = mdl.setupIsImporting
        // Start updating right now, it may finish or be close to finishing when user goes ahead
        // They may watch congratulations / success animation which may also occupy their time :)
        LifecycleEffect(onStarted = { dm.refreshCurrentWallet() })
        JumboTemplate(
            // imageId = if (imp) R.drawable.ph_congratulations else R.drawable.ph_success,
            lottieId = if (imp) R.raw.congratulations else R.raw.success,
            titleText = stringResource(if (imp) R.string.wallet_was_imported else R.string.ready_to_go),
            mainText = stringResource(if (imp) R.string.empty_string else R.string.you_are_all_set)
        ) {
            JumboButtons(mainText = stringResource(if (imp) R.string.btn_proceed else R.string.view_my_wallet),
                mainClicked = {
                    nav?.replaceAll(MainWalletScreen())
                }
            )
        }
    }

}
