package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.*

class RedisManager(private val plugin: EasyPlaytime) {

    private var jedisPool: JedisPool? = null
    private val config = plugin.configManager.getConfig()

    init {
        if (config.getBoolean("redis.enabled", false)) {
            initializeRedis()
        }
    }

    private fun initializeRedis() {
        try {
            val host = config.getString("redis.host", "localhost")
            val port = config.getInt("redis.port", 6379)
            val password = config.getString("redis.password", "")
            val database = config.getInt("redis.database", 0)

            val poolConfig = JedisPoolConfig().apply {
                maxTotal = 10
                maxIdle = 5
                minIdle = 1
                testOnBorrow = true
                testOnReturn = true
                testWhileIdle = true
            }

            jedisPool = if (password.isNullOrEmpty()) {
                JedisPool(poolConfig, host, port, 5000)
            } else {
                JedisPool(poolConfig, host, port, 5000, password)
            }

            // Test connection and select database
            jedisPool?.resource?.use { jedis ->
                if (database > 0) {
                    jedis.select(database)
                }
                jedis.ping()
            }

            plugin.logger.info("Redis-Verbindung erfolgreich hergestellt!")
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Verbinden zu Redis: ${e.message}")
            jedisPool = null
        }
    }

    fun getPlaytime(uuid: UUID): Long {
        if (!isRedisEnabled() || jedisPool == null) return 0L

        return try {
            jedisPool?.resource?.use { jedis ->
                val database = config.getInt("redis.database", 0)
                if (database > 0) {
                    jedis.select(database)
                }
                val key = "easyplaytime:${uuid}"
                val playtimeStr = jedis.get(key)
                playtimeStr?.toLongOrNull() ?: 0L
            } ?: 0L
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Laden der Spielzeit für $uuid: ${e.message}")
            0L
        }
    }

    fun savePlaytime(uuid: UUID, playtimeMs: Long) {
        if (!isRedisEnabled() || jedisPool == null) return

        try {
            jedisPool?.resource?.use { jedis ->
                val database = config.getInt("redis.database", 0)
                if (database > 0) {
                    jedis.select(database)
                }
                val key = "easyplaytime:${uuid}"
                jedis.set(key, playtimeMs.toString())
            }
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Speichern der Spielzeit für $uuid: ${e.message}")
        }
    }

    fun updatePlaytime(uuid: UUID, totalPlaytimeMs: Long) {
        if (!isRedisEnabled() || jedisPool == null) return

        try {
            jedisPool?.resource?.use { jedis ->
                val database = config.getInt("redis.database", 0)
                if (database > 0) {
                    jedis.select(database)
                }
                val key = "easyplaytime:${uuid}"
                // Get current value and use the maximum
                val currentPlaytime = jedis.get(key)?.toLongOrNull() ?: 0L
                val finalPlaytime = maxOf(currentPlaytime, totalPlaytimeMs)
                jedis.set(key, finalPlaytime.toString())
            }
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Aktualisieren der Spielzeit für $uuid: ${e.message}")
        }
    }

    fun getAllPlaytimes(): Map<UUID, Long> {
        if (!isRedisEnabled() || jedisPool == null) return emptyMap()

        return try {
            jedisPool?.resource?.use { jedis ->
                val database = config.getInt("redis.database", 0)
                if (database > 0) {
                    jedis.select(database)
                }
                val pattern = "easyplaytime:*"
                val keys = jedis.keys(pattern)
                val playtimes = mutableMapOf<UUID, Long>()

                // Verwende Pipeline für bessere Performance bei vielen Keys
                if (keys.isNotEmpty()) {
                    val pipeline = jedis.pipelined()
                    val responses = keys.map { key ->
                        key to pipeline.get(key)
                    }
                    pipeline.sync()

                    for ((key, response) in responses) {
                        try {
                            val uuid = UUID.fromString(key.removePrefix("easyplaytime:"))
                            val playtime = response.get()?.toLongOrNull() ?: 0L
                            playtimes[uuid] = playtime
                        } catch (e: IllegalArgumentException) {
                            plugin.logger.warning("Ungültige UUID in Redis: $key")
                        }
                    }
                }

                playtimes
            } ?: emptyMap()
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Laden aller Spielzeiten: ${e.message}")
            emptyMap()
        }
    }

    // Batch-Update für mehrere Spieler gleichzeitig (Performance-Optimierung)
    fun batchUpdatePlaytimes(playtimes: Map<UUID, Long>) {
        if (!isRedisEnabled() || jedisPool == null || playtimes.isEmpty()) return

        try {
            jedisPool?.resource?.use { jedis ->
                val database = config.getInt("redis.database", 0)
                if (database > 0) {
                    jedis.select(database)
                }

                // Verwende Pipeline für Batch-Updates
                val pipeline = jedis.pipelined()
                for ((uuid, playtime) in playtimes) {
                    val key = "easyplaytime:${uuid}"
                    pipeline.set(key, playtime.toString())
                }
                pipeline.sync()
            }
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Batch-Update der Spielzeiten: ${e.message}")
        }
    }

    // Batch-Abfrage für mehrere UUIDs (Performance-Optimierung)
    fun getPlaytimes(uuids: Collection<UUID>): Map<UUID, Long> {
        if (!isRedisEnabled() || jedisPool == null || uuids.isEmpty()) return emptyMap()

        return try {
            jedisPool?.resource?.use { jedis ->
                val database = config.getInt("redis.database", 0)
                if (database > 0) {
                    jedis.select(database)
                }

                val playtimes = mutableMapOf<UUID, Long>()

                // Verwende Pipeline für Batch-Abfragen
                val pipeline = jedis.pipelined()
                val responses = uuids.map { uuid ->
                    uuid to pipeline.get("easyplaytime:${uuid}")
                }
                pipeline.sync()

                for ((uuid, response) in responses) {
                    try {
                        val playtime = response.get()?.toLongOrNull() ?: 0L
                        playtimes[uuid] = playtime
                    } catch (e: Exception) {
                        plugin.logger.warning("Fehler beim Laden der Spielzeit für $uuid: ${e.message}")
                    }
                }

                playtimes
            } ?: emptyMap()
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Batch-Laden der Spielzeiten: ${e.message}")
            emptyMap()
        }
    }

    fun isConnected(): Boolean {
        return try {
            jedisPool?.resource?.use { jedis ->
                val database = config.getInt("redis.database", 0)
                if (database > 0) {
                    jedis.select(database)
                }
                jedis.ping() == "PONG"
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun isRedisEnabled(): Boolean {
        return config.getBoolean("redis.enabled", false)
    }

    fun closeConnection() {
        try {
            jedisPool?.close()
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Schließen der Redis-Verbindung: ${e.message}")
        }
    }
}
