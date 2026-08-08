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
    private val FORCE_HEADPHONES_COLOR = booleanPreferencesKey("force_headphones_color")
    private val HEADPHONES_COLOR = stringPreferencesKey("headphones_color")

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

    fun forceHeadphonesColor(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { prefs -> prefs[FORCE_HEADPHONES_COLOR] ?: false }

    suspend fun setForceHeadphonesColor(context: Context, force: Boolean) {
        context.themeDataStore.edit { it[FORCE_HEADPHONES_COLOR] = force }
    }

    fun headphonesColor(context: Context): Flow<String> =
        context.themeDataStore.data.map { prefs -> prefs[HEADPHONES_COLOR] ?: "BLACK" }

    suspend fun setHeadphonesColor(context: Context, color: String) {
        context.themeDataStore.edit { it[HEADPHONES_COLOR] = color }
    }
}
