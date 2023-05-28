package app.quarkton.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.quarkton.R
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.Overlay
import app.quarkton.ui.screens.BaseScreen
import com.airbnb.lottie.compose.LottieConstants

class WelcomeScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init()
        var overlay by remember { mutableStateOf(false) }
        JumboTemplate(
            // imageId = R.drawable.ph_main,
            lottieId = R.raw.start,
            titleText = stringResource(R.string.ton_wallet),
            mainText = stringResource(R.string.ton_wallet_allows),
            lottieIterations = LottieConstants.IterateForever
        ) {
            JumboButtons(mainText = stringResource(R.string.create_my_wallet), mainClicked = {
                overlay = true
                mdl.generateSeedPhrase {
                    if (it) nav?.push(WalletCreatedScreen())
                    else overlay = false
                }
            }, secText = stringResource(R.string.import_existing_wallet), secClicked = {
                nav?.push(ImportWalletScreen())
            })
        }
        Overlay(
            visible = overlay, showProgress = true
        )
    }

}
