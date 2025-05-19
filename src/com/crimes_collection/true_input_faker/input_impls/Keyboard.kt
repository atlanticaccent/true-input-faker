package com.crimes_collection.true_input_faker.input_impls

import com.crimes_collection.true_input_faker.input_impls.FakeKeyboardInputImpl.Companion.into
import com.crimes_collection.true_input_faker.input_impls.mouse.FakeMouseInputImpl
import com.crimes_collection.true_input_faker.input_impls.mouse.event.LeftClick
import com.crimes_collection.true_input_faker.input_impls.mouse.event.Move
import com.crimes_collection.true_input_faker.input_impls.mouse.event.RightClick
import com.crimes_collection.true_input_faker.settings.Keys
import com.crimes_collection.true_input_faker.settings.Keys.KEY_CLICK
import com.crimes_collection.true_input_faker.settings.Keys.KEY_CLICK_ALT
import com.crimes_collection.true_input_faker.settings.Keys.KEY_DISABLE
import com.crimes_collection.true_input_faker.settings.Keys.KEY_DISABLE_ALT
import com.crimes_collection.true_input_faker.settings.Keys.KEY_DOWN
import com.crimes_collection.true_input_faker.settings.Keys.KEY_LEFT
import com.crimes_collection.true_input_faker.settings.Keys.KEY_RCLICK
import com.crimes_collection.true_input_faker.settings.Keys.KEY_RCLICK_ALT
import com.crimes_collection.true_input_faker.settings.Keys.KEY_RIGHT
import com.crimes_collection.true_input_faker.settings.Keys.KEY_UP
import com.crimes_collection.true_input_faker.settings.Keys.KEY_ZOOM_IN
import com.crimes_collection.true_input_faker.settings.Keys.KEY_ZOOM_OUT
import com.crimes_collection.true_input_faker.toInt
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.InputImplementation
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

class FakeKeyboardInputImpl(inner: InputImplementation) : FakeInputImpl<KeyEvent>(inner) {
    companion object : InputImplFactory<FakeKeyboardInputImpl>(into<Keyboard>(::FakeKeyboardInputImpl))

    lateinit var keydownBuffer: ByteBuffer
    val eventListeners = mutableListOf<KeyboardEventListener>().apply { add(processEvents()) }
    val lock = ReentrantLock()

    fun processEvents(): (List<KeyEvent>) -> Unit {
        var lastTimestamp: Long = 0

        var leftActive = false
        var rightActive = false
        var upActive = false
        var downActive = false
        var zoomInActive = false
        var zoomOutActive = false
        val deactivateAll = {
            leftActive = false
            rightActive = false
            upActive = false
            downActive = false
            zoomInActive = false
            zoomOutActive = false
        }
        var mult = 1.0
        var lastTap: Long? = null
        return { events: List<KeyEvent> ->
            val inputImpl = FakeMouseInputImpl.getOrAttach()

            var x_coord = Mouse.getX()
            var y_coord = Mouse.getY()
            for (event in events) {
                if (event.nanos > lastTimestamp) {
                    lastTimestamp = event.nanos
                    when (event.key) {
                        KEY_LEFT -> leftActive = event.state

                        KEY_RIGHT -> rightActive = event.state

                        KEY_UP -> upActive = event.state

                        KEY_DOWN -> downActive = event.state

                        KEY_ZOOM_IN -> zoomInActive = event.state

                        KEY_ZOOM_OUT -> zoomOutActive = event.state

                        KEY_DISABLE, KEY_DISABLE_ALT -> {
                            if (event.state) {
                                val timestamp = System.currentTimeMillis()
                                lastTap = if (lastTap?.let { timestamp - it < 5000 } ?: false) {
                                    inputImpl.disabled = !inputImpl.disabled
                                    null
                                } else {
                                    timestamp
                                }
                            }
                        }

                        KEY_CLICK, KEY_CLICK_ALT -> {
                            val click = if (event.state) {
                                LeftClick.Down(x_coord, y_coord)
                            } else {
                                LeftClick.Up(x_coord, y_coord)
                            }
                            inputImpl.addEvent(click)
                            deactivateAll()
                            continue
                        }

                        KEY_RCLICK, KEY_RCLICK_ALT -> {
                            val click = if (event.state) {
                                RightClick.Down(x_coord, y_coord)
                            } else {
                                RightClick.Up(x_coord, y_coord)
                            }
                            inputImpl.addEvent(click)
                            deactivateAll()
                            continue
                        }

                        else -> {
                            deactivateAll()
                            continue
                        }
                    }

                    if (event.repeat) {
                        mult *= Keys.MULT_MULT
                        if (mult > Keys.MAX_MULT) {
                            mult = Keys.MAX_MULT
                        }
                    } else {
                        mult = 1.0
                    }
                    var deltaX = (leftActive.toInt() * -Keys.STEP_DIST) + (rightActive.toInt() * Keys.STEP_DIST)
                    var deltaY = (upActive.toInt() * Keys.STEP_DIST) + (downActive.toInt() * -Keys.STEP_DIST)
                    deltaX = (deltaX * mult).roundToInt()
                    deltaY = (deltaY * mult).roundToInt()
                    val scroll = (zoomInActive.toInt() * Keys.STEP_DIST) + (zoomOutActive.toInt() * -Keys.STEP_DIST)
                    x_coord += deltaX
                    y_coord += deltaY
                    inputImpl.addEvent(Move(x_coord, y_coord, scroll))
                }
            }
        }
    }

    override fun pollKeyboard(downBuffer: ByteBuffer) {
        lock.withLock {
            inner.pollKeyboard(downBuffer)
            keydownBuffer = downBuffer
        }
    }

    override fun readKeyboard(buffer: ByteBuffer) {
        lock.withLock {
            inner.readKeyboard(buffer)

            val events = getEvents(buffer)
            for (listener in eventListeners) {
                listener.processEvents(events)
            }
        }
    }

    fun getEvents(buffer: ByteBuffer): List<KeyEvent> {
        val buf = buffer as Buffer
        val position = buf.position()

        val out = mutableListOf<KeyEvent>()
        while (true) {
            val event = KeyEvent.readNext(buffer, keydownBuffer) ?: break
            if (!event.repeat || Keyboard.areRepeatEventsEnabled()) {
                out.add(event)
            }
        }

        buf.position(position)
        return out
    }

    override fun addEvent(event: KeyEvent) = Unit
}

data class KeyEvent(
    val key: Int, val state: Boolean, val character: Int, val nanos: Long, val repeat: Boolean
) {
    companion object {
        const val ZERO: Byte = 0
        const val ONE: Byte = 1

        fun readNext(buffer: ByteBuffer, keydownBuffer: ByteBuffer): KeyEvent? =
            if ((buffer as Buffer).hasRemaining()) {
                val key = buffer.getInt() and 255
                KeyEvent(
                    key,
                    state = buffer.get() != ZERO && keydownBuffer.get(key) != ZERO,
                    character = buffer.getInt(),
                    nanos = buffer.getLong(),
                    repeat = buffer.get() == ONE
                )
            } else {
                null
            }
    }
}

fun interface KeyboardEventListener {
    fun processEvents(events: List<KeyEvent>)
}
