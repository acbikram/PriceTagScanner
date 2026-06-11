package com.pricetag.scanner.data.repository

import com.pricetag.scanner.data.preferences.AppPreferences
import com.pricetag.scanner.domain.model.AppSettings
import com.pricetag.scanner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefs: AppPreferences,
) : SettingsRepository {
    override fun getSettings(): Flow<AppSettings> = prefs.settings
    override suspend fun saveSettings(settings: AppSettings) = prefs.save(settings)
}
