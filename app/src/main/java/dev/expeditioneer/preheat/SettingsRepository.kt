package dev.expeditioneer.preheat

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Thin wrapper around the Preferences-DataStore that backs the app's
 * persisted settings. The DataStore is seeded from the legacy
 * "MyPrefsFile" SharedPreferences via SharedPreferencesMigration on
 * first access (see MyApp.dataStore), so existing user configuration
 * carries over without intervention.
 *
 * The reads are Flow-based; the writes are suspend. Synchronous get*
 * helpers wrap a .first() collect so initialisation code in ViewModels
 * can use them inside a single coroutine.
 */
internal class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    fun stringFlow(key: String, default: String): Flow<String> = dataStore.data
        .map { it[stringPreferencesKey(key)] ?: default }
        .distinctUntilChanged()

    fun booleanFlow(key: String, default: Boolean): Flow<Boolean> = dataStore.data
        .map { it[booleanPreferencesKey(key)] ?: default }
        .distinctUntilChanged()

    fun intFlow(key: String, default: Int): Flow<Int> = dataStore.data
        .map { it[intPreferencesKey(key)] ?: default }
        .distinctUntilChanged()

    fun longFlow(key: String, default: Long): Flow<Long> = dataStore.data
        .map { it[longPreferencesKey(key)] ?: default }
        .distinctUntilChanged()

    suspend fun getString(key: String, default: String): String =
        stringFlow(key, default).first()

    suspend fun getBoolean(key: String, default: Boolean): Boolean =
        booleanFlow(key, default).first()

    suspend fun getInt(key: String, default: Int): Int =
        intFlow(key, default).first()

    suspend fun setString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun setInt(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    suspend fun setLong(key: String, value: Long) {
        dataStore.edit { it[longPreferencesKey(key)] = value }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
