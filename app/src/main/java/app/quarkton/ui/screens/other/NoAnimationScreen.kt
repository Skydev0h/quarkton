package app.quarkton.ui.screens.other

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.quarkton.ui.screens.BaseScreen

class NoAnimationScreen : BaseScreen() {

    // Used to quickly transfer from MainWalletScreen to LockScreen

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init()
    }

}
