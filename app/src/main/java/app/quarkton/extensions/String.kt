package app.quarkton.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import app.quarkton.ui.theme.Styles

fun String.formatBalance(decimalSize: TextUnit = Styles.bigBalanceDeciSize): AnnotatedString {
    val d = indexOf('.')
    if (d == -1)
        return AnnotatedString(this)
    val b = AnnotatedString.Builder(this)
    b.addStyle(SpanStyle(fontSize = decimalSize), d, b.length)
    return b.toAnnotatedString()
}

fun String.breakMiddle(): String {
    if (length < 2) return this
    val h = length / 2
    return substring(0, h) + "\n" + substring(h)
}

fun String.shortAddr(): String =
    if (length > 8) substring(0, 4) + "…" + substring(length - 4) else this

fun String.delNL(): String = this.replace("\n","").replace("\r","")

fun String.appendStyled(s: String, ss: SpanStyle): AnnotatedString {
    return AnnotatedString.Builder(this + s).let { b ->
        b.addStyle(ss, this.length, b.length)
        b.toAnnotatedString()
    }
}

fun String.prependStyled(s: String, ss: SpanStyle): AnnotatedString {
    return AnnotatedString.Builder(s + this).let { b ->
        b.addStyle(ss, 0, s.length)
        b.toAnnotatedString()
    }
}