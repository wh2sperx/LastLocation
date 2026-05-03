package dev.arclyx0

import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class PlayerDataManager(
    private val plugin: LastLocation
) {
    private lateinit var connection: Connection

    fun initialize() {
        val dbFile = File(plugin.dataFolder, "playerdata").absolutePath
        connection = DriverManager.getConnection("jdbc:h2:$dbFile;MODE=MySQL")
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS player_locations (
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

    fun getPlayerBukkitLocation(uuid: UUID): Location? {
        connection.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM player_locations WHERE uuid = ?")
            .use { ps ->
                ps.setString(1, uuid.toString())
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    val world = Bukkit.getWorld(rs.getString("world")) ?: return null
                    return Location(
                        world,
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                    )
                }
            }
    }

    fun getSavedLocation(uuid: UUID): SavedLocation? {
        connection.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM player_locations WHERE uuid = ?")
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

    fun savePlayerLocation(loc: Location, uuid: UUID) {
        connection.prepareStatement(
            """
            MERGE INTO player_locations (uuid, world, x, y, z, yaw, pitch)
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

    fun removePlayerLocation(uuid: UUID) {
        connection.prepareStatement("DELETE FROM player_locations WHERE uuid = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeUpdate()
        }
    }

    fun close() {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }
}