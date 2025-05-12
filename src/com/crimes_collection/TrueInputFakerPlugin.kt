package com.crimes_collection

import com.crimes_collection.settings.SettingsListener
import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import java.net.URL
import java.nio.file.Paths

class TrueInputFakerPlugin : BaseModPlugin() {
    companion object {
        internal val classLoader: ReflectionLoader by lazy {
            val url: URL = try {
                TrueInputFakerPlugin::class.java.protectionDomain.codeSource.location
            } catch (e: SecurityException) {
                try {
                    Paths.get("../mods/**/true_input_faker.jar").toUri().toURL()
                } catch (ex: Exception) {
//                    logger().error("Could not convert jar path to URL; exiting", ex)
                    throw Exception("Could not build custom classloader")
                }
            }

            ReflectionLoader(url, TrueInputFakerPlugin::class.java.classLoader)
        }
    }

    override fun onApplicationLoad() {
        if (Global.getSettings().modManager.isModEnabled("lunalib")) {
            SettingsListener.register()
        }
    }

    override fun onGameLoad(newGame: Boolean) {
        Global.getSector().listenerManager.addListener(KeyboardToMouse(), true)
    }
}
