package com.crimes_collection.true_input_faker.settings

import com.crimes_collection.true_input_faker.logger
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import kotlin.reflect.KMutableProperty0

class SettingsListener : LunaSettingsListener {
    companion object {
        fun register() {
            LunaSettings.addSettingsListener(SettingsListener())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun settingsChanged(modID: String) {
        fun KMutableProperty0<*>.name(): String = name.lowercase()

        if (modID == Keys.MOD_ID) {
            for ((keySet, getter) in mapOf(
                Keys.INT to ::int, Keys.DOUBLE to ::double, Keys.KB to ::keyboard
            )) {
                for (property in keySet) {
                    val value = getter(property.name()) ?: continue
                    when (value) {
                        is Int -> (property as KMutableProperty0<Int>).set(value)
                        is Double -> (property as KMutableProperty0<Double>).set(value)
                    }
                }
            }
        }
    }
}

fun keyboard(id: String) = when (val key = int(id)) {
    0 -> null
    else -> key
}

fun int(id: String) = LunaSettings.getInt(Keys.MOD_ID, id)

fun double(id: String) = LunaSettings.getDouble(Keys.MOD_ID, id)
