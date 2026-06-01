package com.ureka.play4change

import android.app.Application
import com.ureka.play4change.background.BackgroundFetchWorker
import com.ureka.play4change.background.WorkManagerSetup
import com.ureka.play4change.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MainApplication)
            workManagerFactory()
            modules(appModule)
        }
        WorkManagerSetup.scheduleBackgroundFetch(this)
    }
}
