package com.ancient.wenyan.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ancient.wenyan.data.db.dao.CardStateDao
import com.ancient.wenyan.data.db.dao.DailyRecordDao
import com.ancient.wenyan.data.db.dao.ReviewLogDao
import com.ancient.wenyan.data.db.entities.CardStateEntity
import com.ancient.wenyan.data.db.entities.DailyRecordEntity
import com.ancient.wenyan.data.db.entities.ReviewLogEntity

@Database(
    entities = [
        CardStateEntity::class,
        ReviewLogEntity::class,
        DailyRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardStateDao(): CardStateDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun dailyRecordDao(): DailyRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wenyan_recitation.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
    }
}
