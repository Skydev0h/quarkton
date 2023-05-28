package app.quarkton.ui.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.quarkton.CrossDataModel
import cafe.adriel.voyager.androidx.AndroidScreen
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import app.quarkton.MainActivity
import app.quarkton.MainViewModel
import app.quarkton.Persistence
import app.quarkton.QuarkApplication
import app.quarkton.db.AppDatabase
import app.quarkton.ton.DataMaster
import app.quarkton.ui.LocalBFC
import app.quarkton.ui.theme.QuarkTONWalletTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseScreen : AndroidScreen() {

    companion object;

    @Transient
    protected lateinit var act: MainActivity

    @Transient
    protected lateinit var app: QuarkApplication

    @Transient
    protected lateinit var mdl: MainViewModel

    @Transient
    protected lateinit var cdm: CrossDataModel

    @Transient
    protected lateinit var per: Persistence

    @Transient
    protected lateinit var db: AppDatabase

    @Transient
    protected lateinit var dm: DataMaster

    @Transient
    protected var nav: Navigator? = null

    @Transient
    protected var crs: CoroutineScope? = null

    protected var isDark: Boolean = false

    protected fun verb(s: String) = Log.v(this.javaClass.simpleName, s)
    protected fun info(s: String) = Log.i(this.javaClass.simpleName, s)
    protected fun warn(s: String) = Log.w(this.javaClass.simpleName, s)
    protected fun error(s: String) = Log.e(this.javaClass.simpleName, s)
    protected fun fatal(s: String) = Log.wtf(this.javaClass.simpleName, s)

    fun showAlert(id: Int = 1, waitDuration: Long = 32L) {
        act.updateStatusBar(black = isDark, dim = true)
        mdl.showAlert(id, waitDuration)
    }

    fun hideAlert(animDuration: Long = 500L) {
        act.updateStatusBar(black = isDark, dim = false)
        mdl.hideAlert(animDuration)
    }

    @Composable fun Init(
        dark: Boolean = false,
        secure: Boolean? = false,
        portraitOnly: Boolean? = true,
        delaySecure: Long = 0L
    ) {
        @Suppress("SENSELESS_COMPARISON")
        // init is ALWAYS the first function called, it WILL initialize all required stuff
        if (!::act.isInitialized || act == null) {
            act = LocalContext.current as MainActivity
            app = act.application as QuarkApplication
            mdl = act.mdl
            cdm = app.crossDataModel
            per = app.persistence
            db = app.database
            dm = app.dataMaster
        }
        // val ctx = LocalContext.current
        nav = LocalNavigator.current
        val view = LocalView.current
        val fm = LocalFocusManager.current
        // crs = if (needCoroutineScope or (delaySecure != 0L)) rememberCoroutineScope() else null
        crs = rememberCoroutineScope()
        LifecycleEffect(onStarted = {
            /*
            // Fixed by reworking objects acquire logic
            // Not needed anymore
            @Suppress("SENSELESS_COMPARISON")
            if (act == null) {
                // Recreated from system settings change (camera disabled? resolution changed? etc)
                // Need to properly restart activity, voyager is too smart for my dumb logic
                val int = Intent(ctx, MainActivity::class.java)
                ctx.startActivity(int)
                (ctx as? Activity)?.finishAffinity() ?: Runtime.getRuntime().exit(0);
                return@LifecycleEffect
            }
            */
            when (portraitOnly) {
                true  -> act.portraitOnly()
                false -> act.anyOrientation()
                null  -> {}
            }
            if (delaySecure == 0L) {
                when (secure) {
                    true -> act.secure()
                    false -> act.unsecure()
                    null -> {}
                }
            } else {
                // Because of animation 3 words of seed might get leaked if recording, so anim delay
                crs?.launch {
                    delay(delaySecure)
                    when (secure) {
                        true -> act.secure()
                        false -> act.unsecure()
                        null -> {}
                    }
                }
            }
            act.updateStatusBar(dark)
            isDark = dark
        })
    }

    @Composable
    protected fun Preview() {
        QuarkTONWalletTheme {
            Content()
        }
    }

    @Composable
    protected fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
        if (LocalBFC.current.value)
            androidx.activity.compose.BackHandler(enabled, onBack)
    }

}