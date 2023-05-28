package app.quarkton.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [
    WalletItem::class, TransItem::class, NameItem::class, RateItem::class, DAppItem::class
], version = 14, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun transDao(): TransDao
    abstract fun nameDao(): NameDao
    abstract fun rateDao(): RateDao
    abstract fun dappDao(): DAppDao
}