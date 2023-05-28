package app.quarkton.extensions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import app.quarkton.ui.theme.Styles
import org.ton.block.Coins

fun Long.formatBalance(decimals: Int = 4, decimalSize: TextUnit = Styles.bigBalanceDeciSize): AnnotatedString {
    val s = Coins.ofNano(this).toString()
    val d = s.indexOf('.')
    if (d == -1) return AnnotatedString(s)
    val t = s.substring(0, kotlin.math.min(s.length, d + 1 + decimals)).trimEnd('0').trimEnd('.')
    return t.formatBalance(decimalSize)
}

fun Long.simpleBalance(decimals: Int = 4): String {
    val s = Coins.ofNano(this).toString()
    val d = s.indexOf('.')
    if (d == -1) return s
    return s.substring(0, kotlin.math.min(s.length, d + 1 + decimals)).trimEnd('0').trimEnd('.')
}

// b.addStyle(SpanStyle(color = Colors.TextRed, background = Colors.TextRedBack), 5, 7)

fun Long.Companion.fromBalance(str: String): Long {
    val s = str.trim().trimStart('0')
    // Only 0-9, . (once) are allowed
    var metDot = false
    s.forEach {
        if (it == '.') {
            if (metDot)
                throw IllegalArgumentException("Only one . allowed in amount")
            metDot = true
        }
        else if (!it.isDigit())
            throw IllegalArgumentException("Only 0-9 and one . are allowed in amount")
    }
    var (ip, fp) = if (metDot) s.split('.') else listOf(s, "")
    if (fp.length > 9) fp = fp.substring(0 until 9)
    if (fp.length < 9) fp = fp.padEnd(9, '0')
    return (ip + fp).toLong()
}

fun Long.Companion.fromBalanceRelaxed(str: String): Long {
    val s = str.trim().trimStart('0')
    // Only 0-9, . (once) are allowed
    val sb = StringBuilder(str.length)
    var metDot = false
    s.forEach {
        if (it == '.') {
            if (!metDot)
                sb.append(it)
            metDot = true
        }
        if (it.isDigit())
            sb.append(it)
    }
    var (ip, fp) = if (metDot) s.split('.') else listOf(s, "")
    if (fp.length > 9) fp = fp.substring(0 until 9)
    if (fp.length < 9) fp = fp.padEnd(9, '0')
    return (ip + fp).toLong()
}