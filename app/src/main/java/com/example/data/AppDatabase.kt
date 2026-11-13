package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Student::class, SubjectScore::class, SchoolSettings::class, Subject::class, SchoolClass::class, GradingScaleConfig::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun subjectScoreDao(): SubjectScoreDao
    abstract fun schoolSettingsDao(): SchoolSettingsDao
    abstract fun subjectDao(): SubjectDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun gradingScaleDao(): GradingScaleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_result_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
