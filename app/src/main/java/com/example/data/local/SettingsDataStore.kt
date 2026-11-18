package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.BusinessSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "caja_cun_settings"
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val BUSINESS_TYPE = stringPreferencesKey("business_type")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val DAILY_SALES_GOAL = longPreferencesKey("daily_sales_goal_cop")
    }

    val settingsFlow: Flow<BusinessSettings> = context.settingsDataStore.data.map { prefs ->
        BusinessSettings(
            businessName = prefs[Keys.BUSINESS_NAME]?.takeIf { it.isNotBlank() } ?: "Mi Negocio",
            businessType = prefs[Keys.BUSINESS_TYPE]?.takeIf { it.isNotBlank() } ?: "Comercio / Microempresa",
            ownerName = prefs[Keys.OWNER_NAME] ?: "",
            dailySalesGoalCop = prefs[Keys.DAILY_SALES_GOAL] ?: 0L,
            currencyCode = "COP"
        )
    }

    suspend fun updateSettings(settings: BusinessSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BUSINESS_NAME] = settings.businessName.trim().ifBlank { "Mi Negocio" }
            prefs[Keys.BUSINESS_TYPE] = settings.businessType.trim().ifBlank { "Comercio / Microempresa" }
            prefs[Keys.OWNER_NAME] = settings.ownerName.trim()
            prefs[Keys.DAILY_SALES_GOAL] = settings.dailySalesGoalCop.coerceAtLeast(0L)
        }
    }

    suspend fun resetSettings() {
        context.settingsDataStore.edit { it.clear() }
    }
}
