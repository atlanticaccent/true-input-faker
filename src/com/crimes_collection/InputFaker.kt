package com.crimes_collection

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.InputImplementation
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun getInputImplementation() = ReflectionUtils.getStatic(
    Mouse::class.java, "implementation"
) as InputImplementation

fun setInputImplementation(inputImplWrapper: InputImplementation) {
    ReflectionUtils.setStatic(Mouse::class.java, "implementation", inputImplWrapper)
}

fun addMouseEventLogger() {
    Global.getSector().addTransientScript(object : EveryFrameScript {
        override fun isDone() = false

        override fun runWhilePaused() = true

        override fun advance(amount: Float) {
            val coordBuf = getCoordBuffer()
            val event = MouseEvent(
                coordBuf[0] with coordBuf[1] with coordBuf[2], getMouseBufferCopy(), MouseEvent.Other
            )
        }
    })
}

fun setMousePosition(x: Int, y: Int) {
    Mouse.setCursorPosition(x, y)
}

fun getMousePosition(): Pair<Int, Int> = Mouse.getX() to Mouse.getY()

fun getMouseBuffer(): ByteBuffer = ReflectionUtils.getStatic(Mouse::class.java, "buttons") as ByteBuffer

fun getCoordBuffer(): IntBuffer = ReflectionUtils.getStatic(Mouse::class.java, "coord_buffer") as IntBuffer

fun getReadBuffer(): ByteBuffer = ReflectionUtils.getStatic(Mouse::class.java, "readBuffer") as ByteBuffer

fun getMouseBufferCopy(): ByteArray {
    val buf = (ReflectionUtils.getStatic(Mouse::class.java, "buttons") as ByteBuffer).duplicate()
    (buf as Buffer).rewind()
    val arr = ByteArray(buf.remaining())
    buf.get(arr)
    return arr
}

private fun setMouseEventButton(idx: Int) {
    ReflectionUtils.setStatic(Mouse::class.java, "eventButton", idx)
}

private fun setMouseEventState(state: Boolean) {
    ReflectionUtils.setStatic(Mouse::class.java, "eventState", state)
}

