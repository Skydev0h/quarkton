package app.quarkton.extensions

import org.ton.block.AddrStd
import org.ton.block.MsgAddress

fun MsgAddress.toRepr() = (this as AddrStd).toString(userFriendly = true)
fun MsgAddress.toRaw() = (this as AddrStd).toString(userFriendly = false)
