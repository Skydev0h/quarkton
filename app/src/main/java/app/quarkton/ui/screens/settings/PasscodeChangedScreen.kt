package app.quarkton.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.quarkton.R
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate

class PasscodeChangedScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init()
        val imp = mdl.setupIsImporting
        JumboTemplate(
            // imageId = if (imp) R.drawable.ph_congratulations else R.drawable.ph_success,
            lottieId = if (imp) R.raw.congratulations else R.raw.success,
            titleText = stringResource(R.string.passcode_changed),
            mainText = stringResource(R.string.passcode_changed_descr)
        ) {
            JumboButtons(mainText = stringResource(R.string.btn_proceed),
                mainClicked = {
                    nav?.pop()
                }
            )
        }
    }

}
