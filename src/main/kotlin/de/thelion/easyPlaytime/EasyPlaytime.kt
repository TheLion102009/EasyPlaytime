package de.thelion.easyPlaytime

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import de.thelion.easyPlaytime.commands.PlaytimeCommand
import de.thelion.easyPlaytime.listeners.PlayerListener
import de.thelion.easyPlaytime.managers.PlaytimeManager
import de.thelion.easyPlaytime.managers.ConfigManager
import de.thelion.easyPlaytime.placeholders.PlaytimePlaceholder

class EasyPlaytime : JavaPlugin() {
    
    companion object {
        lateinit var instance: EasyPlaytime
            private set
    }
    
    lateinit var playtimeManager: PlaytimeManager
        private set
    lateinit var configManager: ConfigManager
        private set

    override fun onEnable() {
        instance = this
        
        // Initialize managers
        configManager = ConfigManager(this)
        playtimeManager = PlaytimeManager(this)
        
        // Register commands
        getCommand("easyplaytime")?.setExecutor(PlaytimeCommand(this))
        getCommand("eplaytime")?.setExecutor(PlaytimeCommand(this))
        
        // Register listeners
        server.pluginManager.registerEvents(PlayerListener(this), this)
        
        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaytimePlaceholder(this).register()
            logger.info("PlaceholderAPI integration enabled!")
        } else {
            logger.warning("PlaceholderAPI not found! Placeholder integration disabled.")
        }
        
        logger.info("EasyPlaytime Plugin wurde erfolgreich aktiviert!")
    }

    override fun onDisable() {
        // Save all playtime data
        playtimeManager.saveAllData()
        logger.info("EasyPlaytime Plugin wurde deaktiviert!")
    }
}
