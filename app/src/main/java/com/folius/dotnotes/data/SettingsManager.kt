package com.folius.dotnotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val THEME_PREF = stringPreferencesKey("theme_pref")
        val ANIMATIONS_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("animations_enabled")
        val STORAGE_URI = stringPreferencesKey("storage_uri")
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { it[API_KEY] }
    val modelId: Flow<String> = context.dataStore.data.map { it[MODEL_ID] ?: "google/gemini-flash-1.5" }
    val themePref: Flow<String> = context.dataStore.data.map { it[THEME_PREF] ?: "System" }
    val isAnimationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[ANIMATIONS_ENABLED] ?: true }
    val storageUri: Flow<String?> = context.dataStore.data.map { it[STORAGE_URI] }

    suspend fun saveSettings(apiKey: String, modelId: String, theme: String, animationsEnabled: Boolean) {
        context.dataStore.edit {
            it[API_KEY] = apiKey
            it[MODEL_ID] = modelId
            it[THEME_PREF] = theme
            it[ANIMATIONS_ENABLED] = animationsEnabled
        }
    }

    suspend fun saveStorageUri(uri: String?) {
        context.dataStore.edit {
            if (uri == null) {
                it.remove(STORAGE_URI)
            } else {
                it[STORAGE_URI] = uri
            }
        }
    }
}
