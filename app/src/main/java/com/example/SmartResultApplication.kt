package com.example

import android.app.Application
import android.content.Context
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer

/**
 * Application entry point holding the singleton [AppContainer] for app-wide data access.
 */
class SmartResultApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    companion object {
        fun containerFrom(context: Context): AppContainer {
            val app = context.applicationContext
            return if (app is SmartResultApplication) {
                app.container
            } else {
                DefaultAppContainer(app)
            }
        }
    }
}
