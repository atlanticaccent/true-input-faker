@file:Suppress("unused")

package com.crimes_collection.true_input_faker.input_impls.mouse

import com.crimes_collection.true_input_faker.ReflectionUtils
import com.crimes_collection.true_input_faker.input_impls.mouse.event.MouseEvent
import com.crimes_collection.true_input_faker.input_impls.mouse.event.Other
import com.crimes_collection.true_input_faker.with
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.lwjgl.input.Mouse
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

object MouseLogger : EveryFrameScript {
    fun attach() {
        Global.getSector().addTransientScript(this)
    }

    override fun isDone() = false

    override fun runWhilePaused() = true

    override fun advance(amount: Float) {
        val coordBuf = getCoordBuffer()
        val event = MouseEvent(
            coordBuf[0] with coordBuf[1] with coordBuf[2], getMouseBufferCopy(), Other
        )
        println("mouse event: $event")
    }
}

fun getCoordBuffer(): IntBuffer = ReflectionUtils.getStatic(Mouse::class.java, "coord_buffer") as IntBuffer

fun getMouseBufferCopy(): ByteArray {
    val buf = (ReflectionUtils.getStatic(Mouse::class.java, "buttons") as ByteBuffer).duplicate()
    (buf as Buffer).rewind()
    val arr = ByteArray((buf as Buffer).remaining())
    buf.get(arr)
    return arr
}