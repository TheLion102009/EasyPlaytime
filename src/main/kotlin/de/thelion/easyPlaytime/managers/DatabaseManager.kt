package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import org.mariadb.jdbc.Driver
import java.sql.*
import java.util.*

class DatabaseManager(private val plugin: EasyPlaytime) {

    private var connection: Connection? = null
    private val config = plugin.configManager.getConfig()

    init {
        // Statisch den Treiber laden
        try {
            DriverManager.registerDriver(Driver())
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Laden des MariaDB-Treibers: ${e.message}")
        }

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

            // Verbindung aufbauen - versuche sowohl MariaDB als auch MySQL URL
            val urls = listOf(
                "jdbc:mariadb://$host:$port/$database",
                "jdbc:mysql://$host:$port/$database"
            )

            var connected = false
            for (url in urls) {
                try {
                    connection = DriverManager.getConnection(url, username, password)
                    connected = true
                    break
                } catch (e: SQLException) {
                    // Try next URL
                }
            }

            if (!connected) {
                plugin.logger.severe("Konnte keine Verbindung zur Datenbank herstellen!")
                return
            }

            // Tabelle erstellen, falls sie nicht existiert
            createTableIfNotExists(table)

            plugin.logger.info("Datenbankverbindung erfolgreich hergestellt!")
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Verbinden zur Datenbank: ${e.message}")
            connection = null
        }
    }

    private fun ensureConnection(): Boolean {
        if (connection == null || connection!!.isClosed || !connection!!.isValid(2)) {
            try {
                connection?.close()
                initializeDatabase()
                return connection != null && connection!!.isValid(2)
            } catch (e: SQLException) {
                plugin.logger.severe("Fehler beim Neuverbinden zur Datenbank: ${e.message}")
                connection = null
                return false
            }
        }
        return true
    }

    private fun createTableIfNotExists(tableName: String) {
        if (!ensureConnection()) return

        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS `$tableName` (
                `uuid` VARCHAR(36) NOT NULL PRIMARY KEY,
                `playtime_ms` BIGINT NOT NULL DEFAULT 0,
                `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """.trimIndent()

        try {
            connection?.prepareStatement(createTableSQL)?.use { it.execute() }
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Erstellen der Tabelle: ${e.message}")
        }
    }

    fun getPlaytime(uuid: UUID): Long {
        if (!ensureConnection()) return 0L

        val table = config.getString("database.table") ?: "player_playtime"
        val query = "SELECT playtime_ms FROM `$table` WHERE uuid = ?"

        return try {
            connection?.prepareStatement(query)?.use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getLong("playtime_ms")
                    } else {
                        0L
                    }
                }
            } ?: 0L
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Laden der Spielzeit für $uuid: ${e.message}")
            0L
        }
    }

    fun savePlaytime(uuid: UUID, playtimeMs: Long) {
        if (!ensureConnection()) return

        val table = config.getString("database.table") ?: "player_playtime"
        val query = """
            INSERT INTO `$table` (uuid, playtime_ms) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE playtime_ms = VALUES(playtime_ms), last_updated = CURRENT_TIMESTAMP
        """.trimIndent()

        try {
            connection?.prepareStatement(query)?.use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setLong(2, playtimeMs)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Speichern der Spielzeit für $uuid: ${e.message}")
        }
    }

    fun updatePlaytime(uuid: UUID, totalPlaytimeMs: Long) {
        if (!ensureConnection()) return

        val table = config.getString("database.table") ?: "player_playtime"
        val query = """
            INSERT INTO `$table` (uuid, playtime_ms) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE playtime_ms = GREATEST(playtime_ms, VALUES(playtime_ms)), last_updated = CURRENT_TIMESTAMP
        """.trimIndent()

        try {
            connection?.prepareStatement(query)?.use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setLong(2, totalPlaytimeMs)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Aktualisieren der Spielzeit für $uuid: ${e.message}")
        }
    }

    fun closeConnection() {
        try {
            connection?.close()
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Schließen der Datenbankverbindung: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return try {
            connection != null && !connection!!.isClosed && connection!!.isValid(5)
        } catch (e: SQLException) {
            plugin.logger.warning("Fehler beim Überprüfen der Datenbankverbindung: ${e.message}")
            false
        }
    }

    fun isDatabaseEnabled(): Boolean {
        return config.getBoolean("database.enabled", false)
    }

    // Migration von YAML zu Datenbank
    fun migrateFromYaml(yamlPlaytimeData: Map<UUID, Long>): Boolean {
        if (!ensureConnection()) return false

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
        if (!ensureConnection()) return emptyMap()

        val table = config.getString("database.table") ?: "player_playtime"
        val query = "SELECT uuid, playtime_ms FROM `$table`"

        return try {
            connection?.prepareStatement(query)?.use { stmt ->
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
            } ?: emptyMap()
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Laden aller Spielzeiten: ${e.message}")
            emptyMap()
        }
    }
}
