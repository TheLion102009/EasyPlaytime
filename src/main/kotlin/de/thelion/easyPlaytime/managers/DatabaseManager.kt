package de.thelion.easyPlaytime.managers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.thelion.easyPlaytime.EasyPlaytime
import java.sql.*
import java.util.*

class DatabaseManager(private val plugin: EasyPlaytime) {

    private var dataSource: HikariDataSource? = null
    private val config = plugin.configManager.getConfig()

    init {
        if (config.getBoolean("database.enabled", false)) {
            initializeDatabase()
        }
    }

    private fun initializeDatabase() {
        try {
            val host = config.getString("database.host", "localhost")
            val port = config.getInt("database.port", 3306)
            val database = config.getString("database.database", "easyplaytime")
            val username = config.getString("database.username", "minecraft")
            val password = config.getString("database.password", "password")
            val table = config.getString("database.table") ?: "player_playtime"
            val timeout = config.getInt("database.connection-timeout", 30)

            // HikariCP Konfiguration für optimales Connection Pooling
            val hikariConfig = HikariConfig().apply {
                // Versuche zuerst MariaDB, dann MySQL
                try {
                    jdbcUrl = "jdbc:mariadb://$host:$port/$database"
                    driverClassName = "org.mariadb.jdbc.Driver"
                } catch (e: Exception) {
                    jdbcUrl = "jdbc:mysql://$host:$port/$database"
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                }

                this.username = username
                this.password = password

                // Pool-Optimierungen
                maximumPoolSize = 10 // Maximum 10 Verbindungen
                minimumIdle = 2 // Mindestens 2 idle Verbindungen
                connectionTimeout = (timeout * 1000).toLong()
                idleTimeout = 600000 // 10 Minuten
                maxLifetime = 1800000 // 30 Minuten

                // Performance-Optimierungen
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("useLocalSessionState", "true")
                addDataSourceProperty("rewriteBatchedStatements", "true")
                addDataSourceProperty("cacheResultSetMetadata", "true")
                addDataSourceProperty("cacheServerConfiguration", "true")
                addDataSourceProperty("elideSetAutoCommits", "true")
                addDataSourceProperty("maintainTimeStats", "false")

                poolName = "EasyPlaytime-Pool"
            }

            dataSource = HikariDataSource(hikariConfig)

            // Tabelle erstellen, falls sie nicht existiert
            createTableIfNotExists(table)

            plugin.logger.info("Datenbankverbindung mit HikariCP erfolgreich hergestellt! (Pool-Größe: 10)")
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Verbinden zur Datenbank: ${e.message}")
            dataSource?.close()
            dataSource = null
        }
    }

    private fun getConnection(): Connection? {
        return try {
            dataSource?.connection
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Holen einer Verbindung aus dem Pool: ${e.message}")
            null
        }
    }

