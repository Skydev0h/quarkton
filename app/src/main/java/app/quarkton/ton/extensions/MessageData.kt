package app.quarkton.ton.extensions

import org.ton.cell.CellBuilder
import org.ton.contract.wallet.MessageData

fun MessageData.Companion.comment(comment: String): MessageData =
    raw(CellBuilder.createCell {
        storeUInt(0, 32) // op = 0
        storeBytes(comment.encodeToByteArray())
    })