class InputImplWrapper(
    private val inner: InputImplementation
) : InputImplementation by inner {
    companion object {
        var impl: InputImplWrapper? = null

        fun attach() = InputImplWrapper(getInputImplementation()).also {
            setInputImplementation(it)
            impl = it
        }

        fun getOrAttach(): InputImplWrapper {
            return when (val impl = impl) {
                null -> attach()
                else -> impl
            }
        }

        fun disconnect(): Boolean {
            return when (val currImpl = impl) {
                null -> false
                else -> {
                    impl = null
                    setInputImplementation(currImpl.inner)
                    return true
                }
            }
        }
    }

    private val events = mutableListOf<MouseEvent>()
    private var failsafeEvent: MouseEvent? = null
    var debug = true
    var readMousePrintNull = false
    var pollMousePrintNull = false
    var disabled = false
    val lock = ReentrantLock()

    private fun next(): MouseEvent.WriteableMouseEvent? {
        while (true) {
            val rawEvent = when (val event = events.firstOrNull()) {
                null -> {
                    val event = failsafeEvent ?: return null
                    failsafeEvent = null
                    events.add(event)
                    event
                }

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

    fun addEvent(event: MouseEvent) {
        if (failsafeEvent?.type == event.type) {
            failsafeEvent = null
        }
        events.add(event)
    }

    fun addFailsafeEvent(event: MouseEvent) {
        failsafeEvent = event
    }

    fun removeFailsafeEvent() {
        failsafeEvent = null
    }

    override fun readMouse(buf: ByteBuffer) {
        lock.withLock {
            if (disabled) {
                events.clear()
                inner.readMouse(buf)
                return
            }
            when (val event = next()) {
                null -> {
                    readMousePrintNull.then {
                        logger().debug("readMouse event was null")
                        readMousePrintNull = false
                    }
                }

                else -> {
                    debug.then {
                        logger().debug("readMouse: ${event.getDebugString()}")
                    }
                    readMousePrintNull = true
                    event.readMouse(buf)
                    val (xCoord, yCoord) = event.getCoords()
                    inner.setCursorPosition(xCoord, yCoord)
                }
            }
        }
    }

    override fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer) {
        lock.withLock {
            if (disabled) {
                inner.pollMouse(coordBuf, buttonBuf)
                return
            }
            when (val event = next()) {
                null -> {
                    pollMousePrintNull.then {
                        logger().debug("pollMouse event was null")
                        pollMousePrintNull = false
                    }
                }

                else -> {
                    debug.then {
                        logger().debug("pollMouse: ${event.getDebugString()}")
                    }
                    pollMousePrintNull = true
                    (coordBuf as Buffer).clear()
                    (buttonBuf as Buffer).clear()
                    event.pollMouse(coordBuf, buttonBuf)
                }
            }
        }
    }

    // TODO: nice setters for overriding coords, buttons, readBuf. Needs to translate buttons to correct index
}

fun Any?.identityHashCode() = System.identityHashCode(this)

fun ByteBuffer.zero() {
    this.also { (it as Buffer).clear() }.put(ByteArray(this.capacity()) { 0 }).also { (it as Buffer).rewind() }
}

fun ByteBuffer.copyToArray(): ByteArray {
    val dupe = this.duplicate().asReadOnlyBuffer()
    val arr = ByteArray((dupe as Buffer).limit())
    for (idx in arr.indices) {
        arr[idx] = dupe.get(idx)
    }
    return arr
}

data class RawMouseEvent(
    val button: Int,
    val state: Boolean,
    val deltaX: Int,
    val deltaY: Int,
    val deltaScroll: Int,
    val timestampNanos: Long,
) {
    companion object {
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

data class MouseEvent(
    private var coords: Triple<Int, Int, Int>,
    val buttons: ByteArray,
    val type: EventType,
    val repeatable: Boolean = true,
    val id: Int = MouseEvent.id++
) {
    companion object {
        var id = 0
    }

    private var read = false
    private var polled = false
    private val consumed: Boolean
        get() = read && polled
    var deltaX: Int? = null
    var deltaY: Int? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MouseEvent

        if (coords != other.coords) return false
        if (!buttons.contentEquals(other.buttons)) return false

        return true
    }

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
        deltaX?.let { setX(getY() + it) }
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

    override fun hashCode(): Int {
        var result = coords.hashCode()
        result = 31 * result + buttons.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + repeatable.hashCode()
        result = 31 * result + read.hashCode()
        result = 31 * result + polled.hashCode()
        return result
    }

    interface WriteableMouseEvent {
        fun readMouse(buf: ByteBuffer)

        fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer)

        fun getCoords(): Pair<Int, Int>

        fun getDebugString(): String
    }

    sealed class EventType {
        interface Builder {
            operator fun invoke(x: Int, y: Int, repeatable: Boolean = false) = build(x, y, repeatable)

            fun build(x: Int, y: Int, repeatable: Boolean = false): MouseEvent
        }

        interface State {
            val state: Boolean

            object Down : State {
                override val state = true
            }

            object Up : State {
                override val state = false
            }
        }

        interface Button {
            val button: Int

            object Move : Button {
                override val button = -1
            }

            object Left : Button {
                override val button = 0
            }

            object Right : Button {
                override val button = 1
            }
        }

        abstract class Click : EventType(), Builder, Button, State {
            override fun build(x: Int, y: Int, repeatable: Boolean) = MouseEvent(
                x with y with 0, Buttons(
                    if (state) {
                        1
                    } else {
                        0
                    }, button
                ), this, repeatable
            )
        }
    }

    object Other : EventType()
}

object Move : MouseEvent.EventType(), MouseEvent.EventType.Builder, MouseEvent.EventType.Button by Button.Move {
    override fun build(x: Int, y: Int, repeatable: Boolean) = MouseEvent(x with y with 0, Buttons(), this, repeatable)
}

object LeftClick {
    object Down : MouseEvent.EventType.Button by Button.Left, MouseEvent.EventType.State by State.Down,
        MouseEvent.EventType.Click()

    object Up : MouseEvent.EventType.Button by Button.Left, MouseEvent.EventType.State by State.Up,
        MouseEvent.EventType.Click()
}

object RightClick {
    object Down : MouseEvent.EventType.Button by Button.Right, MouseEvent.EventType.State by State.Down,
        MouseEvent.EventType.Click()

    object Up : MouseEvent.EventType.Button by Button.Right, MouseEvent.EventType.State by State.Up,
        MouseEvent.EventType.Click()
}

infix fun <A> Pair<A, A>.with(other: A): Triple<A, A, A> {
    return Triple(this.first, this.second, other)
}

infix fun <T> T.with(other: T): Pair<T, T> {
    return this to other
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