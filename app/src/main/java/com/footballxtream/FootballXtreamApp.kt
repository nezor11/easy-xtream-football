package com.footballxtream

import android.app.Application
import android.content.Context

class FootballXtreamApp : Application() {

    lateinit var container: AppContainer
        private set

    // Apply the saved language to the Application context so strings resolved off it (e.g. in
    // ViewModels) use the chosen language, not just the device default.
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
