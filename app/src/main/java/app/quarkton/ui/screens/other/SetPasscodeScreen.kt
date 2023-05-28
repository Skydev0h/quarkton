package app.quarkton.ui.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.quarkton.R
import app.quarkton.extensions.vibrateError
import app.quarkton.ui.elements.Alert
import app.quarkton.ui.elements.BiathlonBox
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.Keypad
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.onboarding.DoneScreen
import app.quarkton.ui.screens.settings.PasscodeChangedScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

class SetPasscodeScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    private fun finishSetup(nav: Navigator?, sm: SetPasscodeModel) {
        if (mdl.updatingPasscode) {
            per.updatePasscode(sm.passcode.value)
            nav?.replace(PasscodeChangedScreen())
            return
        }
        mdl.finishSetup(sm.passcode.value, completed = {
            nav?.replaceAll(DoneScreen())
        }, failed = {
            act.updateStatusBar(black = false, dim = true)
            mdl.showAlert()
        })
    }

    private fun keypadPressed(
        sm: SetPasscodeModel, c: Char, bad: () -> Unit, good: () -> Unit, next: () -> Unit
    ) {
        if (c == '<') {
            if (sm.passcode.value.isNotEmpty())
                sm.passcode.value = sm.passcode.value.dropLast(1)
            return
        }
        if (sm.passcode.value.length < sm.passlength.value)
            sm.passcode.value = sm.passcode.value.plus(c)
        if (sm.passcode.value.length == sm.passlength.value) {
            if (!sm.confirming.value) {
                next()
                return
            } else {
                if (sm.confirmer.value == sm.passcode.value) {
                    good()
                    return
                }
                bad()
            }
        }
    }

    private fun alertClickHandler(id: Int, nav: Navigator?, sm: SetPasscodeModel) {
        if (id == R.string.btn_try_again) {
            finishSetup(nav, sm)
            return
        }
        sm.passcode.value = ""
        act.updateStatusBar(black = false, dim = false)
        mdl.hideAlert()
    }

    @Composable
    override fun Content() {
        Init()
        val view = LocalView.current

        val sm = rememberScreenModel { SetPasscodeModel() }
        val alertExists by mdl.alertExists.collectAsStateWithLifecycle()
        val alertShown by mdl.alertShown.collectAsStateWithLifecycle()
        TopBar(backIcon = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Spacer(
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .background(Color.White)
            )
            JumboTemplate(
                // imageId = R.drawable.ph_password,
                lottieId = R.raw.password,
                titleText = stringResource(
                    if (!sm.confirming.value)
                        (if (!mdl.updatingPasscode) R.string.set_a_passcode else R.string.set_a_new_passcode)
                    else
                        (if (!mdl.updatingPasscode) R.string.confirm_a_pascode else R.string.confirm_a_new_pascode)
                ),
                mainText = stringResource(
                    if (!mdl.updatingPasscode) R.string.enter_passcode_digits else R.string.enter_new_passcode_digits,
                sm.passlength.value),
                balloon = false,
                lottieAnimEnd = 0.5f
            ) {
                Spacer(modifier = Modifier.weight(28f))
                BiathlonBox(
                    count = sm.passlength.value,
                    filled = sm.passcode.value.length,
                    error = sm.error.value,
                    scale = sm.scale.value
                )
                Spacer(modifier = Modifier.weight(31f))
                if (!sm.confirming.value) {
                    val den = LocalDensity.current
                    val offset = remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
                    Box() {
                        DropdownMenu(expanded = sm.popup.value, onDismissRequest = { sm.popup.value = false },
                            modifier = Modifier.background(Color.White).onSizeChanged {
                                offset.value = with(den) { DpOffset(-it.width.toDp() / 2, -it.height.toDp()) }
                            }.widthIn(200.dp),
                            offset = offset.value
                        ) {
                            for (digits in arrayOf(4, 6)) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.n_digit_code,
                                                digits
                                            ),
                                            style = Styles.passcodeListEntry
                                        )
                                    }, onClick = {
                                        sm.confirming.value = false
                                        sm.passcode.value = ""
                                        sm.confirmer.value = ""
                                        sm.passlength.value = digits
                                        sm.popup.value = false
                                    }
                                )
                            }
                        }
                    }
                    Box() {
                        TextButton(
                            onClick = { sm.popup.value = true },
                            modifier = Modifier
                                .height(Styles.buttonHeight)
                                .width(200.dp),
                            shape = Styles.buttonShape
                        ) {
                            Text(
                                text = stringResource(id = R.string.passcode_options),
                                style = Styles.mainText, color = Colors.Primary
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(Styles.buttonHeight))
                }
                Spacer(modifier = Modifier.weight(16f))
                Keypad(onPressed = {
                    if (sm.scale.value or sm.error.value) return@Keypad
                    keypadPressed(sm, it, bad = {
                        view.vibrateError()
                        crs?.launch {
                            delay(300L)
                            sm.error.value = true
                            sm.scale.value = true
                            delay(500L)
                            sm.scale.value = false
                            sm.confirming.value = false
                            sm.passcode.value = ""
                            sm.confirmer.value = ""
                            delay(1000L)
                            sm.error.value = false
                        }
                    }, good = {
                        crs?.launch {
                            delay(300L)
                            sm.scale.value = true
                            delay(300L)
                            sm.scale.value = false
                            delay(300L)
                            finishSetup(nav, sm)
                        }
                    }) {
                        crs?.launch {
                            delay(300L)
                            sm.scale.value = true
                            delay(300L)
                            sm.scale.value = false
                            delay(300L)
                            sm.confirmer.value = sm.passcode.value
                            sm.confirming.value = true
                            sm.passcode.value = ""
                        }
                    }
                })
            }
        }
        if (alertExists) { // Alert is recomposed for some reason each frame when scrolling
            Alert(enabled = alertShown == 1,
                titleText = stringResource(R.string.setup_failed),
                mainText = stringResource(R.string.error_setup_failed_text),
                buttons = intArrayOf(R.string.btn_ok, R.string.btn_try_again),
                clickHandler = { id -> alertClickHandler(id, nav, sm) })
        }
    }

}

private class SetPasscodeModel : ScreenModel {
    val confirming = mutableStateOf(false)
    val passlength = mutableStateOf(4)
    val passcode = mutableStateOf("")
    val confirmer = mutableStateOf("")
    val error = mutableStateOf(false)
    val scale = mutableStateOf(false)
    val popup = mutableStateOf(false)
}