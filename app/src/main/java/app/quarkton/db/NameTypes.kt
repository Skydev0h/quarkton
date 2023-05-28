package app.quarkton.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "names")
data class NameItem(
    @PrimaryKey()
    val name: String,
    @ColumnInfo(index = true)
    val address: String,
    val checked: Long
)

@Dao
interface NameDao {
    @Query("SELECT * FROM names WHERE name = :name")
    suspend fun get(name: String): NameItem?

    @Query("SELECT * FROM names WHERE address = :address")
    suspend fun getByAddress(address: String): NameItem?

    @Query("SELECT * FROM names")
    suspend fun getAll(): List<NameItem>

    @Query("SELECT * FROM names WHERE name = :name")
    fun observe(name: String): LiveData<NameItem?>

    @Query("SELECT * FROM names WHERE address = :address")
    fun observeByAddress(address: String): LiveData<NameItem?>

    @Query("SELECT * FROM names")
    fun observeAll(): LiveData<List<NameItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(name: NameItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(name: List<NameItem>): List<Long>

    @Query("DELETE FROM names")
    suspend fun deleteAll(): Int
}
