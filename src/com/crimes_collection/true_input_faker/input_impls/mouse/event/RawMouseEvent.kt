package com.crimes_collection.true_input_faker.input_impls.mouse.event

import org.lwjgl.input.Mouse
import java.nio.Buffer
import java.nio.ByteBuffer

data class RawMouseEvent(
    val button: Int,
    val state: Boolean,
    val deltaX: Int,
    val deltaY: Int,
    val deltaScroll: Int,
    val timestampNanos: Long,
) {
    companion object {
        @Suppress("unused")
        fun fromByteBufferComplete(byteBuffer: ByteBuffer): List<RawMouseEvent> {
            val buf = byteBuffer.duplicate()
            (buf as Buffer).position(0)
            val events = mutableListOf<RawMouseEvent>()
            for (baseIdx in 0 until (buf as Buffer).limit() step Mouse.EVENT_SIZE) {
                events.add(fromByteBuffer(buf))
            }
            return events
        }

        fun fromByteBuffer(buffer: ByteBuffer, duplicate: Boolean = false, position: Int? = null): RawMouseEvent {
            val buf = if (duplicate) {
                buffer.duplicate()
            } else {
                buffer
            }
            position?.also { (buf as Buffer).position(it) }
            return RawMouseEvent(
                buf.get().toInt(), buf.get() > 0, buf.getInt(), buf.getInt(), buf.getInt(), buf.getLong()
            )
        }
    }

    fun writeToByteBuffer(buffer: ByteBuffer, position: Int? = null) {
        position?.let {
            (buffer as Buffer).position(it)
        }
        buffer.put(button.toByte())
        buffer.put(
            if (state) {
                1
            } else {
                0
            }
        )
        buffer.putInt(deltaX)
        buffer.putInt(deltaY)
        buffer.putInt(deltaScroll)
        buffer.putLong(timestampNanos)
    }
}