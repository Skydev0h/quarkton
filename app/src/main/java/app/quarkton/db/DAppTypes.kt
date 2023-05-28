package app.quarkton.db

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "dapps")
data class DAppItem(
    @PrimaryKey()
    val id: String,
    @ColumnInfo(index = true)
    val waladdr: String,
    val mykey: String,
    val manifest: String,
    val url: String,
    val name: String,
    val lastAct: Long,
    @ColumnInfo(index = true)
    val active: Boolean,
    val closed: Boolean,
    val iconUrl: String?,
    val touUrl: String?,
    val prpUrl: String?,
    val lastEvent: Long?
) {
    fun domain() = Uri.parse(url).host!!
}

@Dao
interface DAppDao {
    @Query("SELECT * FROM dapps WHERE id = :id")
    fun get(id: String): DAppItem?

    @Query("SELECT * FROM dapps ORDER BY lastAct DESC")
    fun getAll(): List<DAppItem>

    @Query("SELECT * FROM dapps WHERE waladdr = :waladdr ORDER BY lastAct DESC")
    fun getAllByWallet(waladdr: String): List<DAppItem>

    @Query("SELECT * FROM dapps WHERE active = 1 LIMIT 1")
    fun getActive(): DAppItem?

    @Query("SELECT waladdr FROM dapps WHERE active = 1 LIMIT 1")
    fun getActiveWallet(): String?

    @Query("SELECT * FROM dapps ORDER BY lastAct DESC LIMIT 1")
    fun getLastActive(): DAppItem?

    @Query("SELECT * FROM dapps WHERE id = :id")
    fun observe(id: String): LiveData<DAppItem?>

    @Query("SELECT * FROM dapps ORDER BY lastAct DESC")
    fun observeAll(): LiveData<List<DAppItem>>

    @Query("SELECT * FROM dapps WHERE active = 1 LIMIT 1")
    fun observeCurrent(): LiveData<DAppItem?>

    @Query("SELECT * FROM dapps WHERE waladdr = :waladdr ORDER BY lastAct DESC")
    fun observeAllByWallet(waladdr: String): LiveData<List<DAppItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(wallet: DAppItem): Long

    @Query("UPDATE dapps SET active = (id == :id)")
    suspend fun setCurrent(id: String): Int

    @Query("UPDATE dapps SET lastAct = max(ifnull(lastAct, -1), :lastAct) WHERE id = :id")
    suspend fun updateLastAct(id: String, lastAct: Long): Int

    @Query("UPDATE dapps SET lastEvent = max(ifnull(lastEvent, -1), :lastEvent) WHERE id = :id")
    suspend fun updateLastEvent(id: String, lastEvent: Long): Int

    @Query("UPDATE dapps SET closed = 1 WHERE id = :id")
    suspend fun disconnect(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(wallet: List<DAppItem>): List<Long>

    @Query("DELETE FROM dapps WHERE id = :id AND closed = 1")
    suspend fun deleteOneClosed(id: String): Int

    @Query("DELETE FROM dapps")
    suspend fun deleteAll(): Int
}
