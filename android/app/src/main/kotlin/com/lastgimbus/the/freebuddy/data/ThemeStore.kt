package com.lastgimbus.the.freebuddy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "settings")

/** User-selected app theme, persisted via DataStore Preferences. */
object ThemeStore {
    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private val KEY = stringPreferencesKey("theme_mode")
    private val SHOW_EMOJIS = booleanPreferencesKey("show_emojis")

    fun themeMode(context: Context): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            runCatching { ThemeMode.valueOf(prefs[KEY] ?: "") }.getOrDefault(ThemeMode.SYSTEM)
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.themeDataStore.edit { it[KEY] = mode.name }
    }

    fun showEmojis(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { prefs -> prefs[SHOW_EMOJIS] ?: true }

    suspend fun setShowEmojis(context: Context, show: Boolean) {
        context.themeDataStore.edit { it[SHOW_EMOJIS] = show }
    }
}
