package app.quarkton.ui.screens.onboarding

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.math.MathUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.extensions.vibrateError
import app.quarkton.ton.extensions.isStdValid
import app.quarkton.ui.elements.Alert
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.SeedTextField
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ton.crypto.encodeHex
import org.ton.mnemonic.Mnemonic

class ImportWalletScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    private fun continueClicked(nav: Navigator?, values: List<String>, bad: (Int) -> Unit) {
        for (i in values.indices) {
            if (values[i] == "") {
                Log.w("ImportWordsScreen", "Item $i is empty")
                bad(i)
                return
            }
        }
        if (values.size == 1) { // Private key
            val pk = values[0].trim()
            if ((pk.length != 64) or (!pk.all { it.isDigit() or (it.lowercaseChar() in 'a'..'f') })) {
                bad(-1)
                showAlert(3)
                return
            }
        } else {
            // NB: There is tiny probability that valid bep39 seed will be valid also as TON
            //
            val validTon = Mnemonic.isValid(values)
            val validStd = Mnemonic.isStdValid(values)
            if (!validTon && !validStd) {
                bad(-1)
                showAlert()
                return
            }
            if (!validTon) {
                bad(-1)
                showAlert(2)
                return
            }
        }
        mdl.setupIsImporting = true
        mdl.importSeedPhrase(values)
        nav?.push(TestPassedScreen())
    }

    @Composable
    override fun Content() {
        Init(secure = true)
        val view = LocalView.current
        val fm = LocalFocusManager.current

        val scrollState = rememberScrollState()
        val barPosY = remember { mutableStateOf(0f) }
        val textPosY = remember { mutableStateOf(0f) }
        val topPosY = remember { mutableStateOf(0f) }
        val seedLenPopup = remember { mutableStateOf(false) }
        val titleAlpha = {
            1 - MathUtils.clamp((textPosY.value - barPosY.value - 20) / 30, 0f, 1f)
        }
        val seedLen = remember { mutableStateOf(24) }
        val numbers = remember(seedLen.value) { Array(seedLen.value) { it + 1 } }
        val texts = remember(seedLen.value) { Array(seedLen.value) { mutableStateOf(TextFieldValue("")) } }
        val focuses = remember(seedLen.value) { Array(seedLen.value) { FocusRequester() } }
        val alertExists by mdl.alertExists.collectAsStateWithLifecycle()
        val alertShown by mdl.alertShown.collectAsStateWithLifecycle()

        fun done() {
            continueClicked(nav, texts.map { it.value.text }, bad = {
                view.vibrateError()
                if (it >= 0) {
                    crs?.launch {
                        scrollState.animateScrollTo(0, tween(500))
                        delay(500)
                        try {
                            focuses[it].requestFocus()
                        } catch (ex: Exception) {
                            Log.w("ImportWordsScreen", "Cannot request forucs to $it")
                        }
                    }
                }
            })
        }

        LaunchedEffect(Unit) {
            try {
                scrollState.animateScrollTo(0)
                delay(500)
                focuses[0].requestFocus()
            } catch (e: Exception) {
                Log.w("ImportWordsScreen", "Could not set focus to first element")
            }
        }
        TopBar(
            backIcon = true,
            // elevate = MathUtils.clamp(scrollState.value.toFloat(), 0f, 8f),
            titleText = stringResource(id = if (seedLen.value > 1) R.string.n_secret_words
                else R.string.private_key, seedLen.value),
            // textColor = Color.Black, // remember(titleAlpha) { Color.Black.copy(titleAlpha) },
            outBarPos = { barPosY.value = it.y.value },
            textGraphics = { this.alpha = titleAlpha() },
            graphics = { this.shadowElevation = MathUtils.clamp(-topPosY.value, 0f, 8f) }
        ) {
            Box(modifier = Modifier.size(56.dp, 56.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = {
                    seedLenPopup.value = true
                }) {
                    Icon(
                        tint = Colors.Gray,
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Settings"
                    )
                }
                DropdownMenu(expanded = seedLenPopup.value,
                    onDismissRequest = { seedLenPopup.value = false },
                    modifier = Modifier.background(Color.White)) {
                    for (len in 24 downTo 12 step 6) {
                        DropdownMenuItem(text = {
                            Text(
                                text = stringResource(if (len == 24) R.string.import_seed_recommended
                                    else R.string.import_seed_less_secure, len),
                                style = Styles.passcodeListEntry.copy( fontWeight = if (
                                    seedLen.value == len) FontWeight.Medium else FontWeight.Normal)
                            )
                        }, onClick = {
                            seedLen.value = len
                            seedLenPopup.value = false
                        })
                    }
                    DropdownMenuItem(text = {
                        Text(
                            text = stringResource(R.string.import_private_key),
                            style = Styles.passcodeListEntry.copy( fontWeight = if (
                                    seedLen.value == 1) FontWeight.Medium else FontWeight.Normal)
                        )
                    }, onClick = {
                        seedLen.value = 1
                        seedLenPopup.value = false
                    })
                    /*
                    DropdownMenuItem(text = {
                        Text(
                            text = "Connect Ledger hardware wallet",
                            style = Styles.passcodeListEntry
                        )
                    }, onClick = {
                        seedLenPopup.value = false
                    })
                    */
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(Color.White)
        ) {
            Spacer(
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .background(Color.White)
                    .onGloballyPositioned { topPosY.value = it.positionInWindow().y }
            )
            JumboTemplate(
                // imageId = R.drawable.ph_recovery_phrase,
                lottieId = if (seedLen.value > 1) R.raw.recovery_phrase else R.raw.lock_key,
                titleText = stringResource(if (seedLen.value > 1) R.string.n_secret_words
                    else R.string.private_key, seedLen.value),
                mainText = stringResource(if (seedLen.value > 1) R.string.import_words_desc
                    else R.string.import_private_key_desc, seedLen.value),
                balloon = false,
                outTextPos = { textPosY.value = it.y.value },
                // titleColor = remember(titleAlpha) { Color.Black.copy(MathUtils.clamp(1 - titleAlpha * 1.5f, 0f, 1f)) }
                titleGraphics = { this.alpha = MathUtils.clamp(1 - titleAlpha() * 1.5f, 0f, 1f) }
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                if (seedLen.value > 1) {
                    TextButton(
                        onClick = { nav?.push(NoPhraseScreen()) },
                        modifier = Modifier.height(36.dp),
                        shape = Styles.buttonShape
                    ) {
                        Text(
                            text = stringResource(id = R.string.i_dont_have_those),
                            style = Styles.mainText, color = Colors.ModalText
                        )
                    }
                    if (mdl.developmentMode) {
                        TextButton(
                            onClick = {
                                mdl.generateSeedPhrase(seedLen.value) {
                                    for (i in texts.indices) {
                                        texts[i].value = texts[i].value.copy(mdl.seedPhraseWord(i))
                                    }
                                }
                            },
                            modifier = Modifier.height(36.dp),
                            shape = Styles.buttonShape
                        ) {
                            Text(
                                text = stringResource(id = R.string.dev_generate_random),
                                style = Styles.mainText, color = Color.Red
                            )
                        }
                        TextButton(
                            onClick = {
                                mdl.generateSeedPhrase(-seedLen.value) {
                                    for (i in texts.indices) {
                                        texts[i].value = texts[i].value.copy(mdl.seedPhraseWord(i))
                                    }
                                }
                            },
                            modifier = Modifier.height(36.dp),
                            shape = Styles.buttonShape
                        ) {
                            Text(
                                text = stringResource(id = R.string.dev_generate_random_std),
                                style = Styles.mainText, color = Color.Red
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                key(seedLen.value) {
                    if (numbers.size > 1) {
                        for (i in numbers.indices)
                            SeedTextField(i, numbers, texts, focuses, hidePopup = alertExists,
                                keyboardActions = KeyboardActions( onDone = { done() } ))
                    } else {
                        val ctx = LocalContext.current
                        val pickFileLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri ->
                            if (uri != null) {
                                // Update the state with the Uri
                                ctx.contentResolver.openFileDescriptor(uri, "r").use {
                                    if (it == null) return@rememberLauncherForActivityResult
                                    if ((it.statSize != 32L) and (it.statSize != 64L)) {
                                        showAlert(4)
                                        return@rememberLauncherForActivityResult
                                    }
                                }
                                ctx.contentResolver.openInputStream(uri).use {
                                    if (it == null) return@rememberLauncherForActivityResult
                                    val data = it.readBytes()
                                    if (data.size == 32) {
                                        texts[0].value = texts[0].value.copy(data.encodeHex(), TextRange(64))
                                        return@rememberLauncherForActivityResult
                                    }
                                    else if (data.size == 64) {
                                        val str = data.decodeToString()
                                        if (!str.all { c -> c.isDigit() or (c.lowercaseChar() in 'a'..'f') }) {
                                            showAlert(4)
                                            return@rememberLauncherForActivityResult
                                        }
                                        texts[0].value = texts[0].value.copy(str, TextRange(64))
                                    }
                                }
                            }
                        }
                        // Private key
                        OutlinedTextField(value = texts[0].value, onValueChange = {
                            if (it.text.length > 64) return@OutlinedTextField
                            val t = it.text.filter { c -> c.isDigit() or (c.lowercaseChar() in 'a'..'f') }
                            val tr = it.selection
                            val d = it.text.length - t.length
                            texts[0].value = if (t != it.text) it.copy(t,
                                TextRange(tr.start - d, tr.end - d)) else it
                        }, modifier = Modifier
                            .fillMaxWidth(7f/9f)
                            .padding(0.dp, 16.dp),
                            textStyle = Styles.address,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            keyboardActions = KeyboardActions( onDone = { done() } )
                        )
                        TextButton(
                            onClick = {
                                try {
                                    pickFileLauncher.launch("*/*")
                                } catch (_: Throwable) {
                                    error("Failed to open pickFileLauncher")
                                }
                            },
                            modifier = Modifier.height(36.dp),
                            shape = Styles.buttonShape
                        ) {
                            Text(
                                text = stringResource(R.string.load_private_key_from_file),
                                style = Styles.mainText, color = Colors.ModalText
                            )
                        }
                    }
                }
                JumboButtons(
                    mainText = stringResource(R.string.btn_continue),
                    mainClicked = { done() },
                    topSpacing = 28,
                    bottomSpacing = 58
                )
            }
        }
        if (alertExists) { // Alert is recomposed for some reason each frame when scrolling
            Alert(enabled = alertShown == 1,
                titleText = stringResource(R.string.incorrect_words),
                mainText = stringResource(R.string.incorrect_secret_words),
                buttons = intArrayOf(R.string.btn_ok),
                clickHandler = {
                    fm.clearFocus()
                    act.updateStatusBar(black = false, dim = false)
                    mdl.hideAlert()
                })
            Alert(enabled = alertShown == 2,
                titleText = stringResource(R.string.are_you_sure),
                mainText = stringResource(R.string.std_import_warning),
                buttons = intArrayOf(R.string.btn_yes, R.string.btn_no),
                clickHandler = {
                    if (it == R.string.btn_yes) {
                        act.updateStatusBar(black = false, dim = false)
                        mdl.hideAlert()
                        mdl.setupIsImporting = true
                        mdl.importSeedPhrase(texts.map { t -> t.value.text })
                        nav?.push(TestPassedScreen())
                    } else {
                        fm.clearFocus()
                        act.updateStatusBar(black = false, dim = false)
                        mdl.hideAlert()
                    }
                })
            Alert(enabled = alertShown == 3,
                titleText = stringResource(R.string.incorrect_words),
                mainText = stringResource(R.string.invalid_private_key),
                buttons = intArrayOf(R.string.btn_ok),
                clickHandler = {
                    fm.clearFocus()
                    act.updateStatusBar(black = false, dim = false)
                    mdl.hideAlert()
                })
            Alert(enabled = alertShown == 4,
                titleText = stringResource(R.string.invalid_file_selected),
                mainText = stringResource(R.string.invalid_pk_file),
                buttons = intArrayOf(R.string.btn_ok),
                clickHandler = {
                    fm.clearFocus()
                    act.updateStatusBar(black = false, dim = false)
                    mdl.hideAlert()
                })
        }
    }

}