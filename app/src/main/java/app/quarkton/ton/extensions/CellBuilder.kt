package app.quarkton.ton.extensions

import org.ton.cell.Cell
import org.ton.cell.CellBuilder

fun CellBuilder.storeEmbedded(cell: Cell): CellBuilder {
    storeBits(cell.bits)
    storeRefs(cell.refs)
    return this
}
