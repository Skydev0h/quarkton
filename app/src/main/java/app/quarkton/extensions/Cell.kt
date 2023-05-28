package app.quarkton.extensions

import org.ton.block.Either
import org.ton.cell.Cell
import org.ton.tlb.CellRef
import java.math.BigInteger

fun<T> Either<T, CellRef<T>>.toVal(): T? = x ?: y?.value

fun Cell.comment(): String? {
    val cs = beginParse()
    if (cs.remainingBits >= 32) {
        // Canonical comment, op = 0x00000000. Realign remainingBits if broken in this case
        if (cs.preloadUInt(32) == BigInteger.ZERO) {
            cs.skipBits(32)
            if (cs.remainingBits == 0) return ""
            return cs.loadBits((cs.remainingBits / 8) * 8).toByteArray().decodeToString() //.delNL()
        }
    }
    // For other cases require correct amount of bits to form bytes
    if (cs.remainingBits % 8 != 0)
        return null
    if (cs.remainingBits >= 8) {
        // Short comment, op = 0x00, check that all other chars are printable
        if (cs.preloadUInt(8) == BigInteger.ZERO) {
            cs.skipBits(8)
            if (cs.remainingBits == 0) return ""
            val arr = cs.loadBits(cs.remainingBits).toByteArray()
            return if (arr.all { it.isPrintable() }) arr.decodeToString() /*.delNL()*/ else null
        }
    }
    if (cs.remainingBits == 0) return null
    // Last resort. Show comment if ALL body characters are printable
    val arr = cs.loadBits(cs.remainingBits).toByteArray()
    return if (arr.all { it.isPrintable() }) arr.decodeToString() /*.delNL()*/ else null
}