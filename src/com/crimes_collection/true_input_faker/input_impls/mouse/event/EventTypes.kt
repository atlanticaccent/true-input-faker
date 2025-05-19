package com.crimes_collection.true_input_faker.input_impls.mouse.event

import com.crimes_collection.true_input_faker.input_impls.mouse.Buttons
import com.crimes_collection.true_input_faker.input_impls.mouse.event.EventType.Button
import com.crimes_collection.true_input_faker.input_impls.mouse.event.EventType.State
import com.crimes_collection.true_input_faker.with

object Move : EventType(), Button by Button.Move {
    override fun build(x: Int, y: Int, repeatable: Boolean) = MouseEvent(x with y with 0, Buttons(), this, repeatable)

    operator fun invoke(x: Int, y: Int, scroll: Int) = MouseEvent(x with y with scroll, Buttons(), this)
}

abstract class Click : EventType(), Button, State {
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

object LeftClick {
    object Down : Button by Button.Left, State by State.Down, Click()

    object Up : Button by Button.Left, State by State.Up, Click()
}

object RightClick {
    object Down : Button by Button.Right, State by State.Down, Click()

    object Up : Button by Button.Right, State by State.Up, Click()
}

object Other : EventType() {
    override fun build(
        x: Int, y: Int, repeatable: Boolean
    ) = MouseEvent(x with y with 0, Buttons(), Other, repeatable)
}

sealed class EventType {
    operator fun invoke(x: Int, y: Int, repeatable: Boolean = false) = build(x, y, repeatable)

    abstract fun build(x: Int, y: Int, repeatable: Boolean = false): MouseEvent

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
}