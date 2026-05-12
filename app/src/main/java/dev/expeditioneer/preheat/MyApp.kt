package dev.expeditioneer.preheat

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.dataStore by preferencesDataStore(name = "settings")

class MyApp : Application() {
    internal val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(dataStore)
    }
}
