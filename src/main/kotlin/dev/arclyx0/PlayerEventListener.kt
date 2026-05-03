package dev.arclyx0

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerEventListener(private val plugin: LastLocation) : Listener {

    private val msg get() = plugin.messageManager

    private fun getSaveWorlds(): Set<String> =
        plugin.config.getStringList("save-worlds").map { it.lowercase() }.toSet()

    private fun getCommandWorlds(): Set<String> =
        plugin.config.getStringList("command-worlds").map { it.lowercase() }.toSet()

    private fun getFeatWorlds(): Set<String> =
        plugin.config.getStringList("feat-worlds").map { it.lowercase() }.toSet()

    /**
     * Combines command-worlds and feat-worlds for target-world checks.
     */
    private fun getNonSaveWorlds(): Set<String> = getCommandWorlds() + getFeatWorlds()

    // ── PlayerQuitEvent ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val location = player.location
        val worldName = location.world.name.lowercase()
        val saveWorlds = getSaveWorlds()
        val dm = plugin.playerDataManager

        try {
            if (worldName in saveWorlds) {
                // Player quit while in a save-world → save as disconnect location
                dm.saveDisconnectLocation(location, uuid)
                val (_, wcFlag) = dm.getFlags(uuid)
                dm.setFlags(uuid, disconnect = true, worldChange = wcFlag)
            } else {
                // Player quit outside save-world (lobby, feat-world, etc.)
                // If they have a pending world_change_location, promote it to disconnect
                val (_, wcFlag) = dm.getFlags(uuid)
                if (wcFlag) {
                    dm.transferWorldChangeToDisconnect(uuid)
                    dm.setFlags(uuid, disconnect = true, worldChange = false)
                }
            }
        } catch (_: Exception) {
        }
    }

    // ── PlayerTeleportEvent (cross-world) ────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val from = event.from
        val to = event.to
        if (from.world == to.world) return // same-world teleport, ignore

        val fromWorldName = from.world.name.lowercase()
        val toWorldName = to.world.name.lowercase()
        val saveWorlds = getSaveWorlds()
        val nonSaveWorlds = getNonSaveWorlds()

        // Only save when leaving a save-world TO a command/feat-world
        if (fromWorldName !in saveWorlds) return
        if (toWorldName !in nonSaveWorlds) return

        val player = event.player
        val uuid = player.uniqueId
        val dm = plugin.playerDataManager

        try {
            // Save the from-location (where they were in the save-world)
            dm.saveWorldChangeLocation(from, uuid)
            dm.setFlags(uuid, disconnect = false, worldChange = true)
            player.sendMessage(msg.getMessage(Messages.LOCATION_SAVED_TEMP))
        } catch (_: Exception) {
        }
    }

    // ── PlayerDeathEvent ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val deathLoc = player.location
        val worldName = deathLoc.world.name.lowercase()

        // Only cache death location if dying in a save-world
        if (worldName in getSaveWorlds()) {
            plugin.playerDataManager.deathLocationCache[player.uniqueId] = deathLoc
        }
    }

    // ── PlayerRespawnEvent ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val dm = plugin.playerDataManager

        val deathLoc = dm.deathLocationCache.remove(uuid) ?: return
        val respawnWorldName = event.respawnLocation.world.name.lowercase()

        // Only save if respawning in a non-save-world (command/feat-world)
        if (respawnWorldName !in getNonSaveWorlds()) return

        try {
            dm.saveWorldChangeLocation(deathLoc, uuid)
            dm.setFlags(uuid, disconnect = false, worldChange = true)
            player.sendMessage(msg.getMessage(Messages.LOCATION_SAVED_TEMP))
        } catch (_: Exception) {
        }
    }
}
