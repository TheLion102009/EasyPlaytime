package de.thelion.easyPlaytime.listeners

import de.thelion.easyPlaytime.EasyPlaytime
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val plugin: EasyPlaytime) : Listener {
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.playtimeManager.startSession(event.player)
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.playtimeManager.endSession(event.player)
    }
}
