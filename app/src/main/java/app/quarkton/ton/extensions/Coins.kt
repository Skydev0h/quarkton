package app.quarkton.ton.extensions

import app.quarkton.extensions.fromBalance
import app.quarkton.extensions.fromBalanceRelaxed
import org.ton.block.Coins

fun Coins.Companion.of(str: String): Coins {
    return ofNano(Long.fromBalance(str))
}

fun Coins.Companion.ofRelaxed(str: String): Coins {
    return ofNano(Long.fromBalanceRelaxed(str))
}