package de.thelion.easyPlaytime.managers

import de.thelion.easyPlaytime.EasyPlaytime
import java.sql.*
import java.util.*

class DatabaseManager(private val plugin: EasyPlaytime) {

    private var connection: Connection? = null
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

            // Verbindung aufbauen
            val url = "jdbc:mariadb://$host:$port/$database"
            connection = DriverManager.getConnection(url, username, password)

            // Tabelle erstellen, falls sie nicht existiert
            createTableIfNotExists(table)

            plugin.logger.info("Datenbankverbindung erfolgreich hergestellt!")
        } catch (e: SQLException) {
            plugin.logger.severe("Fehler beim Verbinden zur Datenbank: ${e.message}")
            connection = null
        }
    }

    private fun createTableIfNotExists(tableName: String) {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS `$tableName` (
                `uuid` VARCHAR(36) NOT NULL PRIMARY KEY,
                `playtime_ms` BIGINT NOT NULL DEFAULT 0,
                `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """.trimIndent()

        connection?.prepareStatement(createTableSQL)?.use { it.execute() }
    }

    fun getPlaytime(uuid: UUID): Long {
        if (connection == null) return 0L

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
        if (connection == null) return

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

    fun updatePlaytime(uuid: UUID, additionalTimeMs: Long) {
        if (connection == null) return

        val table = config.getString("database.table") ?: "player_playtime"
        val query = """
            INSERT INTO `$table` (uuid, playtime_ms) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE playtime_ms = playtime_ms + VALUES(playtime_ms), last_updated = CURRENT_TIMESTAMP
        """.trimIndent()

        try {
            connection?.prepareStatement(query)?.use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setLong(2, additionalTimeMs)
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
        return connection != null && !connection!!.isClosed
    }

    // Migration von YAML zu Datenbank
    fun migrateFromYaml(yamlPlaytimeData: Map<UUID, Long>): Boolean {
        if (connection == null) return false

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
}
