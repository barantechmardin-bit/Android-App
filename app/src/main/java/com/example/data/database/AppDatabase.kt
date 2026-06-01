package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ErpDao
import com.example.data.model.*

@Database(
    entities = [
        Cari::class,
        ServisKayit::class,
        IslemTuru::class,
        StokParca::class,
        KasaBanka::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun erpDao(): ErpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teknik_servis_erp_db"
                )
                // In production might need a migration strategy, for MVP fallback to destructive migration is handy
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
