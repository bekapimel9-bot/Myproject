package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamelingo_settings")

data class AppSettings(
    val sourceLang: String = "en",
    val targetLang: String = "id",
    val overlayDisplayMode: String = "IN_PLACE", // "IN_PLACE" or "CARD"
    val autoTranslateEnabled: Boolean = false,
    val autoIntervalMs: Long = 2500L,
    val minConfidence: Float = 0.60f,
    val strictOffline: Boolean = true,
    val showOriginalText: Boolean = true,
    val saveHistory: Boolean = true,
    val regionLeft: Float = 0.05f,
    val regionTop: Float = 0.60f,
    val regionRight: Float = 0.95f,
    val regionBottom: Float = 0.95f
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val OVERLAY_MODE = stringPreferencesKey("overlay_mode")
        val AUTO_TRANSLATE = booleanPreferencesKey("auto_translate")
        val AUTO_INTERVAL_MS = longPreferencesKey("auto_interval_ms")
        val MIN_CONFIDENCE = floatPreferencesKey("min_confidence")
        val STRICT_OFFLINE = booleanPreferencesKey("strict_offline")
        val SHOW_ORIGINAL = booleanPreferencesKey("show_original")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val REGION_LEFT = floatPreferencesKey("region_left")
        val REGION_TOP = floatPreferencesKey("region_top")
        val REGION_RIGHT = floatPreferencesKey("region_right")
        val REGION_BOTTOM = floatPreferencesKey("region_bottom")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            sourceLang = prefs[PreferencesKeys.SOURCE_LANG] ?: "en",
            targetLang = prefs[PreferencesKeys.TARGET_LANG] ?: "id",
            overlayDisplayMode = prefs[PreferencesKeys.OVERLAY_MODE] ?: "IN_PLACE",
            autoTranslateEnabled = prefs[PreferencesKeys.AUTO_TRANSLATE] ?: false,
            autoIntervalMs = prefs[PreferencesKeys.AUTO_INTERVAL_MS] ?: 2500L,
            minConfidence = prefs[PreferencesKeys.MIN_CONFIDENCE] ?: 0.60f,
            strictOffline = prefs[PreferencesKeys.STRICT_OFFLINE] ?: true,
            showOriginalText = prefs[PreferencesKeys.SHOW_ORIGINAL] ?: true,
            saveHistory = prefs[PreferencesKeys.SAVE_HISTORY] ?: true,
            regionLeft = prefs[PreferencesKeys.REGION_LEFT] ?: 0.05f,
            regionTop = prefs[PreferencesKeys.REGION_TOP] ?: 0.60f,
            regionRight = prefs[PreferencesKeys.REGION_RIGHT] ?: 0.95f,
            regionBottom = prefs[PreferencesKeys.REGION_BOTTOM] ?: 0.95f
        )
    }

    suspend fun updateLanguages(source: String, target: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SOURCE_LANG] = source
            prefs[PreferencesKeys.TARGET_LANG] = target
        }
    }

    suspend fun setOverlayDisplayMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.OVERLAY_MODE] = mode
        }
    }

    suspend fun setAutoTranslate(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_TRANSLATE] = enabled
        }
    }

    suspend fun setAutoIntervalMs(intervalMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_INTERVAL_MS] = intervalMs
        }
    }

    suspend fun setStrictOffline(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.STRICT_OFFLINE] = enabled
        }
    }

    suspend fun setShowOriginalText(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOW_ORIGINAL] = show
        }
    }

    suspend fun setSaveHistory(save: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SAVE_HISTORY] = save
        }
    }

    suspend fun updateRegion(left: Float, top: Float, right: Float, bottom: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REGION_LEFT] = left.coerceIn(0f, 1f)
            prefs[PreferencesKeys.REGION_TOP] = top.coerceIn(0f, 1f)
            prefs[PreferencesKeys.REGION_RIGHT] = right.coerceIn(0f, 1f)
            prefs[PreferencesKeys.REGION_BOTTOM] = bottom.coerceIn(0f, 1f)
        }
    }
}
