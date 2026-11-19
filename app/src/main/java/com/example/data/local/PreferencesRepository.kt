package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.xireDataStore: DataStore<Preferences> by preferencesDataStore(name = "xire_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AGE = intPreferencesKey("user_age")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    val userProfile: Flow<UserProfile> = context.xireDataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.USER_NAME].orEmpty(),
            age = prefs[Keys.USER_AGE] ?: 0
        )
    }

    val appSettings: Flow<AppSettings> = context.xireDataStore.data.map { prefs ->
        AppSettings(
            maxTokens = (prefs[Keys.MAX_TOKENS] ?: 512).coerceIn(32, 4096),
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            selectedModelName = prefs[Keys.SELECTED_MODEL].orEmpty()
        )
    }

    suspend fun saveProfile(name: String, age: Int) {
        context.xireDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().take(40)
            prefs[Keys.USER_AGE] = age.coerceIn(1, 120)
        }
    }

    suspend fun saveSettings(name: String, age: Int, maxTokens: Int) {
        context.xireDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().take(40)
            prefs[Keys.USER_AGE] = age.coerceIn(1, 120)
            prefs[Keys.MAX_TOKENS] = maxTokens.coerceIn(32, 4096)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.xireDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.key
        }
    }

    suspend fun setSelectedModelName(name: String) {
        context.xireDataStore.edit { prefs ->
            prefs[Keys.SELECTED_MODEL] = name
        }
    }
}
