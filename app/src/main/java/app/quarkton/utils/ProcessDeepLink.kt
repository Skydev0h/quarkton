package app.quarkton.utils

import android.net.Uri
import android.util.Log
import org.ton.api.pub.PublicKeyEd25519
import org.ton.bitstring.BitString
import org.ton.block.AddrStd

fun processDeepLink(u: Uri,
                    onTonResult: (addr: String, amount: Long, comment: String) -> Unit,
                    onTCResult: (id: String, request: String) -> Unit) {
    Log.i("processDeepLink", u.toString())
    if (u.scheme == "tc") {
        processTonConnectLink(u, onTCResult)
        return
    }
    if (u.scheme != "ton")
        throw IllegalArgumentException("Invalid scheme ${u.scheme}")
    val act = u.host ?: ""
    if (act == "transfer") {
        val addr = u.pathSegments[0]
        if (addr.endsWith(".ton")) {
            // EXPLICIT ton://transfer/foundation.ton link is supported
            // Just plain text "foundation.ton" can be too vague
            // Must be in 33..126, 127 is delete (backspace)
            if (!addr.all { (it >= 33.toChar()) && (it <= 126.toChar())
                        && (it != '/') && (it != '?') && (it != '&') && (it != '%') // also forbid url special characters
            })
                throw java.lang.IllegalArgumentException("Domain name contains forbidden characters")
        } else
            AddrStd(addr) // throws if address is invalid
        var amount = 0L
        var comment = ""
        u.queryParameterNames.forEach { k ->
            val v = u.getQueryParameter(k) ?: return@forEach
            if (k == "amount")
                amount = v.toLong()
            if (k == "text")
                comment = v
        }
        onTonResult(addr, amount, comment)
    } else
        throw IllegalArgumentException("Method $act not supported")
}

fun processTonConnectLink(u: Uri,
                          onTCResult: (id: String, request: String) -> Unit) {
    if (u.scheme != "tc")
        throw IllegalArgumentException("Invalid scheme ${u.scheme}")
    if (u.queryParameterNames.size == 1 && u.queryParameterNames.contains("ret"))
        return // It will be unsafe to handle empty deeplinks but do not error out on those
    val ver = u.getQueryParameter("v")
    if (ver != "2")
        throw IllegalArgumentException("Invalid version $ver")
    val id = u.getQueryParameter("id") ?: throw IllegalArgumentException("No id provided")
    if (id.length != 64)
        throw java.lang.IllegalArgumentException("Bad id value")
    val r = u.getQueryParameter("r") ?: throw IllegalArgumentException("No request")
    onTCResult(id, r)
}