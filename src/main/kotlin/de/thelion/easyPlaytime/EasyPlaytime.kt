package de.thelion.easyPlaytime

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import de.thelion.easyPlaytime.commands.PlaytimeCommand
import de.thelion.easyPlaytime.listeners.PlayerListener
import de.thelion.easyPlaytime.managers.PlaytimeManager
import de.thelion.easyPlaytime.managers.ConfigManager
import de.thelion.easyPlaytime.placeholders.PlaytimePlaceholder
import java.util.concurrent.TimeUnit

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

        // Start periodic sync if database is enabled
        if (configManager.getConfig().getBoolean("database.enabled", false) || 
            configManager.getConfig().getBoolean("redis.enabled", false)) {
            startPeriodicSync()
        }
        
        logger.info("EasyPlaytime Plugin wurde erfolgreich aktiviert!")
    }

    override fun onDisable() {
        // Save all playtime data
        playtimeManager.saveAllData()
        logger.info("EasyPlaytime Plugin wurde deaktiviert!")
    }

    private fun startPeriodicSync() {
        // Use Folia-compatible async scheduler
        try {
            server.asyncScheduler.runAtFixedRate(this, { _ ->
                try {
                    if (configManager.getConfig().getBoolean("database.enabled", false) ||
                        configManager.getConfig().getBoolean("redis.enabled", false)) {
                        playtimeManager.syncFromDatabase()
                    }
                } catch (e: Exception) {
                    logger.warning("Fehler bei der periodischen Synchronisation: ${e.message}")
                }
            }, 5, 5, TimeUnit.MINUTES)
            logger.info("Periodische Synchronisation gestartet (alle 5 Minuten)")
        } catch (e: Exception) {
            logger.warning("Konnte periodische Synchronisation nicht starten: ${e.message}")
            logger.warning("Synchronisation wird nur bei Spieler-Events durchgeführt")
        }
    }
}
