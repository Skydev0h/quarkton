package app.quarkton.ton

import com.ditchoom.buffer.ByteOrder
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.allocate
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.crypto.digest.Digest
import org.ton.crypto.kdf.PKCSS2ParametersGenerator
import org.ton.mnemonic.Mnemonic

// https://github.com/ton-core/ton-crypto/blob/master/src/mnemonic/mnemonic.ts
// https://github.com/ton-core/ton-crypto/blob/master/src/hd/mnemonics.ts

class HDKey(val key: ByteArray, val chainCode: ByteArray) {

    companion object {

        const val HD_KEYS_SEED = "TON HD Keys seed"
        val HD_KEYS_BYTES = HD_KEYS_SEED.encodeToByteArray()

        const val MNEMONICS_SEED = "TON Mnemonics HD seed"
        val MNEMONICS_BYTES = MNEMONICS_SEED.encodeToByteArray()

        const val HARDENED_OFFSET = 0x80000000

        fun seedToMasterKey(hdSeed: ByteArray): HDKey =
            with(hmacSha512(MNEMONICS_BYTES, hdSeed)) {
                HDKey(sliceArray(0 until 32), sliceArray(32 until 64))
            }

    }

    operator fun get(vararg indexes: Long): HDKey {
        var k = this
        indexes.forEach {
            k = k.deriveHardenedKey(it)
        }
        return k
    }

    fun toPK(): PrivateKeyEd25519 {
        return PrivateKeyEd25519(key)
    }

    fun deriveHardenedKey(index: Long): HDKey {
        if (index >= HARDENED_OFFSET)
            throw IllegalArgumentException("Key index must be less than the offset")
        val buf = PlatformBuffer.allocate(1 + 32 + 4, byteOrder = ByteOrder.BIG_ENDIAN)
        buf.writeByte(0)
        buf.writeBytes(key)
        buf.writeUInt((index + HARDENED_OFFSET).toUInt())
        buf.resetForRead()
        return with (hmacSha512(chainCode, buf.readByteArray(1 + 32 + 4))) {
            HDKey(sliceArray(0 until 32), sliceArray(32 until 64))
        }
    }

}

fun Mnemonic.toHDSeed(mnemonic: List<String>, password: String = ""): ByteArray =
    with(
        PKCSS2ParametersGenerator(
        digest = Digest.sha512(),
        password = toEntropy(mnemonic, password),
        salt = HDKey.HD_KEYS_BYTES,
        iterationCount = DEFAULT_ITERATIONS
    )
    ) {
        generateDerivedParameters(512).sliceArray(0 until 32)
    }