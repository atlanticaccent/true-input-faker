package com.crimes_collection.true_input_faker

import com.fs.starfarer.api.Global
import org.apache.log4j.Level
import org.apache.log4j.Logger

fun Any.logger(): Logger {
    return Global.getLogger(this::class.java).apply { level = Level.ALL }
}

infix fun <A> Pair<A, A>.with(other: A): Triple<A, A, A> {
    return Triple(this.first, this.second, other)
}

infix fun <T> T.with(other: T): Pair<T, T> {
    return this to other
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

fun Boolean.toInt() = if (this) 1 else 0