    private fun createTableIfNotExists(tableName: String) {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS `$tableName` (
                `uuid` VARCHAR(36) NOT NULL PRIMARY KEY,
                `playtime_ms` BIGINT NOT NULL DEFAULT 0,
                `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX `idx_last_updated` (`last_updated`)
            )
        """.trimIndent()

        try {
            getConnection()?.use { conn ->
                conn.prepareStatement(createTableSQL).use { it.execute() }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Erstellen der Tabelle: ${e.message}")
        }
    }

    fun getPlaytime(uuid: UUID): Long {
        val table = config.getString("database.table") ?: "player_playtime"
        val query = "SELECT playtime_ms FROM `$table` WHERE uuid = ?"

        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            rs.getLong("playtime_ms")
                        } else {
                            0L
                        }
                    }
                }
            } ?: 0L
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Laden der Spielzeit für $uuid: ${e.message}")
            0L
        }
    }

    fun savePlaytime(uuid: UUID, playtimeMs: Long) {
        val table = config.getString("database.table") ?: "player_playtime"
        val query = """
            INSERT INTO `$table` (uuid, playtime_ms) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE playtime_ms = VALUES(playtime_ms), last_updated = CURRENT_TIMESTAMP
        """.trimIndent()

        try {
            getConnection()?.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setLong(2, playtimeMs)
                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Speichern der Spielzeit für $uuid: ${e.message}")
        }
    }

    fun updatePlaytime(uuid: UUID, totalPlaytimeMs: Long) {
        val table = config.getString("database.table") ?: "player_playtime"
        val query = """
            INSERT INTO `$table` (uuid, playtime_ms) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE playtime_ms = GREATEST(playtime_ms, VALUES(playtime_ms)), last_updated = CURRENT_TIMESTAMP
        """.trimIndent()

        try {
            getConnection()?.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setLong(2, totalPlaytimeMs)
                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Aktualisieren der Spielzeit für $uuid: ${e.message}")
        }
    }

    // Batch-Update für mehrere Spieler gleichzeitig (Performance-Optimierung)
    fun batchUpdatePlaytimes(playtimes: Map<UUID, Long>) {
        if (playtimes.isEmpty()) return

        val table = config.getString("database.table") ?: "player_playtime"
        val query = """
            INSERT INTO `$table` (uuid, playtime_ms) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE playtime_ms = GREATEST(playtime_ms, VALUES(playtime_ms)), last_updated = CURRENT_TIMESTAMP
        """.trimIndent()

        try {
            getConnection()?.use { conn ->
                conn.autoCommit = false
                conn.prepareStatement(query).use { stmt ->
                    for ((uuid, playtime) in playtimes) {
                        stmt.setString(1, uuid.toString())
                        stmt.setLong(2, playtime)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
                conn.commit()
                conn.autoCommit = true
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Batch-Update der Spielzeiten: ${e.message}")
        }
    }

    fun closeConnection() {
        try {
            dataSource?.close()
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Schließen des Connection Pools: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return try {
            dataSource?.isRunning == true && getConnection()?.use { it.isValid(2) } == true
        } catch (e: Exception) {
            false
        }
    }

    fun isDatabaseEnabled(): Boolean {
        return config.getBoolean("database.enabled", false)
    }

    // Migration von YAML zu Datenbank
    fun migrateFromYaml(yamlPlaytimeData: Map<UUID, Long>): Boolean {
        if (dataSource == null) return false

        var success = true
        for ((uuid, playtime) in yamlPlaytimeData) {
            try {
                savePlaytime(uuid, playtime)
            } catch (e: Exception) {
                plugin.logger.severe("Fehler beim Migrieren der Daten für $uuid: ${e.message}")
                success = false
            }
        }
        return success
    }

    fun getAllPlaytimes(): Map<UUID, Long> {
        val table = config.getString("database.table") ?: "player_playtime"
        val query = "SELECT uuid, playtime_ms FROM `$table`"

        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.fetchSize = 100 // Performance-Optimierung
                    stmt.executeQuery().use { rs ->
                        val playtimes = mutableMapOf<UUID, Long>()
                        while (rs.next()) {
                            try {
                                val uuid = UUID.fromString(rs.getString("uuid"))
                                val playtime = rs.getLong("playtime_ms")
                                playtimes[uuid] = playtime
                            } catch (e: IllegalArgumentException) {
                                plugin.logger.warning("Ungültige UUID in Datenbank: ${rs.getString("uuid")}")
                            }
                        }
                        playtimes
                    }
                }
            } ?: emptyMap()
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Laden aller Spielzeiten: ${e.message}")
            emptyMap()
        }
    }

    // Batch-Abfrage für mehrere UUIDs (Performance-Optimierung)
    fun getPlaytimes(uuids: Collection<UUID>): Map<UUID, Long> {
        if (uuids.isEmpty()) return emptyMap()

        val table = config.getString("database.table") ?: "player_playtime"
        val placeholders = uuids.joinToString(",") { "?" }
        val query = "SELECT uuid, playtime_ms FROM `$table` WHERE uuid IN ($placeholders)"

        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    uuids.forEachIndexed { index, uuid ->
                        stmt.setString(index + 1, uuid.toString())
                    }
                    stmt.executeQuery().use { rs ->
                        val playtimes = mutableMapOf<UUID, Long>()
                        while (rs.next()) {
                            try {
                                val uuid = UUID.fromString(rs.getString("uuid"))
                                val playtime = rs.getLong("playtime_ms")
                                playtimes[uuid] = playtime
                            } catch (e: IllegalArgumentException) {
                                plugin.logger.warning("Ungültige UUID in Datenbank: ${rs.getString("uuid")}")
                            }
                        }
                        playtimes
                    }
                }
            } ?: emptyMap()
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Batch-Laden der Spielzeiten: ${e.message}")
            emptyMap()
        }
    }
}
