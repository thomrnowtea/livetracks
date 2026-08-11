package com.thomrnowtea.livetracks

import android.app.Application
import com.thomrnowtea.livetracks.data.FileProjectRepository
import com.thomrnowtea.livetracks.data.SharedPreferencesAppSettingsRepository

class LiveTracksApplication : Application() {
    val projectRepository by lazy {
        FileProjectRepository(filesDir.resolve("library.ltdata"))
    }

    val settingsRepository by lazy {
        SharedPreferencesAppSettingsRepository(this)
    }
}
