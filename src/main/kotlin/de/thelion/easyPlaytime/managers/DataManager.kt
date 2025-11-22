package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import java.util.*

class DataManager(private val plugin: EasyPlaytime) {

    private val databaseManager = DatabaseManager(plugin)
    private val redisManager = RedisManager(plugin)

    fun getPlaytime(uuid: UUID): Long {
        return if (redisManager.isRedisEnabled()) {
            redisManager.getPlaytime(uuid)
        } else {
            databaseManager.getPlaytime(uuid)
        }
    }

    fun savePlaytime(uuid: UUID, playtimeMs: Long) {
        if (redisManager.isRedisEnabled()) {
            redisManager.savePlaytime(uuid, playtimeMs)
        } else {
            databaseManager.savePlaytime(uuid, playtimeMs)
        }
    }

    fun updatePlaytime(uuid: UUID, totalPlaytimeMs: Long) {
        if (redisManager.isRedisEnabled()) {
            redisManager.updatePlaytime(uuid, totalPlaytimeMs)
        } else {
            databaseManager.updatePlaytime(uuid, totalPlaytimeMs)
        }
    }

    fun getAllPlaytimes(): Map<UUID, Long> {
        return if (redisManager.isRedisEnabled()) {
            redisManager.getAllPlaytimes()
        } else {
            databaseManager.getAllPlaytimes()
        }
    }

    // Batch-Update für bessere Performance
    fun batchUpdatePlaytimes(playtimes: Map<UUID, Long>) {
        if (redisManager.isRedisEnabled()) {
            redisManager.batchUpdatePlaytimes(playtimes)
        } else {
            databaseManager.batchUpdatePlaytimes(playtimes)
        }
    }

    // Batch-Abfrage für bessere Performance
    fun getPlaytimes(uuids: Collection<UUID>): Map<UUID, Long> {
        return if (redisManager.isRedisEnabled()) {
            redisManager.getPlaytimes(uuids)
        } else {
            databaseManager.getPlaytimes(uuids)
        }
    }

    fun isConnected(): Boolean {
        return if (redisManager.isRedisEnabled()) {
            redisManager.isConnected()
        } else {
            databaseManager.isConnected()
        }
    }

    fun isDatabaseEnabled(): Boolean {
        return plugin.configManager.getConfig().getBoolean("database.enabled", false) ||
               plugin.configManager.getConfig().getBoolean("redis.enabled", false)
    }

    fun closeConnection() {
        if (redisManager.isRedisEnabled()) {
            redisManager.closeConnection()
        } else {
            databaseManager.closeConnection()
        }
    }

    // Migration von YAML zu Datenquelle
    fun migrateFromYaml(yamlPlaytimeData: Map<UUID, Long>): Boolean {
        if (redisManager.isRedisEnabled()) {
            var success = true
            for ((uuid, playtime) in yamlPlaytimeData) {
                try {
                    redisManager.savePlaytime(uuid, playtime)
                } catch (e: Exception) {
                    plugin.logger.severe("Fehler beim Migrieren der Daten für $uuid: ${e.message}")
                    success = false
                }
            }
            return success
        } else {
            return databaseManager.migrateFromYaml(yamlPlaytimeData)
        }
    }
}
