package com.antelop.alarm.data

import androidx.datastore.core.Serializer
import com.antelop.alarm.model.AppPreferences
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.Json

object AppPreferencesSerializer : Serializer<AppPreferences> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: AppPreferences = AppPreferences()

    override suspend fun readFrom(input: InputStream): AppPreferences {
        return try {
            json.decodeFromString(AppPreferences.serializer(), input.readBytes().decodeToString())
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppPreferences, output: OutputStream) {
        output.write(json.encodeToString(AppPreferences.serializer(), t).encodeToByteArray())
    }
}
