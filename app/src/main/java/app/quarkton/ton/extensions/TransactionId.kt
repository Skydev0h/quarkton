package app.quarkton.ton.extensions

import org.ton.bitstring.BitString
import org.ton.lite.client.internal.TransactionId

const val ZERO_TX = "0000000000000000000000000000000000000000000000000000000000000000@0"

fun TransactionId.toRepr(): String {
    return hash.toHex() + "@" + lt.toString()
}

fun TransactionId.Companion.fromRepr(repr: String): TransactionId  {
    val (hash, lt) = repr.split('@')
    return TransactionId(BitString.of(hash), lt.toLong())
}