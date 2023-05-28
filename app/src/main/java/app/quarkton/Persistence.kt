package app.quarkton

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import app.quarkton.db.AppDatabase
import app.quarkton.ton.DataMaster
import app.quarkton.ton.extensions.isStdValid
import org.ton.mnemonic.Mnemonic
import kotlin.properties.Delegates

@Suppress("UNUSED_PARAMETER")
class Persistence(context: Context, db: AppDatabase) {

    companion object {
        private const val SP_SEED = "seedPhrase"
        private const val SP_BIO  = "useBiometric"
        private const val SP_PC   = "passCode"
        private const val SP_LSPL = "lockScreenLen"
    }

    private val masterKey: String by lazy { MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC) }
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "config", masterKey, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun startup() {

    }

    fun performSetup(seed: List<String>, bio: Boolean, passcode: String): Boolean {
        if (seed.size == 1) {
            // Private key
            if (seed[0].length != 64) return false
            if (!seed[0].all { c -> c.isDigit() or (c.lowercaseChar() in 'a'..'f') })
                return false
        } else {
            if (seed.size < 12) return false
            if (seed.size > 24) return false
            if (seed.size % 6 != 0) return false
            // 12, 18, 24
            if (!Mnemonic.isValid(seed) && !Mnemonic.isStdValid(seed)) {
                Log.w("Persistence", "Invalid seed phrase")
                return false
            }
        }
        if ((passcode.length != 4) && (passcode.length != 6)) return false
        with (prefs.edit()) {
            putString(SP_SEED, seed.joinToString(" "))
            putBoolean(SP_BIO, bio)
            putString(SP_PC, passcode)
            putInt(SP_LSPL, passcode.length)
            return commit()
        }
    }

    fun isSetUp(): Boolean =
        prefs.contains(SP_SEED) and prefs.contains(SP_BIO) and prefs.contains(SP_PC)

    fun hardReset(): Boolean = prefs.edit().clear().commit()

    fun bioEnabled(): Boolean = prefs.getBoolean(SP_BIO, false)

    fun bioChange(newEnabled: Boolean): Boolean =
        prefs.edit().putBoolean(SP_BIO, newEnabled).commit()

    fun verifyPasscode(passcode: String): Boolean =
        passcode == prefs.getString(SP_PC, "undefined")

    fun updatePasscode(newPasscode: String): Boolean =
        prefs.edit().putString(SP_PC, newPasscode).commit()

    fun getSeedPhrase(): List<String>? = prefs.getString(SP_SEED, null)?.split(" ")

    fun getAll(): MutableMap<String, *>? =
        if (!BuildConfig.DEBUG) null else prefs.all // DEBUG ONLY (InspectorScreen)

    @SuppressLint("ApplySharedPref")
    fun lockScreenSetPassLength(n: Int): Boolean =
        if ((n != 4) && (n != 6)) false
        else prefs.edit().putInt(SP_LSPL, n).commit()

    fun lockScreenGetPassLength(): Int = prefs.getInt(SP_LSPL, 4)

    var showMyInRecents: Boolean by Delegates.observable(
        prefs.getBoolean("showMyInRecent", false)
    ) { _, _, newValue -> prefs.edit().putBoolean("showMyInRecent", newValue).apply() }

    var advancedFeatures: Boolean by Delegates.observable(
        prefs.getBoolean("advancedFeatures", false)
    ) { _, _, newValue -> prefs.edit().putBoolean("advancedFeatures", newValue).apply() }

    var selectedCurrency: String by Delegates.observable(
        prefs.getString("selectedCurrency", null) ?: "USD"
    ) { _, _, newValue -> prefs.edit().putString("selectedCurrency", newValue).apply() }

    var selectedExplorer: String by Delegates.observable(
        prefs.getString("selectedExplorer", null) ?: "tonscan"
    ) { _, _, newValue -> prefs.edit().putString("selectedExplorer", newValue).apply() }

    var reqAuthForSend: Boolean by Delegates.observable(
        prefs.getBoolean("reqAuthForSend", false)
    ) { _, _, newValue -> prefs.edit().putBoolean("reqAuthForSend", newValue).apply() }

    var cachedDNSRoot: String by Delegates.observable(
        prefs.getString("cachedDNSRoot", null) ?: ""
    ) { _, _, newValue -> prefs.edit().putString("cachedDNSRoot", newValue).apply() }

    var tcBridge: String by Delegates.observable(
        prefs.getString("tcBridge", null) ?: DataMaster.TC_DEFAULT_BRIDGE
    ) { _, _, newValue -> prefs.edit().putString("tcBridge", newValue).apply() }

}