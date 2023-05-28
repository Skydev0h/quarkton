package app.quarkton.ui.screens.other

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.quarkton.MainActivity
import app.quarkton.R
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.screens.BaseScreen
import com.airbnb.lottie.compose.LottieConstants

class NotSetupScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init()
        JumboTemplate(
            // imageId = R.drawable.ph_main,
            lottieId = R.raw.test_time,
            titleText = stringResource(R.string.not_initialized),
            mainText = stringResource(R.string.ton_wallet_not_initialized),
        ) {
            JumboButtons(mainText = stringResource(R.string.start_setup_process), mainClicked = {
                nav?.replace(StartupScreen())
            }, secText = stringResource(R.string.return_back_for_now), secClicked = {
                act.finish()
            })
        }
    }

}
