package com.crimes_collection.true_input_faker.settings

import org.lwjgl.input.Keyboard

data object Keys {
    const val MOD_ID = "true_input_faker"

    var STEP_DIST = 5
    var MULT_MULT = 1.05
    var MAX_MULT = 3.0

    var KEY_LEFT = Keyboard.KEY_LEFT
    var KEY_RIGHT = Keyboard.KEY_RIGHT
    var KEY_UP = Keyboard.KEY_UP
    var KEY_DOWN = Keyboard.KEY_DOWN
    var KEY_CLICK = Keyboard.KEY_F1
    var KEY_CLICK_ALT = Keyboard.KEY_DELETE
    var KEY_RCLICK = Keyboard.KEY_F2
    var KEY_RCLICK_ALT = Keyboard.KEY_END
    var KEY_ZOOM_IN = Keyboard.KEY_PRIOR
    var KEY_ZOOM_OUT = Keyboard.KEY_NEXT
    var KEY_DISABLE = Keyboard.KEY_LSHIFT
    var KEY_DISABLE_ALT = Keyboard.KEY_RSHIFT

    val INT = listOf(
        Keys::STEP_DIST
    )

    val DOUBLE = listOf(
        Keys::MULT_MULT,
        Keys::MAX_MULT,
    )

    val KB = listOf(
        Keys::KEY_RIGHT,
        Keys::KEY_UP,
        Keys::KEY_DOWN,
        Keys::KEY_CLICK,
        Keys::KEY_CLICK_ALT,
        Keys::KEY_RCLICK,
        Keys::KEY_RCLICK_ALT,
        Keys::KEY_ZOOM_IN,
        Keys::KEY_ZOOM_OUT,
        Keys::KEY_DISABLE,
        Keys::KEY_DISABLE_ALT,
    )
}
