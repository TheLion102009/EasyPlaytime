package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlaytimeManager(private val plugin: EasyPlaytime) {
    
    private val playtimeData = ConcurrentHashMap<UUID, Long>()
    private val sessionStartTimes = ConcurrentHashMap<UUID, Long>()
    private val dataFile: File = File(plugin.dataFolder, "playtime.yml")
    private lateinit var dataConfig: FileConfiguration
    
    init {
        loadData()
    }
    
    fun startSession(player: Player) {
        sessionStartTimes[player.uniqueId] = System.currentTimeMillis()
    }
    
    fun endSession(player: Player) {
        val uuid = player.uniqueId
        val startTime = sessionStartTimes.remove(uuid) ?: return
        val sessionTime = System.currentTimeMillis() - startTime
        
        val currentPlaytime = playtimeData.getOrDefault(uuid, 0L)
        playtimeData[uuid] = currentPlaytime + sessionTime
        
        savePlayerData(uuid)
    }
    
    fun getPlaytime(player: Player): Long {
        val uuid = player.uniqueId
        var totalPlaytime = playtimeData.getOrDefault(uuid, 0L)
        
        // Add current session time if player is online
        sessionStartTimes[uuid]?.let { startTime ->
            totalPlaytime += System.currentTimeMillis() - startTime
        }
        
        return totalPlaytime
    }
    
    fun getFormattedPlaytime(player: Player): String {
        val playtimeMs = getPlaytime(player)
        return formatTime(playtimeMs)
    }
    
    fun formatTime(timeMs: Long): String {
        val config = plugin.configManager.getConfig()
        val seconds = timeMs / 1000
        
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        val parts = mutableListOf<String>()
        
        if (config.getBoolean("format.show-days", true) && days > 0) {
            parts.add("${days}d")
        }
        if (config.getBoolean("format.show-hours", true) && hours > 0) {
            parts.add("${hours}h")
        }
        if (config.getBoolean("format.show-minutes", true) && minutes > 0) {
            parts.add("${minutes}m")
        }
        if (config.getBoolean("format.show-seconds", true) && secs > 0) {
            parts.add("${secs}s")
        }
        
        return if (parts.isEmpty()) "0s" else parts.joinToString(" ")
    }
    
    private fun loadData() {
        if (!dataFile.exists()) {
            plugin.dataFolder.mkdirs()
            try {
                dataFile.createNewFile()
            } catch (e: IOException) {
                plugin.logger.severe("Konnte playtime.yml nicht erstellen: ${e.message}")
                return
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)
        
        // Load all player data
        for (key in dataConfig.getKeys(false)) {
            try {
                val uuid = UUID.fromString(key)
                val playtime = dataConfig.getLong(key, 0L)
                playtimeData[uuid] = playtime
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Ungültige UUID in playtime.yml: $key")
            }
        }
        
        plugin.logger.info("Spielzeit-Daten für ${playtimeData.size} Spieler geladen.")
    }
    
    private fun savePlayerData(uuid: UUID) {
        dataConfig.set(uuid.toString(), playtimeData[uuid])
        try {
            dataConfig.save(dataFile)
        } catch (e: IOException) {
            plugin.logger.severe("Konnte Spielzeit-Daten nicht speichern: ${e.message}")
        }
    }
    
    fun saveAllData() {
        // End all active sessions first
        sessionStartTimes.keys.forEach { uuid ->
            val startTime = sessionStartTimes[uuid] ?: return@forEach
            val sessionTime = System.currentTimeMillis() - startTime
            val currentPlaytime = playtimeData.getOrDefault(uuid, 0L)
            playtimeData[uuid] = currentPlaytime + sessionTime
        }
        sessionStartTimes.clear()
        
        // Save all data
        for ((uuid, playtime) in playtimeData) {
            dataConfig.set(uuid.toString(), playtime)
        }
        
        try {
            dataConfig.save(dataFile)
            plugin.logger.info("Alle Spielzeit-Daten gespeichert.")
        } catch (e: IOException) {
            plugin.logger.severe("Konnte Spielzeit-Daten nicht speichern: ${e.message}")
        }
    }
}
