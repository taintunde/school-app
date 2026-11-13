package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Student::class,
        SubjectScore::class,
        AcademicResult::class,
        SchoolSettings::class,
        Subject::class,
        SchoolClass::class,
        GradingScaleConfig::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun subjectScoreDao(): SubjectScoreDao
    abstract fun academicResultDao(): AcademicResultDao
    abstract fun schoolSettingsDao(): SchoolSettingsDao
    abstract fun subjectDao(): SubjectDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun gradingScaleDao(): GradingScaleDao

    companion object {
        const val DATABASE_NAME = "smart_result_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
