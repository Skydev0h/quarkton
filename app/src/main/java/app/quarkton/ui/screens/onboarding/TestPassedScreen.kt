package app.quarkton.ui.screens.onboarding


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.ui.LocalBFC
import app.quarkton.ui.elements.Alert
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.other.SetPasscodeScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

class TestPassedScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(secure = false, delaySecure = 300)
        val bioHardware by mdl.bioHardware.collectAsStateWithLifecycle()
        val bioAvailable by mdl.bioAvailable.collectAsStateWithLifecycle()
        val setupUseBiometrics by mdl.onbUseBiometrics.collectAsStateWithLifecycle()
        val alertExists by mdl.alertExists.collectAsStateWithLifecycle()
        val alertShown by mdl.alertShown.collectAsStateWithLifecycle()
        TopBar(backIcon = true, backClick = {
            if (mdl.setupIsImporting) {
                act.updateStatusBar(black = false, dim = true)
                mdl.showAlert(2)
            }
            else
                nav?.pop()
        })
        JumboTemplate(
            // imageId = R.drawable.ph_success,
            lottieId = R.raw.success,
            titleText = stringResource(R.string.perfect),
            mainText = stringResource(R.string.now_set_passcode)
        ) {
            if (bioHardware) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!bioAvailable) {
                            act.updateStatusBar(black = false, dim = true)
                            mdl.showAlert()
                        }
                        else mdl.onbUseBiometrics.value = !mdl.onbUseBiometrics.value
                    }) {
                    Checkbox(checked = setupUseBiometrics, onCheckedChange = {
                        if (!bioAvailable) {
                            act.updateStatusBar(black = false, dim = true)
                            mdl.showAlert()
                        }
                        else mdl.onbUseBiometrics.value = !mdl.onbUseBiometrics.value
                    })
                    Text(
                        text = stringResource(id = R.string.enable_biometric_auth),
                        style = Styles.checkBox,
                        modifier = Modifier.padding(0.dp, 0.dp, 12.dp, 0.dp),
                        color = if (!bioAvailable) Colors.Gray else Color.Black
                    )
                }
            }
            JumboButtons(
                mainText = stringResource(R.string.set_a_passcode),
                mainClicked = {
                    mdl.updatingPasscode = false
                    nav?.push(SetPasscodeScreen())
                },
                topSpacing = 20
            )
            BackHandler(enabled = mdl.setupIsImporting) {
                act.updateStatusBar(black = false, dim = true)
                mdl.showAlert(2)
            }
        }
        if (alertExists) { // Alert is recomposed for some reason each frame when scrolling
            Alert(enabled = alertShown == 1,
                titleText = stringResource(R.string.not_available),
                mainText = stringResource(R.string.error_bio_not_enrolled),
                buttons = intArrayOf(R.string.btn_ok),
                clickHandler = { mdl.hideAlert() })
            Alert(enabled = alertShown == 2,
                titleText = stringResource(R.string.are_you_sure),
                mainText = stringResource(R.string.words_reenter_if_return),
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
