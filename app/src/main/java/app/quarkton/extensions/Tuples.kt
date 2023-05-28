package app.quarkton.extensions

infix fun <A, B, C> Pair<A, B>.with(that: C): Triple<A, B, C> = Triple(first, second, that)

private fun demonstration(): Triple<Long, String, String> {
    return 1_000_000_000L to "foundation.ton" with "Generous donation"
}