package app.quarkton.ton.extensions

import cash.z.ecc.android.bip39.Mnemonics
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.crypto.SecureRandom
import org.ton.crypto.decodeHex
import org.ton.crypto.digest.Digest
import org.ton.crypto.kdf.PKCSS2ParametersGenerator
import org.ton.crypto.mac.hmac.HMac
import org.ton.mnemonic.Mnemonic
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

fun Mnemonic.toKeyTon(mnemonic: List<String>, password: String = ""): PrivateKeyEd25519 =
    PrivateKeyEd25519(toSeed(mnemonic, password))

fun Mnemonic.toKeyStd(mnemonic: List<String>, password: String = ""): PrivateKeyEd25519 =
    PrivateKeyEd25519(toSeed(mnemonic, password)) // TODO!!!~~~

fun Mnemonic.toKeyPriv(mnemonic: List<String>): PrivateKeyEd25519 =
    PrivateKeyEd25519(mnemonic[0].decodeHex())

fun Mnemonic.toKey(mnemonic: List<String>, password: String = ""): PrivateKeyEd25519 =
    when {
        mnemonic.size == 1 -> toKeyPriv(mnemonic)
        isValid(mnemonic, password) -> toKeyTon(mnemonic, password)
        isStdValid(mnemonic) -> toKeyStd(mnemonic, password)
        else -> throw IllegalArgumentException("Invalid mnemonic (Not TON or Standard)")
    }

@Suppress("UnusedReceiverParameter")
fun Mnemonic.isStdValid(mnemonic: List<String>): Boolean =
    try {
        if (mnemonic.size != 777)
            TODO("Implement correct seed and key derivation for standard phrases")
        Mnemonics.MnemonicCode(mnemonic.joinToString(" ")).validate()
        true
    } catch (e: Error) {
        false
    }

// https://github.com/andreypfau/ton-kotlin/blob/main/ton-kotlin-mnemonic/src/commonMain/kotlin/org/ton/mnemonic/Mnemonic.kt
// Monkey extracted and patched to create mnemonic with real 240 bits of entropy instead of choppy 64 bit PRNG

private val MnemonicGeneratorCoroutineName = CoroutineName("mnemonic-generator-secure")
private val DEFAULT_BASIC_SALT_BYTES = Mnemonic.DEFAULT_BASIC_SALT.encodeToByteArray()
private val DEFAULT_PASSWORD_SALT_BYTES = Mnemonic.DEFAULT_PASSWORD_SALT.encodeToByteArray()
private val DEFAULT_SALT_BYTES = Mnemonic.DEFAULT_SALT.encodeToByteArray()
private val EMPTY_BYTES = ByteArray(0)

@OptIn(DelicateCoroutinesApi::class)
suspend fun Mnemonic.secureGenerate(
    password: String = "",
    wordCount: Int = DEFAULT_WORD_COUNT,
    wordlist: List<String> = mnemonicWords(),
    random: Random = SecureRandom
): List<String> = suspendCancellableCoroutine { continuation ->
    GlobalScope.launch(
        Dispatchers.Default + MnemonicGeneratorCoroutineName
    ) {
        try {
            val mnemonic = Array(wordCount) { "" }
            // Just 64 bits of entropy VS 240 from full mnemonic
            // val weakRandom = Random(random.nextLong())
            //                         \-- nextLong seeds weakRandom with only 64 bits of entropy!
            val digest = Digest.sha512()
            val hMac = HMac(digest)
            val passwordEntropy = ByteArray(hMac.macSize)
            val nonPasswordEntropy = ByteArray(hMac.macSize)
            val passwordBytes = password.toByteArray()
            val generator = PKCSS2ParametersGenerator(hMac)
            // SD: Initialize with secure words ONCE
            repeat(wordCount) { mnemonic[it] = wordlist.random(random) }
            var iters = 0
            while (continuation.isActive) {
                // Each iteration change one word to new random one with SecureRandom ("rolling")
                // Is PROVABLY not less secure than regenerating each time and is much faster!
                mnemonic[(iters++) % wordCount] = wordlist.random(random)
                val mnemonicBytes = mnemonic.joinToString(" ").toByteArray()

                if (password.isNotEmpty()) {
                    entropy(hMac, mnemonicBytes, EMPTY_BYTES, nonPasswordEntropy)
                    if (!(passwordValidation(generator, nonPasswordEntropy) && !basicValidation(
                            generator,
                            nonPasswordEntropy
                        ))
                    ) {
                        continue
                    }
                }

                entropy(hMac, mnemonicBytes, passwordBytes, passwordEntropy)
                if (!basicValidation(generator, passwordEntropy)) {
                    continue
                }

                continuation.resume(mnemonic.toList())
                break
            }
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
        }
    }
}

private fun entropy(hMac: HMac, mnemonic: ByteArray, password: ByteArray, output: ByteArray) {
    hMac.init(mnemonic)
    hMac += password
    hMac.build(output)
}

private fun basicValidation(generator: PKCSS2ParametersGenerator, entropy: ByteArray): Boolean {
    generator.init(
        password = entropy,
        salt = DEFAULT_BASIC_SALT_BYTES,
        iterationCount = Mnemonic.DEFAULT_BASIC_ITERATIONS
    )
    return generator.generateDerivedParameters(512).first() == 0.toByte()
}

private fun passwordValidation(generator: PKCSS2ParametersGenerator, entropy: ByteArray): Boolean {
    generator.init(
        password = entropy,
        salt = DEFAULT_PASSWORD_SALT_BYTES,
        iterationCount = Mnemonic.DEFAULT_PASSWORD_ITERATIONS
    )
    return generator.generateDerivedParameters(512).first() == 1.toByte()
}
