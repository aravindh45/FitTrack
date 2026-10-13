package com.personal.fittrack

import android.app.Application
import com.personal.fittrack.core.AppContainer

class FitTrackApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
