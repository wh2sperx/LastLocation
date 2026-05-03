package dev.arclyx0

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener(private val plugin: LastLocation) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val location = player.location

        try {
            val saveWorlds = plugin.config.getStringList("save-worlds").map { it.lowercase() }
            if (location.world.name.lowercase() !in saveWorlds) return
            plugin.playerDataManager.savePlayerLocation(location, player.uniqueId)
        } catch (_: Exception) {}
    }
}
