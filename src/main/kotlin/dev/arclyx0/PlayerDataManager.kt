package dev.arclyx0

import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class PlayerDataManager(
    private val plugin: LastLocation
) {
    private lateinit var connection: Connection

    /**
     * Runtime flags: Pair(disconnect_flag, world_change_flag).
     * Only lives in RAM — lost on server restart.
     */
    private val playerFlags = ConcurrentHashMap<UUID, Pair<Boolean, Boolean>>()



    fun initialize() {
        val dbFile = File(plugin.dataFolder, "playerdata").absolutePath
        connection = DriverManager.getConnection("jdbc:h2:$dbFile;MODE=MySQL")
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS disconnect_locations (
                    uuid VARCHAR(36) PRIMARY KEY,
                    world VARCHAR(255) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw FLOAT NOT NULL,
                    pitch FLOAT NOT NULL
                )
            """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS world_change_locations (
                    uuid VARCHAR(36) PRIMARY KEY,
                    world VARCHAR(255) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw FLOAT NOT NULL,
                    pitch FLOAT NOT NULL
                )
            """.trimIndent()
            )
        }
    }

    // ── Disconnect Location ──────────────────────────────────────────────

    fun saveDisconnectLocation(loc: Location, uuid: UUID) {
        connection.prepareStatement(
            """
            MERGE INTO disconnect_locations (uuid, world, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, loc.world.name)
            ps.setDouble(3, loc.x)
            ps.setDouble(4, loc.y)
            ps.setDouble(5, loc.z)
            ps.setFloat(6, loc.yaw)
            ps.setFloat(7, loc.pitch)
            ps.executeUpdate()
        }
    }

    fun getDisconnectLocation(uuid: UUID): SavedLocation? {
        connection.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM disconnect_locations WHERE uuid = ?")
            .use { ps ->
                ps.setString(1, uuid.toString())
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return SavedLocation(
                        worldName = rs.getString("world"),
                        x = rs.getDouble("x"),
                        y = rs.getDouble("y"),
                        z = rs.getDouble("z"),
                        yaw = rs.getFloat("yaw"),
                        pitch = rs.getFloat("pitch")
                    )
                }
            }
    }

    fun removeDisconnectLocation(uuid: UUID) {
        connection.prepareStatement("DELETE FROM disconnect_locations WHERE uuid = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeUpdate()
        }
    }

    // ── World Change Location ────────────────────────────────────────────

    fun saveWorldChangeLocation(loc: Location, uuid: UUID) {
        connection.prepareStatement(
            """
            MERGE INTO world_change_locations (uuid, world, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, loc.world.name)
            ps.setDouble(3, loc.x)
            ps.setDouble(4, loc.y)
            ps.setDouble(5, loc.z)
            ps.setFloat(6, loc.yaw)
            ps.setFloat(7, loc.pitch)
            ps.executeUpdate()
        }
    }

    fun getWorldChangeLocation(uuid: UUID): SavedLocation? {
        connection.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM world_change_locations WHERE uuid = ?")
            .use { ps ->
                ps.setString(1, uuid.toString())
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return SavedLocation(
                        worldName = rs.getString("world"),
                        x = rs.getDouble("x"),
                        y = rs.getDouble("y"),
                        z = rs.getDouble("z"),
                        yaw = rs.getFloat("yaw"),
                        pitch = rs.getFloat("pitch")
                    )
                }
            }
    }

    fun removeWorldChangeLocation(uuid: UUID) {
        connection.prepareStatement("DELETE FROM world_change_locations WHERE uuid = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeUpdate()
        }
    }

    // ── Transfer: world_change → disconnect ──────────────────────────────

    /**
     * Copy world_change_location to disconnect_location, then delete world_change_location.
     * Used when player logs out outside a save-world but has a pending world_change record.
     */
    fun transferWorldChangeToDisconnect(uuid: UUID) {
        val loc = getWorldChangeLocation(uuid) ?: return
        connection.prepareStatement(
            """
            MERGE INTO disconnect_locations (uuid, world, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, loc.worldName)
            ps.setDouble(3, loc.x)
            ps.setDouble(4, loc.y)
            ps.setDouble(5, loc.z)
            ps.setFloat(6, loc.yaw)
            ps.setFloat(7, loc.pitch)
            ps.executeUpdate()
        }
        removeWorldChangeLocation(uuid)
    }

    // ── Runtime Flags ────────────────────────────────────────────────────

    fun setFlags(uuid: UUID, disconnect: Boolean, worldChange: Boolean) {
        playerFlags[uuid] = Pair(disconnect, worldChange)
    }

    fun getFlags(uuid: UUID): Pair<Boolean, Boolean> {
        return playerFlags.getOrDefault(uuid, Pair(false, false))
    }

    fun clearFlags(uuid: UUID) {
        playerFlags.remove(uuid)
    }

    // Legacy Compat

    fun getPlayerBukkitLocation(uuid: UUID): Location? {
        val saved = getDisconnectLocation(uuid)
            ?: getWorldChangeLocation(uuid)
            ?: return null
        val world = Bukkit.getWorld(saved.worldName) ?: return null
        return Location(world, saved.x, saved.y, saved.z, saved.yaw, saved.pitch)
    }

    /**
     * Flush all world_change locations to disconnect_locations in the database.
     * Called on plugin disable to persist any pending world_change records.
     */
    fun flushWorldChangeToDisconnect() {
        val uuidsWithWorldChange = playerFlags.entries
            .filter { it.value.second } // world_change flag == true
            .map { it.key }

        for (uuid in uuidsWithWorldChange) {
            try {
                transferWorldChangeToDisconnect(uuid)
                setFlags(uuid, disconnect = true, worldChange = false)
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Failed to flush world_change for $uuid", e)
            }
        }
    }

    fun close() {
        flushWorldChangeToDisconnect()

        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }
}