package com.crimes_collection.true_input_faker.input_impls.mouse

import com.crimes_collection.true_input_faker.input_impls.FakeInputImpl
import com.crimes_collection.true_input_faker.input_impls.mouse.FakeMouseInputImpl.Companion.into
import com.crimes_collection.true_input_faker.input_impls.InputImplFactory
import com.crimes_collection.true_input_faker.input_impls.mouse.event.MouseEvent
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.InputImplementation
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FakeMouseInputImpl(
    inner: InputImplementation
) : FakeInputImpl<MouseEvent>(inner) {
    companion object : InputImplFactory<FakeMouseInputImpl>(into<Mouse> { FakeMouseInputImpl(it) })

    private val events = mutableListOf<MouseEvent>()
    val lock = ReentrantLock()

    private fun next(): MouseEvent.WriteableMouseEvent? {
        while (true) {
            val rawEvent = when (val event = events.firstOrNull()) {
                null -> return null

                else -> event
            }
            when (val event = rawEvent.consume()) {
                null -> {
                    events.removeFirstOrNull()
                    null
                }

                else -> return event
            }
        }
    }

    override fun addEvent(event: MouseEvent) {
        events.add(event)
    }

    override fun readMouse(buf: ByteBuffer) {
        lock.withLock {
            if (disabled) {
                events.clear()
                inner.readMouse(buf)
                return
            }
            val event = next() ?: return
            event.readMouse(buf)
            val (x_coord, y_coord) = event.getCoords()
            inner.setCursorPosition(x_coord, y_coord)
        }
    }

    override fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer) {
        lock.withLock {
            if (disabled) {
                inner.pollMouse(coordBuf, buttonBuf)
                return
            }
            val event = next() ?: return
            (coordBuf as Buffer).clear()
            (buttonBuf as Buffer).clear()
            event.pollMouse(coordBuf, buttonBuf)
        }
    }
}

sealed class Buttons {
    companion object {
        operator fun invoke(byte: Byte, index: Int): ByteArray {
            val array = ByteArray(Mouse.getButtonCount())
            array[index] = byte
            return array
        }

        operator fun invoke(): ByteArray = ByteArray(Mouse.getButtonCount())
    }
}