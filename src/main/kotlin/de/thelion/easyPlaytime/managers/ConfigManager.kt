package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: EasyPlaytime) {
    
    companion object {
        const val CURRENT_CONFIG_VERSION = 2
    }

    // Cache für häufig abgefragte Config-Werte (Performance-Optimierung)
    private var databaseEnabled: Boolean = false
    private var redisEnabled: Boolean = false
    private var syncDebug: Boolean = false
    private var showDays: Boolean = true
    private var showHours: Boolean = true
    private var showMinutes: Boolean = true
    private var showSeconds: Boolean = true

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

        when (oldVersion) {
            1 -> {
                // Update von Version 1 zu Version 2
                if (!config.contains("format.show-seconds")) {
                    config.set("format.show-seconds", true)
                    plugin.logger.info("- Hinzugefügt: format.show-seconds")
                }
                if (!config.contains("config-version")) {
                    config.set("config-version", 2)
                    plugin.logger.info("- Hinzugefügt: config-version")
                }
            }
        }

        // Setze die aktuelle Config-Version
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
}
