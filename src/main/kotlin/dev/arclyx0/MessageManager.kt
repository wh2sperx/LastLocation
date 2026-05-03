package dev.arclyx0

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MessageManager(private val plugin: LastLocation) {

    private val miniMessage = MiniMessage.miniMessage()
    private val cache = mutableMapOf<String, String>()

    init {
        load()
    }

    fun load() {
        cache.clear()

        val file = File(plugin.dataFolder, "messages.yml")
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        for (key in config.getKeys(false)) {
            config.getString(key)?.let { cache[key] = it }
        }
    }

    fun getMessage(msg: Messages, vararg placeholders: Pair<String, String>): Component {
        return getMessage(msg.key, *placeholders)
    }

    fun getMessage(key: String, vararg placeholders: Pair<String, String>): Component {
        val raw = cache[key] ?: "<red>Missing message: $key</red>"

        val resolvers = placeholders.map { (tag, value) ->
            Placeholder.component(tag, Component.text(value))
        }.toTypedArray()

        return miniMessage.deserialize(raw, *resolvers)
    }
}
