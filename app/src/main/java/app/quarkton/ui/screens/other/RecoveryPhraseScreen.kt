package app.quarkton.ui.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.math.MathUtils
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.R
import app.quarkton.extensions.breakMiddle
import app.quarkton.ton.nowms
import app.quarkton.ui.elements.Alert
import app.quarkton.ui.elements.JumboButtons
import app.quarkton.ui.elements.JumboTemplate
import app.quarkton.ui.elements.TopBar
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.onboarding.TestTimeScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator

class RecoveryPhraseScreen : BaseScreen() {

    @Preview
    @Composable
    fun P() {
        Preview()
    }

    private fun mainClicked(
        nav: Navigator?
    ) {
        val now = nowms()
        if (mdl.seedPhraseShown == 1L) {
            // If seedShown is 1 we have been invoked from Settings to display current seed
            nav?.pop()
            return
        }
        if (now - mdl.seedPhraseShown < 60_000) {
            // TODO: Consider smoother color change during transition (in all updates)
            act.updateStatusBar(black = false, dim = true)
            mdl.showAlert()
            return
        }
        mdl.randomizeTestNumbers()
        nav?.push(TestTimeScreen())
    }

    private fun alertClickHandler(
        id: Int, nav: Navigator?
    ) {
        if (id == R.string.btn_skip) {
            act.updateStatusBar(black = false, dim = false)
            mdl.hideAlert()
            mdl.randomizeTestNumbers()
            nav?.push(TestTimeScreen())
            return
        }
        // Because of animation second button may appear a little too early. Show it after anim.
        mdl.delayedSetWarningShown()
        act.updateStatusBar(black = false, dim = false)
        mdl.hideAlert()
    }

    @Composable
    override fun Content() {
        Init(secure = true)

        val nav = LocalNavigator.current
        val seedPhrase = if (mdl.seedPhraseShown == 1L)
            (per.getSeedPhrase() ?: listOf<String>()) else mdl.getSeedPhrase()
        val seedLength = seedPhrase.size
        val halfLength = seedLength / 2
        val scrollState = rememberScrollState()
        val barPosY = remember { mutableStateOf(0f) }
        val textPosY = remember { mutableStateOf(0f) }
        val titleAlpha = {
            1 - MathUtils.clamp((textPosY.value - barPosY.value - 40) / 20, 0f, 1f)
        }
        val alertExists by mdl.alertExists.collectAsStateWithLifecycle()
        val alertShown by mdl.alertShown.collectAsStateWithLifecycle()
        TopBar(
            backIcon = true,
            elevate = clamp(scrollState.value.toFloat(), 0f, 8f),
            titleText = stringResource(id = R.string.your_recovery_phrase),
            // textColor = Color.Black, // .copy(titleAlpha),
            outBarPos = { barPosY.value = it.y.value },
            textGraphics = { this.alpha = titleAlpha() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .background(Color.White)
            )
            JumboTemplate(
                // imageId = R.drawable.ph_recovery_phrase,
                lottieId = if (seedLength > 1) R.raw.recovery_phrase else R.raw.lock_key,
                titleText = stringResource(if (seedLength > 1) R.string.your_recovery_phrase
                    else R.string.your_private_key),
                mainText = stringResource(if (seedLength > 1) R.string.write_down_these_words
                    else R.string.write_down_private_key),
                balloon = false,
                outTextPos = { textPosY.value = it.y.value },
                // titleColor = Color.Black.copy(clamp(1 - titleAlpha() * 1.5f, 0f, 1f))
                titleGraphics = { this.alpha = clamp(1 - titleAlpha() * 1.5f, 0f, 1f) }
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                if (seedLength > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            for (i in 1..halfLength) {
                                Row(Modifier.height(32.dp)) {
                                    Text(
                                        text = "$i. ",
                                        textAlign = TextAlign.End,
                                        style = Styles.mainText,
                                        color = Colors.Gray,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = seedPhrase[i - 1],
                                        textAlign = TextAlign.Start,
                                        style = Styles.phrase,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            for (i in (halfLength + 1)..seedLength) {
                                Row(Modifier.height(32.dp)) {
                                    Text(
                                        text = "$i. ",
                                        textAlign = TextAlign.End,
                                        style = Styles.mainText,
                                        color = Colors.Gray,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = seedPhrase[i - 1],
                                        textAlign = TextAlign.Start,
                                        style = Styles.phrase,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1.2f)) // It looks better to add a little weight here
                    }
                } else {
                    Text (
                        text = seedPhrase[0].breakMiddle(),
                        style = Styles.address
                    )
                }
                JumboButtons(
                    mainText = stringResource(R.string.btn_done),
                    mainClicked = { mainClicked(nav) },
                    bottomSpacing = 58
                )
            }
        }
        if (alertExists) { // Alert is recomposed for some reason each frame when scrolling
            Alert(enabled = alertShown == 1,
                titleText = stringResource(R.string.sure_done),
                mainText = stringResource(R.string.write_words_too_fast),
                buttons = if (mdl.seedPhraseWarningShown) intArrayOf(
                    R.string.btn_skip, R.string.btn_ok_sorry
                ) else intArrayOf(R.string.btn_ok_sorry),
                clickHandler = { id ->
                    alertClickHandler(id, nav)
                })
        }
    }

}
