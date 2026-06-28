package com.controlcom.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "controlcom_settings")

class SettingsRepository(private val context: Context) {

    private val pcIpKey = stringPreferencesKey("pc_ip")
    private val pcPortKey = intPreferencesKey("pc_port")
    private val tokenKey = stringPreferencesKey("token")
    private val onboardingCompletedKey = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            pcIp = prefs[pcIpKey].orEmpty(),
            pcPort = prefs[pcPortKey] ?: 7847,
            token = prefs[tokenKey].orEmpty(),
            onboardingCompleted = prefs[onboardingCompletedKey] ?: false
        )
    }

    suspend fun saveSettings(pcIp: String, pcPort: Int) {
        context.dataStore.edit { prefs ->
            prefs[pcIpKey] = pcIp.trim()
            prefs[pcPortKey] = pcPort
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = completed
        }
    }

    fun createApi(settings: AppSettings): ControlComApi {
        val baseUrl = "http://${settings.pcIp}:${settings.pcPort}/"
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        if (settings.token.isNotBlank()) {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${settings.token}")
                    .build()
                chain.proceed(request)
            }
            clientBuilder.addInterceptor(authInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ControlComApi::class.java)
    }
}

data class AppSettings(
    val pcIp: String,
    val pcPort: Int,
    val token: String,
    val onboardingCompleted: Boolean = false
) {
    val isConfigured: Boolean get() = pcIp.isNotBlank() && pcPort > 0
    val isReady: Boolean get() = isConfigured && token.isNotBlank()
}
