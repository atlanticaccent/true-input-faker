package com.crimes_collection

import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.input.InputEventAPI
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class KeyboardToMouse : CampaignInputListener {
    private var clickHold = false

    override fun getListenerInputPriority() = Int.MAX_VALUE

    override fun processCampaignInputPreCore(events: MutableList<InputEventAPI>) {
        val inputImplWrapper = InputImplWrapper.getOrAttach()

        var x_coord = Mouse.getX()
        var deltaX: Int? = null
        var y_coord = Mouse.getY()
        var deltaY: Int? = null
        for (event in events) {
            var fakeEvent: MouseEvent
            if ((event.eventValue != Keyboard.KEY_LCONTROL && event.eventValue != Keyboard.KEY_RCONTROL) && clickHold) {
                clickHold = false
                inputImplWrapper.addEvent(LeftClick.Up(x_coord, y_coord))
            }
            when {
                event.isKeyDownEvent || event.isRepeat -> {
                    fakeEvent = Move(x_coord, y_coord)
                    when (event.eventValue) {
                        Keyboard.KEY_LEFT -> {
                            deltaX = -5
                        }

                        Keyboard.KEY_RIGHT -> {
                            deltaX = 5
                        }

                        Keyboard.KEY_UP -> {
                            deltaY = 5
                        }

                        Keyboard.KEY_DOWN -> {
                            deltaY = -5
                        }

                        Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL -> {
                            if (event.isRepeat) {
                                clickHold = true
                                continue
                            } else {
                                inputImplWrapper.addEvent(LeftClick.Down(x_coord, y_coord))
                                fakeEvent = LeftClick.Up(x_coord, y_coord)
                            }
                            event.consume()
                        }

                        else -> continue
                    }
                    x_coord += deltaX ?: 0
                    y_coord += deltaY ?: 0
                    fakeEvent.deltaX(deltaX)
                    fakeEvent.deltaY(deltaY)
                    inputImplWrapper.setCursorPosition(x_coord, y_coord)
                    inputImplWrapper.addEvent(fakeEvent)
                }
            }
            if (events.isEmpty() && clickHold) {
                clickHold = false
                inputImplWrapper.addEvent(LeftClick.Up(x_coord, y_coord))
                event.consume()
            }
        }
    }

    override fun processCampaignInputPreFleetControl(events: MutableList<InputEventAPI>?) = Unit

    override fun processCampaignInputPostCore(events: MutableList<InputEventAPI>?) = Unit
}
