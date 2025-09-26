package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: EasyPlaytime) {
    
    init {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
    }
    
    fun getConfig(): FileConfiguration {
        return plugin.config
    }
    
    fun reloadConfig() {
        plugin.reloadConfig()
    }
    
    fun saveConfig() {
        plugin.saveConfig()
    }
}
