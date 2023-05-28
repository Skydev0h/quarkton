package app.quarkton.db

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "dapps")
data class DAppItem(
    @PrimaryKey()
    val id: String,
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
    suspend fun get(id: String): DAppItem?

    @Query("SELECT * FROM dapps ORDER BY lastAct DESC")
    suspend fun getAll(): List<DAppItem>

    @Query("SELECT * FROM dapps WHERE active = 1 LIMIT 1")
    fun getActive(): DAppItem?

    @Query("SELECT * FROM dapps ORDER BY lastAct DESC LIMIT 1")
    suspend fun getLastActive(): DAppItem?

    @Query("SELECT * FROM dapps WHERE id = :id")
    fun observe(id: String): LiveData<DAppItem?>

    @Query("SELECT * FROM dapps ORDER BY lastAct DESC")
    fun observeAll(): LiveData<List<DAppItem>>

    @Query("SELECT * FROM dapps WHERE active = 1 LIMIT 1")
    fun observeCurrent(): LiveData<DAppItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(wallet: DAppItem): Long

    @Query("UPDATE dapps SET active = (id == :id)")
    suspend fun setCurrent(id: String): Int

    @Query("UPDATE dapps SET lastAct = max(lastAct, :lastAct) WHERE id = :id")
    suspend fun updateLastAct(id: String, lastAct: Long): Int

    @Query("UPDATE dapps SET lastAct = max(lastEvent, :lastEvent) WHERE id = :id")
    suspend fun updateLastEvent(id: String, lastEvent: Long): Int

    @Query("UPDATE dapps SET closed = 1 WHERE id = :id")
    suspend fun disconnect(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(wallet: List<DAppItem>): List<Long>

    @Query("DELETE FROM dapps")
    suspend fun deleteAll(): Int
}
