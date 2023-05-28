package app.quarkton.ton

import org.ton.crypto.digest.Digest
import org.ton.crypto.mac.hmac.HMac

// import javax.crypto.Mac
// import javax.crypto.spec.SecretKeySpec

// const val HMAC_SHA512 = "HmacSHA512"

fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray =
    HMac(Digest.sha512(), key).apply{update(data)}.build()
//    Mac.getInstance(HMAC_SHA512)
//        .apply { init(SecretKeySpec(key, HMAC_SHA512)) }
//        .doFinal(data)
