package app.quarkton.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.quarkton.R
import app.quarkton.extensions.simpleBalance
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.theme.Styles

class TokensListScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    @Composable
    override fun Content() {
        Init(dark = true)

        val scrollState = rememberScrollState()

        val wallet = db.walletDao().observeCurrent().observeAsState()

        TopBar(color = Color.Black, textColor = Color.White, backColor = Color.White,
            titleText = stringResource(R.string.settings_list_of_tokens), backIcon = true)

        Surface(
            modifier = Modifier.fillMaxSize(), color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize())
            {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = Styles.panelShapeTop
                ) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState))
                    {
                        UniversalItem(header = stringResource(R.string.native_tokens))
                        UniversalItem(text = stringResource(R.string.token_ton),
                            toggle = true, last = true, enabled = false,
                            value = wallet.value?.balance?.simpleBalance(2) ?: "") {}

                        // SettingItem(header = stringResource(R.string.extra_currencies))

                        UniversalItem(header = stringResource(R.string.jettons))
                        /*
                        SettingItem(stringResource(R.string.search_in_transactions)) {}
                        SettingItem(stringResource(R.string.add_manually), last = true) {}
                        */

                        UniversalItem(header = stringResource(R.string.nfts))
                        /*
                        SettingItem(stringResource(R.string.search_in_transactions)) {}
                        SettingItem(stringResource(R.string.add_manually), last = true) {}
                        */
                    }
                }
            }
        }
    }

}