package app.quarkton.db

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.quarkton.extensions.comment
import app.quarkton.extensions.toRepr
import app.quarkton.extensions.toVal
import app.quarkton.ton.extensions.ZERO_TX
import app.quarkton.ton.extensions.toRepr
import app.quarkton.ton.now
import org.ton.block.AddrStd
import org.ton.block.IntMsgInfo
import org.ton.block.TrPhaseComputeVm
import org.ton.block.TransOrd
import org.ton.lite.client.internal.TransactionId
import org.ton.lite.client.internal.TransactionInfo

@Entity(tableName = "transactions", primaryKeys = ["id", "acc"])
data class TransItem(
    @ColumnInfo(index = true)
    val id: String,
    @ColumnInfo(index = true)
    val acc: String,
    val src: String? = null,
    val incmt: String? = null,
    val inamt: Long? = null,
    // There may be maximum of 4 output messages
    //     from all possible wallets (upto 4 refs)
    // First output message
    val dst: String? = null,
    val cmt: String? = null,
    val amt: Long? = null,
    // Second output message
    val dst1: String? = null,
    val cmt1: String? = null,
    val amt1: Long? = null,
    // Third output message
    val dst2: String? = null,
    val cmt2: String? = null,
    val amt2: Long? = null,
    // Fourth output message
    val dst3: String? = null,
    val cmt3: String? = null,
    val amt3: Long? = null,
    // End of output messages
    val now: Long,
    @ColumnInfo(index = true)
    val lt: Long,
    val totalFee: Long,
    val storFee: Long,
    val actFee: Long,
    val compFee: Long,
    val fwdFee: Long,
    val actOk: Boolean,
    val compOk: Boolean,
    val prevId: String
) {
    companion object {
        fun fromTransactionInfo(ti: TransactionInfo, workchainId: Int): TransItem {
            val t = ti.transaction.value
            val d = t.description.value as? TransOrd
            val c = d?.computePh as? TrPhaseComputeVm

            val inmsg = with (t.r1.value.inMsg.value?.value) {
                Pair(this?.info as? IntMsgInfo, this?.body?.toVal()) }
            val outmsgs = t.r1.value.outMsgs.map { p ->
                with(p.second.value) { Pair(info as? IntMsgInfo, body.toVal()) } }
            val outs = (0 .. 3).map { outmsgs.getOrNull(it) }
            /*
            Log.i("Transaction", "ID: " + ti.id.toRepr())
            Log.i("Transaction", "TransactionInfo:\n$t")
            Log.i("Transaction", "InMsg:\n" + (inmsg.first?.toString() ?: "NULL"))
            for (i in 0..3)
                Log.i("Transaction", "OutMsg[$i]:\n" + (outs[i]?.first?.toString() ?: "NULL"))
            Log.i("Transaction", "TransOrd:\n" + (d?.toString() ?: "NULL"))
            Log.i("Transaction", "ComputePh:\n" + (c?.toString() ?: "NULL"))
            */
            return TransItem(
                id = ti.id.toRepr(),
                acc = AddrStd(workchainId, t.accountAddr).toRepr(),
                src = inmsg.first?.src?.toRepr(),
                dst = outs[0]?.first?.dest?.toRepr(),
                dst1 = outs[1]?.first?.dest?.toRepr(),
                dst2 = outs[2]?.first?.dest?.toRepr(),
                dst3 = outs[3]?.first?.dest?.toRepr(),
                incmt = inmsg.second?.comment(),
                cmt = outs[0]?.second?.comment(),
                cmt1 = outs[1]?.second?.comment(),
                cmt2 = outs[2]?.second?.comment(),
                cmt3 = outs[3]?.second?.comment(),
                inamt = inmsg.first?.value?.coins?.amount?.toLong(),
                amt = outs[0]?.first?.value?.coins?.amount?.toLong(),
                amt1 = outs[1]?.first?.value?.coins?.amount?.toLong(),
                amt2 = outs[2]?.first?.value?.coins?.amount?.toLong(),
                amt3 = outs[3]?.first?.value?.coins?.amount?.toLong(),
                now = t.now.toLong(),
                lt = ti.id.lt,
                totalFee = t.totalFees.coins.amount.toLong(),
                storFee = d?.storagePh?.value?.storageFeesCollected?.amount?.toLong() ?: 0,
                actFee = d?.action?.value?.value?.totalActionFees?.value?.amount?.toLong() ?: 0,
                compFee = c?.gasFees?.amount?.toLong() ?: 0,
                fwdFee = d?.action?.value?.value?.totalFwdFees?.value?.amount?.toLong() ?: 0,
                actOk = d?.action?.value?.value?.success ?: true,
                compOk = c?.success ?: true,
                prevId = TransactionId(t.prevTransHash, t.prevTransLt.toLong()).toRepr()
            )
        }

        fun pendingTransaction(args: Triple<String, Long, String>, failed: Boolean = false): TransItem {
            return TransItem(
                id = ZERO_TX,
                acc = ZERO_ADDR,
                src = null,
                dst = args.first,
                dst1 = null,
                dst2 = null,
                dst3 = null,
                incmt = null,
                cmt = args.third.ifEmpty { null },
                cmt1 = null,
                cmt2 = null,
                cmt3 = null,
                inamt = null,
                amt = args.second,
                amt1 = null,
                amt2 = null,
                amt3 = null,
                now = now(),
                lt = Long.MAX_VALUE,
                totalFee = 0L,
                storFee = 0L,
                actFee = 0L,
                compFee = 0L,
                fwdFee = 0L,
                actOk = !failed,
                compOk = !failed,
                prevId = ZERO_TX
            )
        }
    }
}

@Dao
interface TransDao {
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: String): TransItem?

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransItem>

    @Query("SELECT * FROM transactions WHERE acc = :address ORDER BY lt DESC")
    suspend fun getByAccount(address: String): List<TransItem>

    @Query("SELECT * FROM transactions WHERE acc = :address ORDER BY lt DESC LIMIT 1")
    suspend fun getLastByAccount(address: String): TransItem?

    @Query("SELECT * FROM transactions WHERE acc = :address ORDER BY lt ASC LIMIT 1")
    suspend fun getFirstByAccount(address: String): TransItem?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observe(id: String): LiveData<TransItem?>

    @Query("SELECT * FROM transactions")
    fun observeAll(): LiveData<List<TransItem>>

    @Query("SELECT * FROM transactions WHERE acc = :address ORDER BY lt DESC")
    fun observeByAccount(address: String): LiveData<List<TransItem>>

    @Query("SELECT * FROM transactions WHERE acc = :address ORDER BY lt DESC LIMIT 1")
    fun observeLastByAccount(address: String): LiveData<TransItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(transaction: TransItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(transaction: List<TransItem>): List<Long>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int
}
