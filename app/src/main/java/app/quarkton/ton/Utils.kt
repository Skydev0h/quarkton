package app.quarkton.ton

import app.quarkton.QuarkApplication
import kotlinx.datetime.Clock

fun now(): Long = nowms() / 1000
fun nowms(): Long = Clock.System.now().toEpochMilliseconds()

fun supportedExplorers() = mapOf(
    "tonscan" to "TONScan",
    /*
    TODO: Add support for more explorers
    "tonapi"  to "TON API",
    "toncx"   to "TON Scan",
    "whales"  to "Ton Whales",
    "dton"    to "dTon"
    */
)

fun makeExplorerLink(address: String? = null, transaction: String? = null, explorer: String? = null): String {
    return when (explorer ?: QuarkApplication.app.persistence.selectedExplorer) {
        "tonscan" ->
            when {
                transaction != null -> "https://tonscan.org/tx/$transaction"
                address     != null -> "https://tonscan.org/address/$address"
                else                -> "https://tonscan.org/"
            }
        else -> ""
    }
}