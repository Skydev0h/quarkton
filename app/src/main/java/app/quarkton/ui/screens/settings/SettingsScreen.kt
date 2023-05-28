package app.quarkton.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.extensions.vrStr
import app.quarkton.ton.supportedExplorers
import app.quarkton.ui.elements.Alert
import app.quarkton.ui.elements.UniversalItem
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.other.RecoveryPhraseScreen
import app.quarkton.ui.screens.other.SetPasscodeScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import cafe.adriel.voyager.navigator.Navigator

class SettingsScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    fun checkPasscode(title: String, next: BaseScreen, allowFP: Boolean = false) {
        mdl.nextSettingsScreen = next
        mdl.nextSettingsTitle.value = title
        mdl.nextSettingsAllowFP = allowFP
        nav?.push(CheckPasscodeScreen())
    }

    @Composable
    override fun Content() {
        Init(dark = true)

        val bio = remember { mutableStateOf(per.bioEnabled()) }
        val bioHardware by mdl.bioHardware.collectAsStateWithLifecycle()
        val bioAvailable by mdl.bioAvailable.collectAsStateWithLifecycle()
        val alertExists by mdl.alertExists.collectAsStateWithLifecycle()
        val alertShown by mdl.alertShown.collectAsStateWithLifecycle()
        val moreAuth = remember { mutableStateOf(per.reqAuthForSend) }

        val wallet = db.walletDao().observeCurrent().observeAsState()
        val walver = remember { derivedStateOf {
            wallet.value?.verRev?.vrStr(full = true) ?: ""
        }}

        val chg = stringResource(R.string.changing_smth)

        TopBar(color = Color.Black, textColor = Color.White, backColor = Color.White,
            titleText = stringResource(R.string.wallet_settings), backIcon = true)

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
                    Column(modifier = Modifier.fillMaxSize())
                    {

                        UniversalItem(header = stringResource(R.string.settings_category_general))
                        /*
                        UniversalItem(text = stringResource(R.string.settings_notifications), toggle = false) {
                            // TODO: Implement notifications (not REQUIRED for contest)
                            // Implementing notifications without using third party services (at
                            // least firebase) is a very complex task. Implementing them in timely
                            // manner is impossible task. (Android allows periodic work only in 15
                            // minute intervals, many OEM battery management services will want to
                            // kill the app or foreground service with persistent notification is
                            // needed that is also frowned upon by Android Doze and other services)
                        }
                        */
                        UniversalItem(text = stringResource(R.string.settings_active_address), value = walver.value,
                            valueColor = if (walver.value.endsWith("(MC)")) Colors.TextOrange else null) {
                            // TODO: Display as dropdown with advanced settings to go to separage page
                            // Go to advanced settings right away if unusual wallet is chosen
                            // Maybe display only if advanced settings are enabled
                            nav?.push(ChooseWalletScreen())
                        }
                        key(per.selectedCurrency) {
                            UniversalItem(text = stringResource(R.string.settings_primary_currency),
                                value = per.selectedCurrency) {
                                nav?.push(CurrencyListScreen())
                            }
                        }
                        UniversalItem(text = stringResource(R.string.settings_list_of_tokens)) {
                            nav?.push(TokensListScreen())
                        }
                        UniversalItem(text = stringResource(R.string.preferred_explorer),
                            value = supportedExplorers()[per.selectedExplorer],
                            last = true) {
                            nav?.push(ExplorersListScreen())
                        }

                        UniversalItem(header = stringResource(R.string.settings_category_security))
                        val reqauth = stringResource(R.string.require_auth)
                        UniversalItem(text = stringResource(R.string.require_auth_for_sending),
                            toggle = moreAuth.value, subText = stringResource(R.string.ask_passcode_each_time)
                        ) {
                            val newVal = !moreAuth.value
                            if (!newVal) {
                                // moreAuth True -> False
                                mdl.nextSettingsAllowFP = true
                                mdl.nextSettingsAction = {
                                    per.reqAuthForSend = false
                                    moreAuth.value = false
                                    nav?.popUntil { s -> s is SettingsScreen }
                                }
                                mdl.nextSettingsTitle.value = chg.replace("~", reqauth)
                                nav?.push(CheckPasscodeScreen())
                            } else {
                                // moreAuth False -> True
                                // Allowed without confirmation because it may be possible that you
                                // need to quickly turn it on in some emergency situation for safety
                                per.reqAuthForSend = true
                                moreAuth.value = true
                            }
                        }
                        UniversalItem(text = stringResource(R.string.settings_show_recovery_phrase)) {
                            mdl.seedPhraseShown = 1L
                            checkPasscode(it, RecoveryPhraseScreen())
                        }
                        val enterCurrentPasscode = stringResource(R.string.enter_current_passcode)
                        UniversalItem(text = stringResource(R.string.settings_change_passcode)) {
                            mdl.updatingPasscode = true
                            checkPasscode(enterCurrentPasscode, SetPasscodeScreen())
                        }
                        UniversalItem(text = stringResource(R.string.settings_biometric_auth),
                            toggle = bio.value, enabled = bioAvailable and bioHardware) {
                            if (!bioHardware)
                                showAlert(1)
                            else if (!bioAvailable)
                                showAlert(2)
                            else {
                                val newVal = !bio.value
                                mdl.nextSettingsAllowFP = !newVal
                                mdl.nextSettingsAction = {
                                    per.bioChange(newVal)
                                    bio.value = per.bioEnabled()
                                    nav?.popUntil { s -> s is SettingsScreen }
                                }
                                mdl.nextSettingsTitle.value = chg.replace("~", it)
                                nav?.push(CheckPasscodeScreen())
                            }
                        }
                        UniversalItem(text = stringResource(R.string.settings_delete_wallet), color = Color.Red, last = true) {
                            checkPasscode(it, DeleteWalletScreen())
                        }
                        /*
                        UniversalItem(header = stringResource(R.string.advanced_settings),
                            color = Colors.DarkShade)
                        UniversalItem(text = stringResource(R.string.activate_advanced_features),
                            color = Colors.HalfShade, last = true)
                        {

                        }
                        */
                    }
                }
            }
        }

        if (alertExists) { // Alert is recomposed for some reason each frame when scrolling
            Alert(enabled = alertShown == 1,
                titleText = stringResource(R.string.not_available),
                mainText = stringResource(R.string.error_bio_not_supported),
                buttons = intArrayOf(R.string.btn_ok),
                clickHandler = { mdl.hideAlert() })
            Alert(enabled = alertShown == 2,
                titleText = stringResource(R.string.not_available),
                mainText = stringResource(R.string.error_bio_not_enrolled),
                buttons = intArrayOf(R.string.btn_ok),
                clickHandler = { mdl.hideAlert() })
        }
    }

}