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
        
        // Check Minecraft version compatibility (1.21.0 - 1.21.10)
        checkServerCompatibility()

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

    private fun checkServerCompatibility() {
        val version = server.minecraftVersion
        val isFolia = try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        val serverType = if (isFolia) "Folia" else "Paper"

        // Check if version is 1.21.x (1.21.0 - 1.21.10)
        val versionParts = version.split(".")
        if (versionParts.size >= 2) {
            val major = versionParts[0].toIntOrNull() ?: 0
            val minor = versionParts[1].toIntOrNull() ?: 0

            if (major == 1 && minor == 21) {
                logger.info("✓ Running on $serverType $version - Fully supported!")
                return
            }
        }

        logger.warning("⚠ Running on $serverType $version")
        logger.warning("⚠ This plugin is designed for Minecraft 1.21.0 - 1.21.10")
        logger.warning("⚠ Other versions may work but are not officially supported")
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
