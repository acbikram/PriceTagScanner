package com.pricetag.scanner.domain.repository

import com.pricetag.scanner.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun saveSettings(settings: AppSettings)
}
