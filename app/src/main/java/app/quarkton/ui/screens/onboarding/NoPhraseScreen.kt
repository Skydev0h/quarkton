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
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen

class NoPhraseScreen : BaseScreen() {

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
            titleText = stringResource(R.string.too_bad),
            mainText = stringResource(R.string.without_secret_words)
        ) {
            JumboButtons(mainText = stringResource(R.string.enter_24_secret_words), mainClicked = {
                nav?.pop()
            }, secText = stringResource(R.string.create_new_instead), secClicked = {
                overlay = true
                mdl.generateSeedPhrase {
                    if (it) {
                        nav?.popUntil { s -> s is WelcomeScreen }
                        nav?.push(WalletCreatedScreen())
                    }
                    else overlay = false
                }
            }, secWidth = 260)
        }
        Overlay(
            visible = overlay, showProgress = true
        )
    }

}
