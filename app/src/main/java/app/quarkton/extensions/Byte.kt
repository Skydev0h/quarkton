package app.quarkton.extensions

fun Byte.isPrintable(): Boolean {
    return (this == 10.toByte()) ||
           (this == 13.toByte()) ||
           ((this >= 32.toByte()) &&
            (this <= 127.toByte()))
}