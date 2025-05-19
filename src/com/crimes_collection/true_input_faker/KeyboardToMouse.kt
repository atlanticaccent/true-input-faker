package com.crimes_collection.true_input_faker

import com.crimes_collection.true_input_faker.input_impls.FakeKeyboardInputImpl
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI

class KeyboardToMouse : BaseEveryFrameCombatPlugin() {
    var init = false

    override fun advance(amount: Float, events: List<InputEventAPI>) {
        if (!init) {
            FakeKeyboardInputImpl.getOrAttach()

            init = true
        }
    }
}
