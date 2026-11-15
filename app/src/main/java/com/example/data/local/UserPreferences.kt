package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.DeltaRegion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "village_traders_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        private val KEY_DELTA_REGION = stringPreferencesKey("delta_region")
        private val KEY_TELEGRAM_HANDLE = stringPreferencesKey("telegram_handle")
        const val DEFAULT_TELEGRAM_HANDLE = "nirajthetrader"
    }

    val hasSeenWelcome: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_WELCOME] ?: false
    }

    val selectedRegion: Flow<DeltaRegion> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_DELTA_REGION]
        DeltaRegion.entries.firstOrNull { it.name == raw } ?: DeltaRegion.INDIA
    }

    val telegramHandle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TELEGRAM_HANDLE]?.takeIf { it.isNotBlank() } ?: DEFAULT_TELEGRAM_HANDLE
    }

    suspend fun setWelcomeSeen(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_WELCOME] = seen
        }
    }

    suspend fun setSelectedRegion(region: DeltaRegion) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DELTA_REGION] = region.name
        }
    }

    suspend fun setTelegramHandle(handle: String) {
        val cleaned = handle.trim().removePrefix("@").ifBlank { DEFAULT_TELEGRAM_HANDLE }
        context.dataStore.edit { prefs ->
            prefs[KEY_TELEGRAM_HANDLE] = cleaned
        }
    }
}
