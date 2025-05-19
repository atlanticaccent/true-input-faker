package com.crimes_collection.true_input_faker.input_impls.mouse.event

import com.crimes_collection.true_input_faker.toPrettyDebugString
import com.crimes_collection.true_input_faker.with
import org.lwjgl.input.Mouse
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

data class MouseEvent(
    private var coords: Triple<Int, Int, Int>,
    val buttons: ByteArray,
    val type: EventType,
    val repeatable: Boolean = false,
) {
    companion object {
        var id = 0
    }

    val id: Int = MouseEvent.id++
    private var read = false
    private var polled = false
    private val consumed: Boolean
        get() = read && polled
    var deltaX: Int? = null
    var deltaY: Int? = null

    fun consume(): WriteableMouseEvent? {
        return if (repeatable || !consumed) {
            object : WriteableMouseEvent {
                val rawEvent = RawMouseEvent(
                    button = (type as? EventType.Button)?.button ?: -1,
                    state = (type as? EventType.State)?.state ?: false,
                    deltaX = coords.first,
                    deltaY = coords.second,
                    deltaScroll = coords.third,
                    timestampNanos = System.nanoTime()
                )

                override fun readMouse(buf: ByteBuffer) {
                    rawEvent.writeToByteBuffer(buf)
                    read = true
                }

                override fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer) {
                    coords.toList().forEachIndexed { idx, coord ->
                        coordBuf.put(idx, coord)
                    }
                    val buttonBuffer = buttonBuf as Buffer
                    buttonBuffer.clear()
                    buttons.forEachIndexed { index, byte ->
                        buttonBuf.put(index, byte)
                    }
                    buttonBuffer.position(0)
                    buttonBuffer.limit(Mouse.getButtonCount())
                    polled = true
                }

                override fun getCoords(): Pair<Int, Int> = coords.first to coords.second

                override fun getDebugString(): String = this@MouseEvent.toPrettyDebugString()
            }
        } else {
            null
        }
    }

    fun getX() = coords.first

    fun deltaX(deltaX: Int?) {
        this.deltaX = deltaX
        deltaX?.let { setX(getX() + it) }
    }

    fun setX(x: Int) {
        val (_, y, scroll) = coords
        coords = x with y with scroll
    }

    fun getY() = coords.second

    fun deltaY(deltaY: Int?) {
        this.deltaY = deltaY
        deltaY?.let { setY(getY() + it) }
    }

    fun setY(y: Int) {
        val (x, _, scroll) = coords
        coords = x with y with scroll
    }

    interface WriteableMouseEvent {
        fun readMouse(buf: ByteBuffer)

        fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer)

        fun getCoords(): Pair<Int, Int>

        fun getDebugString(): String
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MouseEvent

        if (repeatable != other.repeatable) return false
        if (id != other.id) return false
        if (read != other.read) return false
        if (polled != other.polled) return false
        if (deltaX != other.deltaX) return false
        if (deltaY != other.deltaY) return false
        if (coords != other.coords) return false
        if (!buttons.contentEquals(other.buttons)) return false
        if (type != other.type) return false
        if (consumed != other.consumed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coords.hashCode()
        result = 31 * result + buttons.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + repeatable.hashCode()
        result = 31 * result + read.hashCode()
        result = 31 * result + polled.hashCode()
        return result
    }
}