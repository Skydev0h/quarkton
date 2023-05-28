package app.quarkton.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "rates")
data class RateItem(
    @PrimaryKey()
    val name: String,
    val updated: Long,
    val rate: Double,
    val sortidx: Int
)

@Dao
interface RateDao {
    @Query("SELECT * FROM rates WHERE name = :name")
    suspend fun get(name: String): RateItem?

    @Query("SELECT * FROM rates ORDER BY sortidx ASC")
    suspend fun getAll(): List<RateItem>

    @Query("SELECT MIN(updated) FROM rates")
    suspend fun getMinUpdated(): Long?

    @Query("SELECT * FROM rates WHERE name = :name")
    fun observe(name: String): LiveData<RateItem?>

    @Query("SELECT * FROM rates ORDER BY sortidx ASC")
    fun observeAll(): LiveData<List<RateItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(name: RateItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(name: List<RateItem>): List<Long>

    @Query("DELETE FROM rates")
    suspend fun deleteAll(): Int
}
