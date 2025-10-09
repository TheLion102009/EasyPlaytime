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
    private val dataManager = DataManager(plugin)

    init {
        if (dataManager.isDatabaseEnabled()) {
            loadDataFromDataSource()
        } else {
            loadData() // Fallback to YAML
        }
    }

    private fun loadDataFromDataSource() {
        try {
            val allPlaytimes = dataManager.getAllPlaytimes()
            playtimeData.putAll(allPlaytimes)
            plugin.logger.info("Spielzeit-Daten für ${playtimeData.size} Spieler aus Datenquelle geladen.")
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Laden der Daten aus der Datenquelle: ${e.message}")
        }
    }

    fun startSession(player: Player) {
        sessionStartTimes[player.uniqueId] = System.currentTimeMillis()
    }

    fun endSession(player: Player) {
        val uuid = player.uniqueId
        val startTime = sessionStartTimes.remove(uuid) ?: return
        val sessionTime = System.currentTimeMillis() - startTime

        val currentPlaytime = playtimeData.getOrDefault(uuid, 0L)
        val totalPlaytime = currentPlaytime + sessionTime
        playtimeData[uuid] = totalPlaytime

        if (dataManager.isDatabaseEnabled()) {
            // Send total playtime to data source, not just session time
            dataManager.updatePlaytime(uuid, totalPlaytime)
            plugin.logger.info("Spielzeit für ${player.name} aktualisiert: ${formatTime(totalPlaytime)} (Session: ${formatTime(sessionTime)})")
        } else {
            savePlayerData(uuid) // Fallback to YAML
        }
    }

    fun getPlaytime(player: Player): Long {
        val uuid = player.uniqueId

        // Always try to get the most current data from data source if enabled
        if (dataManager.isDatabaseEnabled()) {
            try {
                val dataSourcePlaytime = dataManager.getPlaytime(uuid)
                val localPlaytime = playtimeData.getOrDefault(uuid, 0L)

                // Use the highest playtime available
                val currentPlaytime = maxOf(dataSourcePlaytime, localPlaytime)
                playtimeData[uuid] = currentPlaytime

                if (dataSourcePlaytime != localPlaytime) {
                    plugin.logger.info("Spielzeit für ${player.name} abgeglichen: Datenquelle=${formatTime(dataSourcePlaytime)}, Lokal=${formatTime(localPlaytime)}")
                }
            } catch (e: Exception) {
                plugin.logger.warning("Konnte Spielzeit für ${player.name} nicht aus Datenquelle laden: ${e.message}")
            }
        }

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
        if (!::dataConfig.isInitialized) {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile)
        }
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

        if (dataManager.isDatabaseEnabled()) {
            // Save all data to data source with total playtime
            for ((uuid, playtime) in playtimeData) {
                dataManager.updatePlaytime(uuid, playtime)
            }
            plugin.logger.info("Alle Spielzeit-Daten in die Datenquelle gespeichert.")
        } else {
            // Save all data to YAML (fallback)
            if (!::dataConfig.isInitialized) {
                dataConfig = YamlConfiguration.loadConfiguration(dataFile)
            }
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

    // Method to migrate YAML data to data source
    fun migrateToDatabase(): Boolean {
        if (!dataManager.isDatabaseEnabled()) {
            plugin.logger.warning("Datenquelle ist nicht aktiviert. Migration übersprungen.")
            return false
        }

        return dataManager.migrateFromYaml(playtimeData)
    }

    // Method to check data source connection status
    fun isDatabaseConnected(): Boolean {
        return dataManager.isConnected()
    }

    // Method to check if database is enabled in config
    fun isDatabaseEnabled(): Boolean {
        return dataManager.isDatabaseEnabled()
    }

    // Method to sync a specific player from data source
    fun syncPlayerFromDatabase(uuid: UUID) {
        if (!dataManager.isDatabaseEnabled()) {
            return
        }

        try {
            val dataSourcePlaytime = dataManager.getPlaytime(uuid)
            val localPlaytime = playtimeData.getOrDefault(uuid, 0L)

            if (dataSourcePlaytime > localPlaytime) {
                playtimeData[uuid] = dataSourcePlaytime
                plugin.logger.info("Spieler-Daten aus Datenquelle synchronisiert für ${uuid}: ${formatTime(dataSourcePlaytime)} (lokal war: ${formatTime(localPlaytime)})")
            } else if (dataSourcePlaytime < localPlaytime) {
                // Local playtime is higher - update data source immediately
                dataManager.updatePlaytime(uuid, localPlaytime)
                plugin.logger.info("Datenquelle mit höherer lokaler Spielzeit aktualisiert für ${uuid}: ${formatTime(localPlaytime)}")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Synchronisieren des Spielers ${uuid}: ${e.message}")
        }
    }

    // Method to sync data from data source to ensure consistency across servers
    fun syncFromDatabase() {
        if (!dataManager.isDatabaseEnabled()) {
            return
        }

        try {
            val dataSourcePlaytimes = dataManager.getAllPlaytimes()

            // Update local cache with data source data - keep the highest playtime
            for ((uuid, dataSourcePlaytime) in dataSourcePlaytimes) {
                val localPlaytime = playtimeData.getOrDefault(uuid, 0L)
                if (dataSourcePlaytime > localPlaytime) {
                    playtimeData[uuid] = dataSourcePlaytime
                    plugin.logger.info("Synchronisiert Spielzeit für ${uuid} von Datenquelle: ${formatTime(dataSourcePlaytime)} (lokal war: ${formatTime(localPlaytime)})")
                } else if (dataSourcePlaytime < localPlaytime) {
                    // Local playtime is higher - update data source
                    dataManager.updatePlaytime(uuid, localPlaytime)
                    plugin.logger.info("Aktualisiere Datenquelle mit höherer lokaler Spielzeit für ${uuid}: ${formatTime(localPlaytime)}")
                }
            }

            plugin.logger.info("Datenquellen-Synchronisation abgeschlossen. ${dataSourcePlaytimes.size} Spieler synchronisiert.")
        } catch (e: Exception) {
            plugin.logger.severe("Fehler bei der Datenquellen-Synchronisation: ${e.message}")
        }
    }
}
