package app.quarkton.ui.screens.settings

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.quarkton.R
import app.quarkton.extensions.vibrateError
import app.quarkton.ui.elements.BiathlonBox
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.Keypad
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles

class CheckPasscodeScreen : BaseSettingsScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    private fun keypadPressed(
        sm: CheckPasscodeScreenModel, c: Char, bad: () -> Unit, good: () -> Unit
    ) {
        if (c == '<') {
            if (sm.passcode.value.isNotEmpty())
                sm.passcode.value = sm.passcode.value.dropLast(1)
            return
        }
        if (sm.passcode.value.length < sm.passlength.value)
            sm.passcode.value = sm.passcode.value.plus(c)
        if (sm.passcode.value.length == sm.passlength.value) {
            if (per.verifyPasscode(sm.passcode.value)) {
                good()
                return
            }
            bad()
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        if (!mdl.nextSettingsAllowFP) return // safeguard

        val executor = ContextCompat.getMainExecutor(act)
        val biometricPrompt = BiometricPrompt(act, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    // nav?.replace(mdl.nextScreen ?: MainWalletScreen())
                    // mdl.nextScreen = null
                    onSuccess()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(act.getString(R.string.biometric_authentication))
            .setSubtitle(act.getString(R.string.use_biometrics_to_unlock))
            .setNegativeButtonText(act.getString(R.string.use_passcode))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    @Composable
    override fun Content() {
        Init(dark = true)
        val view = LocalView.current

        val sm = rememberScreenModel { CheckPasscodeScreenModel() }
        val nextTitle by mdl.nextSettingsTitle.collectAsStateWithLifecycle()
        val bio = per.bioEnabled()

        fun done() {
            mdl.nextSettingsAction?.invoke()
            if (mdl.nextSettingsScreen != null)
                nav?.replace(mdl.nextSettingsScreen ?: SettingsScreen())
            mdl.nextSettingsAction = null
            mdl.nextSettingsScreen = null
        }

        fun biometricDone() {
            if (!mdl.nextSettingsAllowFP) return // safeguard
            if (!bio) return // safeguard
            crs?.launch {
                sm.stop.value = true
                crs?.launch {
                    delay(300L)
                    sm.scale.value = true
                    delay(300L)
                    sm.scale.value = false
                    delay(300L)
                    done()
                    // sm.stop.value = false
                }
            }
        }

        LifecycleEffect(onStarted = {
            // act.portraitOnly().unsecure().updateStatusBar(true)
            sm.passlength.value = per.lockScreenGetPassLength()
            if (bio && mdl.nextSettingsAllowFP) {
                crs?.launch {
                    delay(160)
                    try {
                        showBiometricPrompt(::biometricDone)
                    } catch (_: Throwable) {
                        // ignored
                    }
                }
            }
            sm.stop.value = false
        })

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
                titleText = nextTitle,
                mainText = stringResource(R.string.enter_passcode_digits, sm.passlength.value),
                balloon = false,
                lottieAnimEnd = 0.5f
            ) {
                Spacer(modifier = Modifier.weight(28f))
                BiathlonBox(
                    count = sm.passlength.value,
                    filled = if (sm.stop.value) sm.passlength.value else sm.passcode.value.length,
                    error = sm.error.value,
                    scale = sm.scale.value
                )
                Spacer(modifier = Modifier.weight(31f))
                val den = LocalDensity.current
                val offset = remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
                Box() {
                    DropdownMenu(
                        expanded = sm.popup.value,
                        onDismissRequest = { sm.popup.value = false },
                        modifier = Modifier
                            .background(Color.White)
                            .onSizeChanged {
                                offset.value =
                                    with(den) {
                                        DpOffset(
                                            0.dp,
                                            -it.height.toDp() - Styles.buttonHeight
                                        )
                                    }
                            }
                            .widthIn(200.dp),
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
                                        style = Styles.passcodeListEntry,
                                        color = Color.Black
                                    )
                                }, onClick = {
                                    sm.passcode.value = ""
                                    sm.passlength.value = digits
                                    sm.popup.value = false
                                    per.lockScreenSetPassLength(digits)
                                }
                            )
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
                }
                Spacer(modifier = Modifier.weight(16f))
                Keypad(onPressed = {
                    if (sm.scale.value or sm.error.value or sm.stop.value) return@Keypad
                    if (it == '~')
                        try {
                            if (bio && mdl.nextSettingsAllowFP)
                                showBiometricPrompt(::biometricDone)
                        } catch (_: Throwable) {
                            // ignored
                        }
                    else
                        keypadPressed(sm, it, bad = {
                            view.vibrateError()
                            sm.stop.value = true
                            crs?.launch {
                                delay(300L)
                                sm.error.value = true
                                sm.scale.value = true
                                delay(500L)
                                sm.scale.value = false
                                sm.passcode.value = ""
                                delay(1000L)
                                sm.error.value = false
                                sm.stop.value = false
                            }
                        }) {
                            sm.stop.value = true
                            crs?.launch {
                                delay(300L)
                                sm.scale.value = true
                                delay(300L)
                                sm.scale.value = false
                                delay(300L)
                                done()
                                // sm.stop.value = false
                            }
                        }
                }, dark = false, fp = bio && mdl.nextSettingsAllowFP)
            }
        }

    }

}

private class CheckPasscodeScreenModel : ScreenModel {
    val passlength = mutableStateOf(4)
    val passcode = mutableStateOf("")
    val error = mutableStateOf(false)
    val scale = mutableStateOf(false)
    val popup = mutableStateOf(false)
    val stop = mutableStateOf(false)
}