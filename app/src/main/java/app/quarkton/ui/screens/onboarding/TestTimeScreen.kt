package app.quarkton.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.math.MathUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.extensions.vibrateError
import app.quarkton.ui.elements.Alert
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.SeedTextField
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.theme.Styles
import cafe.adriel.voyager.navigator.Navigator

class TestTimeScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    private fun continueClicked(nav: Navigator?, values: List<String>, bad: () -> Unit, empty: (Int) -> Unit) {
        val numbers = mdl.testNumbers
        for (i in 0..2) {
            if (values[i] == "") {
                empty(i)
                bad()
                return
            }
        }
        for (i in 0..2) {
            if (!mdl.checkSeedPhraseWord(numbers[i] - 1, values[i])) {
                act.updateStatusBar(black = false, dim = true)
                mdl.showAlert()
                bad()
                return
            }
        }
        nav?.push(TestPassedScreen())
    }

    private fun alertClickHandler(
        id: Int, nav: Navigator?, resetTexts: () -> Unit
    ) {
        if (id == R.string.btn_see_words) {
            act.updateStatusBar(black = false, dim = false)
            mdl.hideAlert()
            mdl.seedPhraseShown = 0L // Do not show warning if coming back from here
            nav?.pop()
            return
        }
        if (id == R.string.btn_try_again) {
            resetTexts()
        }
        act.updateStatusBar(black = false, dim = false)
        mdl.hideAlert()
    }

    @Composable
    override fun Content() {
        Init(secure = true)
        val scrollState = rememberScrollState()
        val barPosY = remember { mutableStateOf(0f) }
        val textPosY = remember { mutableStateOf(0f) }
        val topPosY = remember { mutableStateOf(0f) }
        val titleAlpha = {
            1 - MathUtils.clamp((textPosY.value - barPosY.value - 20) / 30, 0f, 1f)
        }
        val numbers = mdl.testNumbers
        val texts = remember { Array(3) { mutableStateOf(TextFieldValue("")) } }
        val focuses = remember { Array(3) { FocusRequester() } }
        val focusManager = LocalFocusManager.current
        val view = LocalView.current
        val alertExists by mdl.alertExists.collectAsStateWithLifecycle()
        val alertShown by mdl.alertShown.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            focuses[0].requestFocus()
        }
        TopBar(
            backIcon = true,
            // elevate = MathUtils.clamp(scrollState.value.toFloat(), 0f, 8f),
            titleText = stringResource(R.string.test_time),
            // textColor = Color.Black, // remember(titleAlpha) { Color.Black.copy(titleAlpha) },
            outBarPos = { barPosY.value = it.y.value },
            textGraphics = { this.alpha = titleAlpha() },
            graphics = { this.shadowElevation = MathUtils.clamp(-topPosY.value, 0f, 8f) }
        )
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
                // imageId = R.drawable.ph_test_time,
                lottieId = R.raw.test_time,
                titleText = stringResource(R.string.test_time),
                mainText = stringResource(R.string.lets_check_words, *numbers),
                balloon = false,
                outTextPos = { textPosY.value = it.y.value },
                titleGraphics = { this.alpha = MathUtils.clamp(1 - titleAlpha() * 1.5f, 0f, 1f) }
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                if (mdl.developmentMode) {
                    TextButton(
                        onClick = {
                            for (i in 0..2) {
                                texts[i].value = texts[i].value.copy(mdl.seedPhraseWord(numbers[i] - 1))
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        shape = Styles.buttonShape
                    ) {
                        Text(
                            text = stringResource(id = R.string.dev_fill_words),
                            style = Styles.mainText, color = Color.Red
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                for (i in 0..2) SeedTextField(i, numbers, texts, focuses, hidePopup = alertExists)
                JumboButtons(
                    mainText = stringResource(R.string.btn_continue),
                    mainClicked = { continueClicked(nav, texts.map { it.value.text },
                        bad = { view.vibrateError() },
                        empty = {
                            focuses[it].requestFocus()
                        })
                    },
                    topSpacing = 16,
                    bottomSpacing = 58
                )
            }
        }
        if (alertExists) { // Alert is recomposed for some reason each frame when scrolling
            Alert(enabled = alertShown == 1,
                titleText = stringResource(R.string.incorrect_words),
                mainText = stringResource(R.string.words_do_not_match),
                buttons = intArrayOf(R.string.btn_see_words, R.string.btn_try_again),
                clickHandler = { id -> focusManager.clearFocus()
                alertClickHandler(id, nav, resetTexts = {
                    for (i in 0..2) texts[i].value = texts[i].value.copy("", TextRange.Zero)
                    focuses[0].requestFocus()
                }) })
        }
    }

}