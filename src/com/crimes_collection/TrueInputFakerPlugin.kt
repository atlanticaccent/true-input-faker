package com.crimes_collection

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import java.net.URL
import java.nio.file.Paths

class TrueInputFakerPlugin : BaseModPlugin() {
    companion object {
        internal val classLoader: ReflectionLoader by lazy {
            val url: URL = try {
                TrueInputFakerPlugin::class.java.getProtectionDomain().codeSource.location
            } catch (e: SecurityException) {
                try {
                    Paths.get("../mods/**/true_input_faker.jar").toUri().toURL()
                } catch (ex: Exception) {
//                    logger().error("Could not convert jar path to URL; exiting", ex)
                    throw Exception("Could not build custom classloader")
                }
            }

            ReflectionLoader(url, TrueInputFakerPlugin::class.java.getClassLoader())
        }
    }

    override fun onGameLoad(newGame: Boolean) {
        Global.getSector().listenerManager.addListener(KeyboardToMouse(), true)
    }
}
