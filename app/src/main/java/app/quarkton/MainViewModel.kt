package app.quarkton

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import app.quarkton.db.DAppItem
import app.quarkton.ton.extensions.secureGenerate
import app.quarkton.ton.now
import app.quarkton.ton.nowms
import app.quarkton.ui.screens.BaseScreen
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.Mnemonics.WordCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.ton.crypto.SecureRandom
import org.ton.mnemonic.Mnemonic
import java.util.Observable

class MainViewModel : ViewModel() {

    private lateinit var per: Persistence

    private val _startupSplitPosition = MutableStateFlow(0.4F)
    val startupSplitPosition: StateFlow<Float> = _startupSplitPosition

    var developmentMode = false
    var devModeWent = false

    var seedPhraseShown = 0L
    var seedPhraseWarningShown = false
    var setupIsImporting = false

    private val _alertShown = MutableStateFlow(0)
    val alertShown: StateFlow<Int> = _alertShown

    private val _alertExists = MutableStateFlow(false)
    val alertExists: StateFlow<Boolean> = _alertExists

    val bioHardware = MutableStateFlow(true)
    val bioAvailable = MutableStateFlow(true)
    val onbUseBiometrics = MutableStateFlow(true)

    var nextScreen: BaseScreen? = null
    var justLocking = false

    var updatingPasscode = false

    val nextSettingsTitle = MutableStateFlow("")
    var nextSettingsScreen: BaseScreen? = null
    var nextSettingsAction: (() -> Unit)? = null
    var nextSettingsAllowFP: Boolean = false

    var getOverToSendNow: Boolean = false
    var sendingToAddress: String = ""
    var sendingAmount: Long = 0
    var sendingComment: String = ""
    var sendAllBalance: Boolean = false

    var qrFromAddrScr: Boolean = false

    val tcPendingCR = MutableStateFlow<Pair<String, String>?>(null)
    var tcPendingItem = MutableStateFlow<DAppItem?>(null)
    var tcWantedCItems = mapOf<String, String>()

    val nowRealtime = flow {
        while (true) {
            emit(nowms())
            delay(10L)
        }
    }

    val nowPrecise = flow {
        while (true) {
            emit(nowms())
            delay(100L)
        }
    }

    val nowRelaxed = flow {
        while (true) {
            emit(now() * 1000L)
            delay(1000L)
        }
    }.distinctUntilChanged()

    fun setPersistance(p: Persistence) { if (!::per.isInitialized) per = p }
    fun transitionToCreation() { _startupSplitPosition.value = 0f }
    fun transitionToLockScreen() { _startupSplitPosition.value = 1f}

    fun transitionDevMode() {
        if (!developmentMode) return
        _startupSplitPosition.value = 0.25f
    }

    fun delayedSetWarningShown() {
        viewModelScope.launch {
            delay(500)
            seedPhraseWarningShown = true
        }
    }

    fun showAlert(id: Int = 1, waitDuration: Long = 32L) {
        _alertExists.value = true
        viewModelScope.launch {
            delay(waitDuration) // To fix animation not playing
            _alertExists.value = true // Just to be sure
            _alertShown.value = id
        }
    }

    fun hideAlert(animDuration: Long = 500L) {
        _alertShown.value = 0
        viewModelScope.launch {
            delay(animDuration)
            if (_alertShown.value == 0) // Anti race
                _alertExists.value = false
        }
    }

    // *********************************************************************************************

    // Transient seed phrase during setup phase
    private var seedPhrase: List<String> = emptyList()

    var testNumbers: Array<Int> = arrayOf(1, 2, 3)

    fun generateSeedPhrase(words: Int = Mnemonic.DEFAULT_WORD_COUNT, onComplete: (success: Boolean) -> Unit) {
        seedPhrase = emptyList()
        viewModelScope.launch {
            seedPhrase = if (words < 0) {
                MnemonicCode(when (words) {
                    -12 -> WordCount.COUNT_12
                    -18 -> WordCount.COUNT_18
                    -24 -> WordCount.COUNT_24
                    else -> throw Error("Invalid words value")
                }).words.map { String(it) }
            } else {
                // ~~FIXED~~: Potentially unsafe RNG - only 64 bits of entropy
                //            (24 words = 256 bits!). Now using rolling SecureRandom for words
                Mnemonic.secureGenerate(wordCount = words)
            }
        }.invokeOnCompletion {
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(it == null)
            }
        }
    }

    fun clearSeedPhrase() {
        seedPhrase = emptyList()
    }

    fun randomizeTestNumbers() {
        testNumbers = (Array(seedPhrase.size) { it + 1 })
        testNumbers.shuffle(SecureRandom)
        testNumbers = testNumbers.sliceArray(0..2).sortedArray()
    }

    fun fillRealisticLookingButInvalidSeedForPreviewInAndroidStudio() {
        val words = Mnemonic.mnemonicWords()
        seedPhrase = List(24) { words[(7 + (it + 77) * 777) % 2048] }
    }

    fun seedPhraseWord(i: Int): String = seedPhrase[i]
    fun checkSeedPhraseWord(i: Int, s: String): Boolean = seedPhrase[i] == s
    fun getSeedPhrase(): List<String> = seedPhrase
    fun importSeedPhrase(newSeedPhrase: List<String>) { seedPhrase = newSeedPhrase }

    fun finishSetup(passcode: String, completed: () -> Unit, failed: () -> Unit) {
        if (per.performSetup(seedPhrase, onbUseBiometrics.value, passcode)) {
            seedPhrase = emptyList()
            completed()
        } else {
            failed()
        }
    }

    fun sendReset() {
        getOverToSendNow = false
        sendingToAddress = ""
        sendingAmount = 0
        sendingComment = ""
        sendAllBalance = false
    }

    // *********************************************************************************************



}