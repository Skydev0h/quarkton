package app.quarkton.db

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import app.quarkton.extensions.toRepr
import app.quarkton.ton.Wallet
import app.quarkton.ton.extensions.toRepr
import app.quarkton.ton.nowms

@Entity(tableName = "wallets")
data class WalletItem(
    @PrimaryKey
    val verRev: Int,
    val address: String,
    val balance: Long,
    val updated: Long,
    @ColumnInfo(index = true)
    val current: Boolean,
    val lastTxId: String = ""
) {
    companion object {
        fun fromWallet(wal: Wallet, current: Boolean, updated: Long? = null) =
            WalletItem(
                verRev = if (wal.workchainId == -1) -wal.verRev else wal.verRev,
                address = wal.address.toRepr(),
                balance = wal.data.value.balance?.amount?.toLong() ?: 0L,
                updated = updated ?: nowms(),
                current = current,
                lastTxId = wal.data.value.fullState?.lastTransactionId?.toRepr() ?: "@"
            )
    }
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE verRev = :verRev")
    suspend fun get(verRev: Int): WalletItem?

    @Query("SELECT * FROM wallets ORDER BY ABS(verRev) DESC")
    suspend fun getAll(): List<WalletItem>

    @Query("SELECT * FROM wallets WHERE current = 1 LIMIT 1")
    fun getCurrent(): WalletItem?

    @Query("SELECT * FROM wallets WHERE address = :address")
    suspend fun getByAddress(address: String): WalletItem?

    @Query("SELECT * FROM wallets WHERE verRev = :verRev")
    fun observe(verRev: Int): LiveData<WalletItem?>

    @Query("SELECT * FROM wallets ORDER BY ABS(verRev) DESC")
    fun observeAll(): LiveData<List<WalletItem>>

    @Query("SELECT * FROM wallets WHERE current = 1 LIMIT 1")
    fun observeCurrent(): LiveData<WalletItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(wallet: WalletItem): Long

    @Query("UPDATE wallets SET current = (verRev == :verRev)")
    suspend fun setCurrent(verRev: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(wallet: List<WalletItem>): List<Long>

    @Query("DELETE FROM wallets")
    suspend fun deleteAll(): Int
}
