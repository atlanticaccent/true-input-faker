package com.crimes_collection

import com.crimes_collection.settings.Keys
import com.crimes_collection.settings.Keys.KEY_CLICK
import com.crimes_collection.settings.Keys.KEY_CLICK_ALT
import com.crimes_collection.settings.Keys.KEY_DISABLE
import com.crimes_collection.settings.Keys.KEY_DISABLE_ALT
import com.crimes_collection.settings.Keys.KEY_DOWN
import com.crimes_collection.settings.Keys.KEY_LEFT
import com.crimes_collection.settings.Keys.KEY_RIGHT
import com.crimes_collection.settings.Keys.KEY_UP
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import org.lwjgl.input.Mouse
import kotlin.math.roundToInt

class KeyboardToMouse : CampaignInputListener, BaseEveryFrameCombatPlugin() {
    private var mult = 1.0
    private var lastTap: Long? = null
    private var leftActive = false
    private var rightActive = false
    private var upActive = false
    private var downActive = false

    override fun getListenerInputPriority() = Int.MAX_VALUE

    override fun processCampaignInputPreCore(events: MutableList<InputEventAPI>) {
        if (Global.getCurrentState() != GameState.COMBAT) {
            processInput(events)
        }
    }

    override fun processInputPreCoreControls(amount: Float, events: List<InputEventAPI>) {
        if (Global.getCurrentState() != GameState.CAMPAIGN || Global.getSector().campaignUI.currentCoreTab != CoreUITabId.REFIT) {
            processInput(events)
        }
    }

    private fun processInput(events: List<InputEventAPI>) {
        val inputImplWrapper = InputImplWrapper.getOrAttach()

        var x_coord = Mouse.getX()
        var deltaX = 0
        var y_coord = Mouse.getY()
        var deltaY = 0
        for (event in events) {
            if (event.isMouseEvent || event.isConsumed) {
                continue
            }

            var fakeEvent: MouseEvent
            when {
                event.isKeyDownEvent || event.isRepeat -> {
                    fakeEvent = Move(x_coord, y_coord)
                    when (event.eventValue) {
                        KEY_LEFT -> leftActive = true

                        KEY_RIGHT -> rightActive = true

                        KEY_UP -> upActive = true

                        KEY_DOWN -> downActive = true

                        KEY_CLICK, KEY_CLICK_ALT -> {
                            inputImplWrapper.addEvent(LeftClick.Down(x_coord, y_coord))
                            if (event.isRepeat) {
                                inputImplWrapper.removeFailsafeEvent()
                            } else {
                                inputImplWrapper.addFailsafeEvent(LeftClick.Up(x_coord, y_coord))
                            }
                            event.consume()
                            continue
                        }

                        else -> {
                            leftActive = false
                            rightActive = false
                            upActive = false
                            downActive = false
                            continue
                        }
                    }
                    if (event.isRepeat) {
                        mult *= Keys.MULT_MULT
                        if (mult > Keys.MAX_MULT) {
                            mult = Keys.MAX_MULT
                        }
                    } else {
                        mult = 1.0
                    }
                    deltaX += leftActive.toInt() * -Keys.STEP_DIST
                    deltaX += rightActive.toInt() * Keys.STEP_DIST
                    deltaY += upActive.toInt() * Keys.STEP_DIST
                    deltaY += downActive.toInt() * -Keys.STEP_DIST
                    deltaX = (deltaX * mult).roundToInt()
                    deltaY = (deltaY * mult).roundToInt()
                    x_coord += deltaX
                    y_coord += deltaY
                    fakeEvent.setX(x_coord)
                    fakeEvent.setY(y_coord)
                    inputImplWrapper.addEvent(fakeEvent)
                }

                event.isKeyUpEvent -> {
                    when (event.eventValue) {
                        KEY_CLICK, KEY_CLICK_ALT -> {
                            inputImplWrapper.addEvent(LeftClick.Up(x_coord, y_coord))
                        }

                        KEY_DISABLE, KEY_DISABLE_ALT -> {
                            val timestamp = System.currentTimeMillis()
                            this.lastTap = if (lastTap?.let { timestamp - it < 5000 } ?: false) {
                                inputImplWrapper.disabled = !inputImplWrapper.disabled
                                null
                            } else {
                                timestamp
                            }
                        }

                        KEY_LEFT -> leftActive = false

                        KEY_RIGHT -> rightActive = false

                        KEY_UP -> upActive = false

                        KEY_DOWN -> downActive = false
                    }
                }
            }
        }
    }

    override fun processCampaignInputPreFleetControl(events: MutableList<InputEventAPI>?) = Unit

    override fun processCampaignInputPostCore(events: MutableList<InputEventAPI>?) = Unit
}
