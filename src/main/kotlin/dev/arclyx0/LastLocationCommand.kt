package dev.arclyx0

import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LastLocationCommand(private val plugin: LastLocation) : CommandExecutor {
    private val msg get() = plugin.messageManager

    private fun getCommandWorlds(): List<String> {
        return plugin.config.getStringList("command-worlds").map { it.lowercase() }
    }

    private fun getFeatWorlds(): List<String> {
        return plugin.config.getStringList("feat-worlds").map { it.lowercase() }
    }

    /**
     * All worlds where /lastlocation can be used (command-worlds + feat-worlds).
     */
    private fun getAllowedWorlds(): List<String> = getCommandWorlds() + getFeatWorlds()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (args.size == 1 && args[0].equals("reload", ignoreCase = true)) {
            if (!sender.hasPermission("arclyx0.admin")) {
                sender.sendMessage(msg.getMessage(Messages.NO_PERMISSION_RELOAD))
                return true
            }
            plugin.reloadConfig()
            msg.load()
            sender.sendMessage(msg.getMessage(Messages.RELOAD_SUCCESS))
            return true
        }

        if (args.size == 1) {
            if (!sender.hasPermission("arclyx0.teleport.others")) {
                sender.sendMessage(msg.getMessage(Messages.NO_PERMISSION_TELEPORT_OTHERS))
                return true
            }

            val targetPlayer = plugin.server.getPlayer(args[0])
            if (targetPlayer == null) {
                sender.sendMessage(msg.getMessage(Messages.PLAYER_NOT_ONLINE, "player" to args[0]))
                return true
            }

            teleportPlayer(targetPlayer, sender)
            return true
        }

        if (args.isEmpty()) {
            if (sender !is Player) {
                sender.sendMessage(msg.getMessage(Messages.CONSOLE_MUST_SPECIFY_PLAYER, "label" to label))
                return true
            }

            if (!sender.hasPermission("arclyx0.teleport.self")) {
                sender.sendMessage(msg.getMessage(Messages.NO_PERMISSION_TELEPORT_SELF))
                return true
            }

            val allowedWorlds = getAllowedWorlds()
            if (allowedWorlds.isNotEmpty()) {
                val currentWorld = sender.world.name.lowercase()
                if (currentWorld !in allowedWorlds) {
                    sender.sendMessage(
                        msg.getMessage(
                            Messages.WRONG_WORLD,
                            "worlds" to allowedWorlds.joinToString(", ")
                        )
                    )
                    return true
                }
            }

            teleportPlayer(sender, null)
            return true
        }

        return false
    }

    private fun teleportPlayer(target: Player, executor: CommandSender? = null) {
        val uuid = target.uniqueId
        val dm = plugin.playerDataManager
        val multiverseCore = plugin.multiverseCore
        val (disconnectFlag, worldChangeFlag) = dm.getFlags(uuid)

        // Priority 1: world_change_flag == true
        // Priority 2: disconnect_flag == true
        // Priority 3: both false → default spawn
        val savedLoc: SavedLocation?
        val locationType: LocationType

        when {
            worldChangeFlag -> {
                savedLoc = dm.getWorldChangeLocation(uuid)
                locationType = LocationType.WORLD_CHANGE
            }
            disconnectFlag -> {
                savedLoc = dm.getDisconnectLocation(uuid)
                locationType = LocationType.DISCONNECT
            }
            else -> {
                savedLoc = null
                locationType = LocationType.DEFAULT
            }
        }

        val mvWorld = savedLoc?.let {
            multiverseCore.worldManager.getLoadedWorld(it.worldName).orNull
        }

        val isSavedLocation: Boolean
        val destination = if (mvWorld != null) {
            isSavedLocation = true
            Location(
                mvWorld.bukkitWorld.orNull,
                savedLoc.x, savedLoc.y, savedLoc.z,
                savedLoc.yaw, savedLoc.pitch
            )
        } else {
            isSavedLocation = false
            val defaultWorldName = plugin.config.getString("default-world", "qhuyy")
            if (defaultWorldName == null) {
                target.sendMessage(msg.getMessage(Messages.DEFAULT_WORLD_NOT_FOUND, "world" to "qhuyy"))
                return
            }

            val defaultWorld = multiverseCore.worldManager.getLoadedWorld(defaultWorldName).orNull
            if (defaultWorld == null) {
                target.sendMessage(msg.getMessage(Messages.DEFAULT_WORLD_NOT_FOUND, "world" to defaultWorldName))
                return
            }

            defaultWorld.spawnLocation
        }

        if (destination == null) {
            val failMsg = if (executor != null) Messages.TELEPORT_OTHERS_FAILED else Messages.TELEPORT_FAILED
            (executor ?: target).sendMessage(msg.getMessage(failMsg, "player" to target.name))
            return
        }

        multiverseCore.safetyTeleporter
            .to(destination)
            .by(target)
            .checkSafety(false)
            .teleport(target)
            .onSuccess {
                if (isSavedLocation) {
                    when (locationType) {
                        LocationType.WORLD_CHANGE -> {
                            dm.removeWorldChangeLocation(uuid)
                            dm.setFlags(uuid, disconnect = dm.getFlags(uuid).first, worldChange = false)
                        }
                        LocationType.DISCONNECT -> {
                            // After teleporting from disconnect_location → clear ALL records
                            // so that next time they leave the world, a fresh world_change_location is created
                            dm.removeDisconnectLocation(uuid)
                            dm.removeWorldChangeLocation(uuid)
                            dm.setFlags(uuid, disconnect = false, worldChange = false)
                        }
                        LocationType.DEFAULT -> { /* no-op */ }
                    }
                }
                if (executor != null) {
                    val successMsg =
                        if (isSavedLocation) Messages.TELEPORT_OTHERS_SUCCESS else Messages.TELEPORT_OTHERS_DEFAULT_SPAWN
                    executor.sendMessage(msg.getMessage(successMsg, "player" to target.name))
                } else {
                    val successMsg = if (isSavedLocation) Messages.TELEPORT_SUCCESS else Messages.TELEPORT_DEFAULT_SPAWN
                    target.sendMessage(msg.getMessage(successMsg))
                }
            }
    }

    private enum class LocationType {
        WORLD_CHANGE, DISCONNECT, DEFAULT
    }
}