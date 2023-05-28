package app.quarkton.ui.screens.other

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.onboarding.DoneScreen
import app.quarkton.ui.screens.onboarding.ImportWalletScreen
import app.quarkton.ui.screens.onboarding.NoPhraseScreen
import app.quarkton.ui.screens.onboarding.TestPassedScreen
import app.quarkton.ui.screens.onboarding.TestTimeScreen
import app.quarkton.ui.screens.onboarding.WalletCreatedScreen
import app.quarkton.ui.screens.onboarding.WelcomeScreen
import app.quarkton.ui.screens.settings.ChooseWalletScreen
import app.quarkton.ui.screens.settings.SettingsScreen
import app.quarkton.ui.screens.settings.TokensListScreen
import app.quarkton.ui.screens.wallet.MainWalletScreen
import app.quarkton.ui.screens.wallet.QRScanScreen
import app.quarkton.ui.screens.wallet.SendAddressScreen
import app.quarkton.ui.screens.wallet.TransferSentScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.Styles
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.navigator.Navigator

class StartupScreen : BaseScreen() {

    @Preview @Composable fun P() { Preview() }

    private fun animEnd(it: Float, nav: Navigator?) {
        if (it < 0.01) {
            act.updateStatusBar(false)
            if (mdl.developmentMode)
                nav?.push(WelcomeScreen())
            else
                nav?.replace(WelcomeScreen())
        }
        if (it > 0.99) {
            nav?.replace(LockScreen())
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun decideNavigation(nav: Navigator?) {
        if (per.isSetUp())
            mdl.transitionToLockScreen()
        else
            mdl.transitionToCreation()
    }

    @Composable
    override fun Content() {
        Init()
        LifecycleEffect(onStarted = {
            try {
                dm.updateGlobalConfigInBackground()
                dm.updateCurrenciesInBackground()
            } catch (e: Throwable) {
                Log.w("StartupScreen", "Failed to initially update global config or currencies")
            }
//            if (mdl.developmentMode) {
//                if (!mdl.devModeWent) {
//                    nav?.push(MainWalletScreen())
//                    mdl.devModeWent = true
//                }
//            }
        })

        val screens = remember {
            arrayOf(
                LockScreen::class.java,
                RecoveryPhraseScreen::class.java,
                SetPasscodeScreen::class.java,
                WelcomeScreen::class.java,
                WalletCreatedScreen::class.java,
                TestTimeScreen::class.java,
                TestPassedScreen::class.java,
                ImportWalletScreen::class.java,
                NoPhraseScreen::class.java,
                DoneScreen::class.java,
            )
        }

        val walscreens = remember {
            arrayOf(
                MainWalletScreen::class.java,
                SendAddressScreen::class.java,
                TransferSentScreen::class.java,
                QRScanScreen::class.java
            )
        }

        val setscreens = remember {
            arrayOf(
                SettingsScreen::class.java,
                ChooseWalletScreen::class.java,
                TokensListScreen::class.java
            )
        }

        var navDecided by remember { mutableStateOf(false) }

        val splitPosition by animateFloatAsState(
            label = "splitPosition",
            targetValue = mdl.startupSplitPosition.collectAsStateWithLifecycle().value,
            finishedListener = { animEnd(it, nav) },
            animationSpec = tween(300, 150)
            // Brief delay and longer duration so that to match intro animation in requirements
        )

        Surface(
            modifier = Modifier.fillMaxSize(), color = Color.Black
        ) {
            if (mdl.developmentMode) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TextButton(
                        onClick = {
                            dm.clearDatabase()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        shape = Styles.buttonShape
                    ) {
                        Text(
                            text = "Clear database",
                            style = Styles.mainText, color = Color.Red
                        )
                    }
                    TextButton(
                        onClick = {
                            nav?.push(InspectorScreen())
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        shape = Styles.buttonShape
                    ) {
                        Text(
                            text = "Persistence data inspector",
                            style = Styles.mainText, color = Color.Green
                        )
                    }
                    TextButton(
                        onClick = {
                            nav?.push(DebugScreen())
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        shape = Styles.buttonShape
                    ) {
                        Text(
                            text = "Debugging screen",
                            style = Styles.mainText, color = Color.Yellow
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight(splitPosition)
                        .fillMaxWidth()
                )
                val cs = if (splitPosition > 0.001) 10.dp else 0.dp
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = RoundedCornerShape(cs, cs)
                ) {
                    if (mdl.developmentMode) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TextButton(
                                onClick = { decideNavigation(nav) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth(),
                                shape = Styles.buttonShape
                            ) {
                                Text(
                                    text = "Normal flow (decide navigation)",
                                    style = Styles.mainText, color = Colors.BalGreen
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)) {
                                    for (s in screens) {
                                        TextButton(
                                            onClick = { nav?.push(s.newInstance()) },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .fillMaxWidth(),
                                            shape = Styles.buttonShape
                                        ) {
                                            Text(
                                                text = s.simpleName.replace("Screen", ""),
                                                style = Styles.mainText, color = Colors.Primary
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)) {
                                    for (s in walscreens) {
                                        TextButton(
                                            onClick = { nav?.push(s.newInstance()) },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .fillMaxWidth(),
                                            shape = Styles.buttonShape,
                                        ) {
                                            Text(
                                                text = s.simpleName.replace("Screen", ""),
                                                style = Styles.mainText, color = Color.Red
                                            )
                                        }
                                    }
                                    for (s in setscreens) {
                                        TextButton(
                                            onClick = { nav?.push(s.newInstance()) },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .fillMaxWidth(),
                                            shape = Styles.buttonShape
                                        ) {
                                            Text(
                                                text = s.simpleName.replace("Screen", ""),
                                                style = Styles.mainText, color = Color.Magenta
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!navDecided) {
            navDecided = true
            SideEffect {
                if (!mdl.developmentMode)
                    this.decideNavigation(nav)
                else
                    mdl.transitionDevMode()
            }
        }
    }

}