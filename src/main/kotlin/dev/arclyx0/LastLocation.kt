package dev.arclyx0

import org.bukkit.plugin.java.JavaPlugin
import org.mvplugins.multiverse.core.MultiverseCoreApi

class LastLocation : JavaPlugin() {

    lateinit var playerDataManager: PlayerDataManager
        private set

    lateinit var multiverseCore: MultiverseCoreApi
        private set

    lateinit var messageManager: MessageManager
        private set

    override fun onEnable() {
        saveDefaultConfig()
        messageManager = MessageManager(this)
        playerDataManager = PlayerDataManager(this); playerDataManager.initialize()
        multiverseCore = MultiverseCoreApi.get()
        server.pluginManager.registerEvents(PlayerEventListener(this), this)
        getCommand("lastlocation")?.setExecutor(LastLocationCommand(this))
    }

    override fun onDisable() {
        if (::playerDataManager.isInitialized) {
            playerDataManager.close()
        }
    }
}
