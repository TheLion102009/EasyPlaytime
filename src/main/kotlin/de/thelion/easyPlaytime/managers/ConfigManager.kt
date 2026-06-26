package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: EasyPlaytime) {

    companion object {
        const val CURRENT_CONFIG_VERSION = 3
    }

    // Cache für häufig abgefragte Config-Werte (Performance-Optimierung)
    private var databaseEnabled: Boolean = false
    private var redisEnabled: Boolean = false
    private var syncDebug: Boolean = false
    private var showDays: Boolean = true
    private var showHours: Boolean = true
    private var showMinutes: Boolean = true
    private var showSeconds: Boolean = true

    // Auto-Modus Cache
    private var autoEnabled: Boolean = true
    private var autoCount: Int = 3

    init {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        checkAndUpdateConfig()
        cacheConfigValues()
    }

    private fun checkAndUpdateConfig() {
        val config = plugin.config
        val configVersion = config.getInt("config-version", 1)

        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.logger.info("Config-Update erkannt: Version $configVersion -> $CURRENT_CONFIG_VERSION")
            updateConfig(configVersion)
        }
    }

    private fun updateConfig(oldVersion: Int) {
        val config = plugin.config

        if (oldVersion <= 1) {
            if (!config.contains("format.show-seconds")) {
                config.set("format.show-seconds", true)
                plugin.logger.info("- Hinzugefügt: format.show-seconds")
            }
        }

        if (oldVersion <= 2) {
            if (!config.contains("format.auto.enabled")) {
                config.set("format.auto.enabled", true)
                plugin.logger.info("- Hinzugefügt: format.auto.enabled")
            }
            if (!config.contains("format.auto.count")) {
                config.set("format.auto.count", 3)
                plugin.logger.info("- Hinzugefügt: format.auto.count")
            }
        }

        config.set("config-version", CURRENT_CONFIG_VERSION)
        plugin.saveConfig()
        plugin.logger.info("Config erfolgreich auf Version $CURRENT_CONFIG_VERSION aktualisiert!")
    }

    private fun cacheConfigValues() {
        val config = plugin.config
        databaseEnabled = config.getBoolean("database.enabled", false)
        redisEnabled = config.getBoolean("redis.enabled", false)
        syncDebug = config.getBoolean("database.sync-debug", false)
        showDays = config.getBoolean("format.show-days", true)
        showHours = config.getBoolean("format.show-hours", true)
        showMinutes = config.getBoolean("format.show-minutes", true)
        showSeconds = config.getBoolean("format.show-seconds", true)
        autoEnabled = config.getBoolean("format.auto.enabled", true)
        autoCount = config.getInt("format.auto.count", 3).coerceIn(1, 4)
    }

    fun getConfig(): FileConfiguration {
        return plugin.config
    }

    fun reloadConfig() {
        plugin.reloadConfig()
        cacheConfigValues()
    }

    fun saveConfig() {
        plugin.saveConfig()
    }

    // Schnelle Zugriffsmethoden auf gecachte Werte
    fun isDatabaseEnabled(): Boolean = databaseEnabled
    fun isRedisEnabled(): Boolean = redisEnabled
    fun isSyncDebug(): Boolean = syncDebug
    fun isShowDays(): Boolean = showDays
    fun isShowHours(): Boolean = showHours
    fun isShowMinutes(): Boolean = showMinutes
    fun isShowSeconds(): Boolean = showSeconds

    // Auto-Modus
    fun isAutoEnabled(): Boolean = autoEnabled
    fun getAutoCount(): Int = autoCount
}
