package com.crimes_collection.settings

import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener

class SettingsListener : LunaSettingsListener {
    companion object {
        fun register() {
            LunaSettings.addSettingsListener(SettingsListener())
        }
    }

    override fun settingsChanged(modID: String) {
        if (modID == Keys.MOD_ID) {
            int("step_dist")?.also { Keys.STEP_DIST = it }
            double("mult_mult")?.also { Keys.MULT_MULT = it }
            double("max_mult")?.also { Keys.MAX_MULT = it }

            keyboard("key_left")?.also { Keys.KEY_LEFT = it }
            keyboard("key_right")?.also { Keys.KEY_RIGHT = it }
            keyboard("key_up")?.also { Keys.KEY_UP = it }
            keyboard("key_down")?.also { Keys.KEY_DOWN = it }
            keyboard("key_click")?.also { Keys.KEY_CLICK = it }
            keyboard("key_click_alt")?.also { Keys.KEY_CLICK_ALT = it }
            keyboard("key_disable")?.also { Keys.KEY_DISABLE = it }
            keyboard("key_disable_alt")?.also { Keys.KEY_DISABLE_ALT = it }
        }
    }

    fun keyboard(id: String) = when (val key = int(id)) {
        0 -> null
        else -> key
    }

    fun int(id: String) = LunaSettings.getInt(Keys.MOD_ID, id)

    fun double(id: String) = LunaSettings.getDouble(Keys.MOD_ID, id)
}
