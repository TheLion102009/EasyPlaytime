package de.thelion.easyPlaytime.placeholders

import de.thelion.easyPlaytime.EasyPlaytime
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class PlaytimePlaceholder(private val plugin: EasyPlaytime) : PlaceholderExpansion() {
    
    override fun getIdentifier(): String = "easyplaytime"
    
    override fun getAuthor(): String = "thelion"
    
    override fun getVersion(): String = plugin.description.version
    
    override fun persist(): Boolean = true
    
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        
        return when (params.lowercase()) {
            "playtime" -> plugin.playtimeManager.getFormattedPlaytime(player)
            else -> null
        }
    }
}
