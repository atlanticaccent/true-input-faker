@file:Suppress("unused")

package com.crimes_collection

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.SettingsAPI
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CampaignClockAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.price_of_command.reflection.ReflectionUtils
import org.apache.log4j.Level
import org.apache.log4j.Logger
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun Any.logger(): Logger {
    return Global.getLogger(this::class.java).apply { level = Level.ALL }
}

fun Any.debug(message: String) {
    this.logger().debug(message)
}

fun playerFleet(): CampaignFleetAPI {
    return Global.getSector().playerFleet
}

fun playerOfficers(): MutableList<OfficerDataAPI> {
    return playerFleet().fleetData.officersCopy
}

fun Long.toClock(): CampaignClockAPI {
    return Global.getSector().clock.createClock(this)
}

fun Long.toDateString(): String {
    return this.toClock().dateString
}

/**
 * Executes the given block if true
 */
fun Boolean.then(block: () -> Unit): Boolean {
    if (this) {
        block()
    }
    return this
}

/**
 * Returns the result of the given block if true otherwise null
 */
inline fun <T> Boolean.andThenOrNull(block: () -> T?): T? {
    return if (this) {
        block()
    } else {
        null
    }
}

fun clock(): CampaignClockAPI = Global.getSector().clock

fun IntelInfoPlugin.addToManager(notify: Boolean = false) {
    Global.getSector().intelManager.addIntel(this, !notify)
}

fun UIComponentAPI.getParent(): UIPanelAPI {
    return ReflectionUtils.invoke("getParent", this) as UIPanelAPI
}

fun createCustom(width: Float, height: Float, plugin: CustomUIPanelPlugin): CustomPanelAPI =
    Global.getSettings().createCustom(width, height, plugin)

fun createCustom(width: Float, height: Float) = createCustom(width, height, BaseCustomUIPanelPlugin())

@Suppress("UNCHECKED_CAST")
fun UIComponentAPI.getChildrenCopy(): List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
}

@Suppress("UNCHECKED_CAST")
fun UIComponentAPI.getChildrenNonCopy(): List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
}

fun List<OfficerDataAPI>.containsPerson(person: PersonAPI): Boolean = this.find { it.person == person } != null

fun settings(): SettingsAPI = Global.getSettings()

fun PersonAPI.getPossessiveSuffix() = if (nameString.endsWith('s')) {
    "'"
} else {
    "'s"
}

fun PersonAPI.possessive() = nameString + getPossessiveSuffix()

val os = System.getProperty("os.name").lowercase(Locale.getDefault())

fun <T> forPlatform(
    win: () -> T,
    linux: () -> T,
    macos: () -> T,
): T = when {
    os.contains("win") -> win()
    os.contains("nix") || os.contains("nux") || os.contains("aix") -> linux()
    os.contains("mac") -> macos()
    else -> throw Exception("Could not detect current platform")
}

fun <T> forPlatform(
    win: T, linux: T, macos: T
): T = forPlatform({ win }, { linux }, { macos })

fun <T, C : Collection<T>?> C.ifEmptyNull() = this?.ifEmpty { null }

// TODO: delegate that replaces property with null on get
open class ReadOnceProperty<T>(var value: T, private val default: T) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val temp = value
        value = default
        return temp
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun bypass() {}
}

open class TakingProperty<T>(value: T?) : ReadOnceProperty<T?>(value, null) {
    constructor() : this(null)
}

class CondTakingProperty<T>(private val block: () -> Boolean) : TakingProperty<T>() {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return if (block()) {
            value
        } else {
            super.getValue(thisRef, property)
        }
    }
}

fun Any.toPrettyDebugString(indentWidth: Int = 4) = buildString {
    fun StringBuilder.indent(level: Int) = append("".padStart(level * indentWidth))
    var ignoreSpace = false
    var indentLevel = 0
    this@toPrettyDebugString.toString().onEach {
        when (it) {
            '(', '[', '{' -> appendLine(it).indent(++indentLevel)
            ')', ']', '}' -> appendLine().indent(--indentLevel).append(it)
            ',' -> appendLine(it).indent(indentLevel).also { ignoreSpace = true }
            ' ' -> if (ignoreSpace) ignoreSpace = false else append(it)
            '=' -> append(" = ")
            else -> append(it)
        }
    }
}
