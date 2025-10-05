package de.thelion.easyPlaytime.commands

import de.thelion.easyPlaytime.EasyPlaytime
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PlaytimeCommand(private val plugin: EasyPlaytime) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Verwendung: /$label <playtime|reload> [Spieler]")
            return true
        }
        
        when (args[0].lowercase()) {
            "playtime" -> {
                handlePlaytimeCommand(sender, args)
            }
            "reload" -> {
                handleReloadCommand(sender)
            }
            else -> {
                sender.sendMessage("${ChatColor.RED}Unbekannter Befehl. Verwendung: /$label <playtime|reload> [Spieler]")
            }
        }
        
        return true
    }
    
    private fun handlePlaytimeCommand(sender: CommandSender, args: Array<out String>) {
        val targetPlayer: Player? = if (args.size > 1) {
            val playerName = args[1]
            Bukkit.getPlayer(playerName) ?: Bukkit.getOfflinePlayer(playerName).takeIf { it.hasPlayedBefore() }?.let { offlinePlayer ->
                // For offline players, we need to create a temporary player object or handle differently
                sender.sendMessage("${ChatColor.YELLOW}Spieler $playerName ist offline. Zeige gespeicherte Spielzeit...")
                return // For now, we'll only support online players
            }
        } else {
            if (sender is Player) sender else null
        }
        
        if (targetPlayer == null) {
            if (args.size > 1) {
                sender.sendMessage("${ChatColor.RED}Spieler '${args[1]}' wurde nicht gefunden oder war noch nie online.")
            } else {
                sender.sendMessage("${ChatColor.RED}Du musst ein Spieler sein oder einen Spielernamen angeben.")
            }
            return
        }
        
        val playtime = plugin.playtimeManager.getFormattedPlaytime(targetPlayer)
        val playerName = targetPlayer.name
        
        if (sender == targetPlayer) {
            sender.sendMessage("${ChatColor.GREEN}Deine Spielzeit: ${ChatColor.YELLOW}$playtime")
        } else {
            sender.sendMessage("${ChatColor.GREEN}Spielzeit von $playerName: ${ChatColor.YELLOW}$playtime")
        }
    }
    
    private fun handleReloadCommand(sender: CommandSender) {
        if (!sender.hasPermission("easyplaytime.reload")) {
            sender.sendMessage("${ChatColor.RED}Du hast keine Berechtigung für diesen Befehl.")
            return
        }
        
        plugin.configManager.reloadConfig()
        
        // Check if database is enabled and migrate if necessary
        if (plugin.configManager.getConfig().getBoolean("database.enabled", false)) {
            sender.sendMessage("${ChatColor.YELLOW}Datenbank ist aktiviert. Migriere Daten...")
            val migrationSuccess = plugin.playtimeManager.migrateToDatabase()
            if (migrationSuccess) {
                sender.sendMessage("${ChatColor.GREEN}Daten erfolgreich in die Datenbank migriert!")
            } else {
                sender.sendMessage("${ChatColor.RED}Fehler bei der Datenmigration. Überprüfe die Logs.")
            }
        }
        
        sender.sendMessage("${ChatColor.GREEN}EasyPlaytime Konfiguration wurde neu geladen!")
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> {
                val subcommands = mutableListOf("playtime")
                if (sender.hasPermission("easyplaytime.reload")) {
                    subcommands.add("reload")
                }
                subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                if (args[0].equals("playtime", ignoreCase = true)) {
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
