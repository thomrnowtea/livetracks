package com.thomrnowtea.livetracks.data

import android.content.Context
import com.thomrnowtea.livetracks.domain.TrackType

enum class AppLanguage { SPANISH, ENGLISH }

data class AppSettings(
    val language: AppLanguage = AppLanguage.SPANISH,
    val keepScreenAwake: Boolean = true,
    val confirmDestructiveActions: Boolean = true,
    val defaultStemType: TrackType = TrackType.MUSIC,
    val defaultMonitorSendDb: Float = -6f,
    val openTimelineAfterImport: Boolean = true,
    val automaticUpdateChecks: Boolean = true,
    val includePrereleaseUpdates: Boolean = true,
)

interface AppSettingsRepository {
    fun read(): AppSettings
    fun write(value: AppSettings)
}

class SharedPreferencesAppSettingsRepository(context: Context) : AppSettingsRepository {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override fun read() = AppSettings(
        language = enumValueOrDefault(preferences.getString("language", null), AppLanguage.SPANISH),
        keepScreenAwake = preferences.getBoolean("keep_screen_awake", true),
        confirmDestructiveActions = preferences.getBoolean("confirm_destructive_actions", true),
        defaultStemType = enumValueOrDefault(preferences.getString("default_stem_type", null), TrackType.MUSIC),
        defaultMonitorSendDb = preferences.getFloat("default_monitor_send_db", -6f).coerceIn(-60f, 0f),
        openTimelineAfterImport = preferences.getBoolean("open_timeline_after_import", true),
        automaticUpdateChecks = preferences.getBoolean("automatic_update_checks", true),
        includePrereleaseUpdates = preferences.getBoolean("include_prerelease_updates", true),
    )

    override fun write(value: AppSettings) {
        preferences.edit()
            .putString("language", value.language.name)
            .putBoolean("keep_screen_awake", value.keepScreenAwake)
            .putBoolean("confirm_destructive_actions", value.confirmDestructiveActions)
            .putString("default_stem_type", value.defaultStemType.name)
            .putFloat("default_monitor_send_db", value.defaultMonitorSendDb)
            .putBoolean("open_timeline_after_import", value.openTimelineAfterImport)
            .putBoolean("automatic_update_checks", value.automaticUpdateChecks)
            .putBoolean("include_prerelease_updates", value.includePrereleaseUpdates)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}
