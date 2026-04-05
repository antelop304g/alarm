package com.antelop.alarm.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.antelop.alarm.AlarmApplication
import com.antelop.alarm.data.AppPreferencesRepository
import com.antelop.alarm.data.AppPreferencesSerializer
import com.antelop.alarm.data.SetupController
import com.antelop.alarm.data.SmsRepository
import com.antelop.alarm.domain.AlertPolicy
import com.antelop.alarm.domain.KeywordMatcher
import com.antelop.alarm.domain.MessageFingerprintFactory
import com.antelop.alarm.model.AppPreferences

class AppContainer(
    private val context: Context,
) {
    private val appPreferencesStore = DataStoreFactory.create(
        serializer = AppPreferencesSerializer,
        produceFile = { context.dataStoreFile("app_preferences.json") },
    )

    val preferencesRepository = AppPreferencesRepository(appPreferencesStore)
    val keywordMatcher = KeywordMatcher()
    val messageFingerprintFactory = MessageFingerprintFactory(keywordMatcher)
    val alertPolicy = AlertPolicy()
    val smsRepository = SmsRepository(context.contentResolver)
    val setupController = SetupController(context, preferencesRepository)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as AlarmApplication).container
