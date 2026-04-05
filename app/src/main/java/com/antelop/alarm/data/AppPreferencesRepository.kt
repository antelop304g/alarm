package com.antelop.alarm.data

import androidx.datastore.core.DataStore
import com.antelop.alarm.model.AlertSettings
import com.antelop.alarm.model.AppPreferences
import com.antelop.alarm.model.KeywordEntry
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferencesRepository(
    private val dataStore: DataStore<AppPreferences>,
) {
    val preferences: Flow<AppPreferences> = dataStore.data
    val keywords: Flow<List<KeywordEntry>> = preferences.map { it.keywords }
    val alertSettings: Flow<AlertSettings> = preferences.map { it.alertSettings }

    suspend fun addKeyword(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        dataStore.updateData { prefs ->
            prefs.copy(
                keywords = prefs.keywords + KeywordEntry(
                    id = UUID.randomUUID().toString(),
                    value = trimmed,
                    enabled = true,
                ),
            )
        }
    }

    suspend fun updateKeyword(id: String, value: String) {
        dataStore.updateData { prefs ->
            prefs.copy(
                keywords = prefs.keywords.map { entry ->
                    if (entry.id == id) entry.copy(value = value.trim()) else entry
                },
            )
        }
    }

    suspend fun toggleKeyword(id: String, enabled: Boolean) {
        dataStore.updateData { prefs ->
            prefs.copy(
                keywords = prefs.keywords.map { entry ->
                    if (entry.id == id) entry.copy(enabled = enabled) else entry
                },
            )
        }
    }

    suspend fun deleteKeyword(id: String) {
        dataStore.updateData { prefs ->
            prefs.copy(keywords = prefs.keywords.filterNot { it.id == id })
        }
    }

    suspend fun updateAlertSettings(transform: (AlertSettings) -> AlertSettings) {
        dataStore.updateData { prefs ->
            prefs.copy(alertSettings = transform(prefs.alertSettings))
        }
    }

    suspend fun setDefaultSmsApp(isDefault: Boolean) {
        dataStore.updateData { prefs ->
            prefs.copy(appState = prefs.appState.copy(isDefaultSmsApp = isDefault))
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        dataStore.updateData { prefs ->
            prefs.copy(appState = prefs.appState.copy(setupCompleted = completed))
        }
    }

    suspend fun setLastMatchedFingerprint(fingerprint: String?) {
        dataStore.updateData { prefs ->
            prefs.copy(appState = prefs.appState.copy(lastMatchedMessageFingerprint = fingerprint))
        }
    }
}
