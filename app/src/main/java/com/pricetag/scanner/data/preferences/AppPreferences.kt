package com.pricetag.scanner.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.pricetag.scanner.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SERVER_IP        = stringKey("server_ip")
        val SERVER_PORT      = intKey("server_port")
        val AUTO_CONNECT     = booleanKey("auto_connect")
        val BEEP_ENABLED     = booleanKey("beep_enabled")
        val VIBRATE_ENABLED  = booleanKey("vibrate_enabled")
        val AUTO_SEND        = booleanKey("auto_send")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                serverIp       = prefs[Keys.SERVER_IP]       ?: "192.168.1.100",
                serverPort     = prefs[Keys.SERVER_PORT]      ?: 5000,
                autoConnect    = prefs[Keys.AUTO_CONNECT]     ?: true,
                beepEnabled    = prefs[Keys.BEEP_ENABLED]     ?: true,
                vibrateEnabled = prefs[Keys.VIBRATE_ENABLED]  ?: true,
                autoSendAfterScan = prefs[Keys.AUTO_SEND]     ?: false,
            )
        }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_IP]       = settings.serverIp
            prefs[Keys.SERVER_PORT]     = settings.serverPort
            prefs[Keys.AUTO_CONNECT]    = settings.autoConnect
            prefs[Keys.BEEP_ENABLED]    = settings.beepEnabled
            prefs[Keys.VIBRATE_ENABLED] = settings.vibrateEnabled
            prefs[Keys.AUTO_SEND]       = settings.autoSendAfterScan
        }
    }
}
