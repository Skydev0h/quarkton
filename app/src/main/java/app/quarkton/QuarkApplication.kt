package app.quarkton

import android.app.Application
import androidx.room.Room
import app.quarkton.db.AppDatabase
import app.quarkton.ton.DataMaster

class QuarkApplication : Application() {

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "quark-db"
        ).fallbackToDestructiveMigration().build()
    }

    val persistence by lazy {
        Persistence(applicationContext, database)
    }

    val dataMaster by lazy {
        DataMaster(applicationContext, database, persistence)
    }

    val crossDataModel by lazy {
        CrossDataModel(applicationContext)
    }

    companion object {
        lateinit var app: QuarkApplication
    }

    override fun onCreate() {
        super.onCreate()
        app = this
    }

}