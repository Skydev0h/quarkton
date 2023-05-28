package app.quarkton

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.quarkton.ui.LocalBFC
import app.quarkton.ui.MyScreenTransition
import app.quarkton.ui.screens.BaseScreen
import app.quarkton.ui.screens.other.LockScreen
import app.quarkton.ui.screens.other.NotSetupScreen
import app.quarkton.ui.screens.other.StartupScreen
import app.quarkton.ui.screens.wallet.MainWalletScreen
import app.quarkton.ui.theme.Colors
import app.quarkton.ui.theme.QuarkTONWalletTheme
import app.quarkton.utils.processDeepLink
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


open class MainActivity : FragmentActivity() {

    val app by lazy { application as QuarkApplication }
    val mdl: MainViewModel by viewModels() // Localized to Activity
    val crossDataModel by lazy { app.crossDataModel } // Global
    val persistence by lazy { app.persistence }
    val database by lazy { app.database }
    val dataMaster by lazy { app.dataMaster }

    var deepLink by mutableStateOf<Uri?>(null)

    private var wantSecure: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStatusBar(true)
        mdl.setPersistance(persistence)
        // mdl.developmentMode = BuildConfig.DEBUG
        if (mdl.developmentMode)
            mdl.fillRealisticLookingButInvalidSeedForPreviewInAndroidStudio()
        @Suppress("DEPRECATION") // Just to make sure it applies in some strange situations
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        var startScreen: BaseScreen = StartupScreen()
        Log.i("AppActivity::onCreate", "Intent: $intent")
        if (intent != null && intent.action == "android.intent.action.VIEW") {
            val data = intent.data
            Log.i("MainActivity", "Received intent: $data")
            if (data != null) {
                try {
                    processDeepLink(data,
                        onTonResult = { addr, amount, comment ->
                            if (!persistence.isSetUp())
                                startScreen = NotSetupScreen()
                            else {
                                mdl.sendReset() // incl. sendAllBalance
                                mdl.sendingToAddress = addr
                                mdl.sendingAmount = amount
                                mdl.sendingComment = comment
                                mdl.getOverToSendNow = true
                            }
                        },
                        onTCResult = { id, request ->
                            if (!persistence.isSetUp())
                                startScreen = NotSetupScreen()
                            else {
                                mdl.tcPendingCR.value = Pair(id, request)
                            }
                        }
                    )
                } catch (e: Throwable) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.the_provided_link_is_invalid_or_corrupted),
                        Toast.LENGTH_LONG
                    ).show()
                    this.finish()
                }
            }
        }
        setContent {
            // Back Fix Crutch - recreates BackHandler shortly after Resuming activity
            val bfc = remember { mutableStateOf(false) }
            val crs = rememberCoroutineScope()
            DisposableEffect(true) {
                val obs = LifecycleEventObserver {
                        _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE)
                        bfc.value = false
                    if (event == Lifecycle.Event.ON_RESUME) {
                        crs.launch {
                            delay(10)
                            bfc.value = true
                        }
                    }
                }
                lifecycle.addObserver(obs)
                onDispose { lifecycle.removeObserver(obs) }
            }

            QuarkTONWalletTheme {
                CompositionLocalProvider(LocalBFC provides bfc) {
                    Navigator(startScreen) { nav ->
                        MyScreenTransition(nav) { screen ->
                            screen.Content()
                        }

                        LaunchedEffect(deepLink) {
                            val dl = deepLink ?: return@LaunchedEffect
                            deepLink = null // prevent repeated delivery
                            if (!persistence.isSetUp()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.please_complete_setup_first_and_then_try_again,
                                    Toast.LENGTH_LONG
                                ).show()
                                return@LaunchedEffect
                            }

                            try {
                                processDeepLink(dl,
                                    onTonResult = { addr, amount, comment ->
                                        mdl.sendReset() // incl. sendAllBalance
                                        mdl.sendingToAddress = addr
                                        mdl.sendingAmount = amount
                                        mdl.sendingComment = comment
                                        mdl.getOverToSendNow = true
                                    },
                                    onTCResult = { id, request ->
                                        mdl.tcPendingCR.value = Pair(id, request)
                                    }
                                )
                            } catch (e: Throwable) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.the_provided_link_is_invalid_or_corrupted),
                                    Toast.LENGTH_LONG
                                ).show()
                                return@LaunchedEffect
                            }

                            // Just go to MainWalletScreen, potentially recreating it
                            // If on DoneScreen or any other post-main screen it works correctly

                            // This bypass could be terrible if not checked...
                            if ((nav.lastItem !is LockScreen) && (nav.lastItem !is StartupScreen))
                                nav.replaceAll(MainWalletScreen())
                        }
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    Log.i("MainActivity", "Periodical refresher started")
                    dataMaster.periodicalRefresher()
                } catch (e: CancellationException) {
                    Log.i("MainActivity", "Periodical refresher stopped")
                } catch (e: Throwable) {
                    Log.e("MainActivity", "Periodical refresher crashed", e)
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    Log.i("MainActivity", "TON Connect looper started")
                    dataMaster.tonConnectLooper()
                } catch (e: CancellationException) {
                    Log.i("MainActivity", "TON Connect looper stopped")
                } catch (e: Throwable) {
                    Log.e("MainActivity", "TON Connect looper crashed", e)
                }
            }
        }
        persistence.startup()
        checkBiometrics()
    }

    override fun onResume() {
        super.onResume()
        checkBiometrics()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i("AppActivity::onNewIntent", "Intent: $intent")
        if (intent != null && intent.action == "android.intent.action.VIEW") {
            val data = intent.data
            deepLink = null // to re-trigger the event, it ignores null anyway
            deepLink = data
        }
    }

    fun updateStatusBar(black: Boolean, dim: Boolean = false): MainActivity {

        try {
            var col = if (dim) Colors.Gray else (if (black) Color.Black else Color.White)
            if (wantSecure && mdl.developmentMode) col = col.copy(col.alpha,
                max(0.7f, col.red), min(0.4f, col.green), min(0.4f, col.blue))
            window.statusBarColor = col.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                (!black) && (!dim)
        } catch (ex: Exception) {
            // To fix preview
        }
        return this
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun portraitOnly(): MainActivity {
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (ex: NullPointerException) {
            // To fix preview
        }
        return this
    }

    fun anyOrientation(): MainActivity {
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } catch (ex: NullPointerException) {
            // To fix preview
        }
        return this
    }

    fun secure(): MainActivity {
        wantSecure = true
        if (mdl.developmentMode) return this
        if (window != null) window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        return this
    }

    fun unsecure(): MainActivity {
        wantSecure = false
        if (window != null) window.setFlags(0, WindowManager.LayoutParams.FLAG_SECURE)
        return this
    }

    private fun checkBiometrics() {
        val res = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (res == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE || res == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            mdl.bioHardware.value = false
            mdl.bioAvailable.value = false
            mdl.onbUseBiometrics.value = false
            return
        }
        if (res == BiometricManager.BIOMETRIC_SUCCESS) {
            mdl.bioHardware.value = true
            mdl.bioAvailable.value = true
            mdl.onbUseBiometrics.value = true
            return
        }
        mdl.bioHardware.value = true
        mdl.bioAvailable.value = false
        mdl.onbUseBiometrics.value = false
    }

}
