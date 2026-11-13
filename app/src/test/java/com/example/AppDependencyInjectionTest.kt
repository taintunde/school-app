package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ResultViewModel
import com.example.di.AppViewModelProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDependencyInjectionTest {

    @Test
    fun applicationContainerProvidesSingletonRoomDatabaseAndRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context is SmartResultApplication)

        val containerA = SmartResultApplication.containerFrom(context)
        val containerB = SmartResultApplication.containerFrom(context)
        assertSame(containerA, containerB)

        val dbFromContainer = containerA.database
        val dbFromStaticSingleton = AppDatabase.getDatabase(context)
        assertSame(dbFromStaticSingleton, dbFromContainer)

        assertSame(containerA.studentDao, containerB.studentDao)
        assertSame(containerA.academicResultDao, containerB.academicResultDao)
        assertSame(containerA.resultRepository, containerB.resultRepository)
    }

    @Test
    fun viewModelFactoryInjectsResultRepositoryIntoResultViewModel() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val extras = MutableCreationExtras().apply {
            set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, application)
        }

        val viewModel = AppViewModelProvider.Factory.create(ResultViewModel::class.java, extras)
        assertNotNull(viewModel)
    }
}
