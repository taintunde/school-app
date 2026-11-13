package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.SmartResultApplication
import com.example.data.AcademicResultDao
import com.example.data.AppDatabase
import com.example.data.GradingScaleDao
import com.example.data.ResultRepository
import com.example.data.ResultViewModel
import com.example.data.SchoolClassDao
import com.example.data.SchoolSettingsDao
import com.example.data.StudentDao
import com.example.data.SubjectDao
import com.example.data.SubjectScoreDao

/**
 * Dependency injection container providing application-wide singleton instances
 * for the Room database, DAOs, and repositories.
 */
interface AppContainer {
    val database: AppDatabase
    val studentDao: StudentDao
    val subjectScoreDao: SubjectScoreDao
    val academicResultDao: AcademicResultDao
    val schoolSettingsDao: SchoolSettingsDao
    val subjectDao: SubjectDao
    val schoolClassDao: SchoolClassDao
    val gradingScaleDao: GradingScaleDao
    val resultRepository: ResultRepository
}

/**
 * Default runtime implementation of [AppContainer] backed by the singleton [AppDatabase].
 */
class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext: Context = context.applicationContext

    override val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }

    override val studentDao: StudentDao by lazy {
        database.studentDao()
    }

    override val subjectScoreDao: SubjectScoreDao by lazy {
        database.subjectScoreDao()
    }

    override val academicResultDao: AcademicResultDao by lazy {
        database.academicResultDao()
    }

    override val schoolSettingsDao: SchoolSettingsDao by lazy {
        database.schoolSettingsDao()
    }

    override val subjectDao: SubjectDao by lazy {
        database.subjectDao()
    }

    override val schoolClassDao: SchoolClassDao by lazy {
        database.schoolClassDao()
    }

    override val gradingScaleDao: GradingScaleDao by lazy {
        database.gradingScaleDao()
    }

    override val resultRepository: ResultRepository by lazy {
        ResultRepository(
            studentDao = studentDao,
            scoreDao = subjectScoreDao,
            academicResultDao = academicResultDao,
            settingsDao = schoolSettingsDao,
            subjectDao = subjectDao,
            schoolClassDao = schoolClassDao,
            gradingScaleDao = gradingScaleDao
        )
    }
}

/**
 * ViewModelProvider.Factory that injects [ResultRepository] from [AppContainer]
 * into [ResultViewModel].
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = checkNotNull(
                extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ) { "APPLICATION_KEY is required to instantiate ViewModels via AppViewModelProvider" }

            val container = (application as? SmartResultApplication)?.container
                ?: DefaultAppContainer(application)

            if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
                return ResultViewModel(application, container.resultRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
