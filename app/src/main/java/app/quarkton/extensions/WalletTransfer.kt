package app.quarkton.extensions

import android.util.Log
import app.quarkton.ton.Wallet
import org.ton.block.MessageRelaxed
import org.ton.cell.CellBuilder
import org.ton.contract.wallet.WalletTransfer
import org.ton.tlb.CellRef
import org.ton.tlb.constructor.AnyTlbConstructor
import org.ton.tlb.storeRef

fun WalletTransfer.measure(inclRoot: Boolean = true): Pair<Int, Int> {
    var bits = 0
    var cells = 0
    val msg = (CellBuilder.createCell {
        storeRef(MessageRelaxed.tlbCodec(AnyTlbConstructor), CellRef(Wallet.intMsg(this@measure)))
    })
    Log.w("WalletTransfer", "measure $msg")
    for (c in (if (inclRoot) msg else msg.beginParse().loadRef()).treeWalk()) {
        cells += 1
        bits += c.bits.size
        Log.i("WalletTransfer", "treeWalk $c")
    }
    return Pair(bits, cells)
}

