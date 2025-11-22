package de.thelion.easyPlaytime.listeners

import de.thelion.easyPlaytime.EasyPlaytime
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val plugin: EasyPlaytime) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Start tracking playtime for this player
        plugin.playtimeManager.startSession(player)

        // Sync data from database when player joins to ensure consistency (async)
        if (plugin.configManager.getConfig().getBoolean("database.enabled", false) ||
            plugin.configManager.getConfig().getBoolean("redis.enabled", false)) {
            // Async ausführen um den Main-Thread nicht zu blockieren
            plugin.server.asyncScheduler.runNow(plugin) { _ ->
                try {
                    plugin.playtimeManager.syncPlayerFromDatabase(player.uniqueId)
                } catch (e: Exception) {
                    plugin.logger.warning("Fehler beim Synchronisieren der Daten für ${player.name}: ${e.message}")
                }
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // End tracking playtime for this player (async DB-Update)
        plugin.playtimeManager.endSession(player)
    }
}
