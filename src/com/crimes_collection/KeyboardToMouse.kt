package com.crimes_collection

import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.input.InputEventAPI
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class KeyboardToMouse : CampaignInputListener {
    private var lastEvent: MouseEvent? = null

    override fun getListenerInputPriority() = Int.MAX_VALUE

    override fun processCampaignInputPreCore(events: MutableList<InputEventAPI>) {
        val inputImplWrapper = InputImplWrapper.getOrAttach()

        var x_coord = Mouse.getX()
        var y_coord = Mouse.getY()
        for (event in events) {
            if (event.isMouseEvent) {
                continue
            }

            var fakeEvent: MouseEvent? = null

            when {
                event.isRepeat -> {
                    val lastEvent = lastEvent
//                    if (lastEvent?.type is Move) {
//                        fakeEvent = Move(x_coord + (lastEvent.deltaX ?: 0), y_coord + (lastEvent.deltaY ?: 0))
//                        event.consume()
//                        inputImplWrapper.addEvent(fakeEvent)
//                    }
                }

                event.isKeyDownEvent -> {
                    fakeEvent = Move(x_coord, y_coord)
                    when (event.eventValue) {
                        Keyboard.KEY_LEFT -> {
                            fakeEvent.deltaX(-5)
                        }

                        Keyboard.KEY_RIGHT -> {
                            fakeEvent.deltaX(5)
                        }

                        Keyboard.KEY_UP -> {
                            fakeEvent.deltaY(5)
                        }

                        Keyboard.KEY_DOWN -> {
                            fakeEvent.deltaY(-5)
                        }

                        Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL -> {
                            fakeEvent = LeftClick.Down(x_coord, y_coord)
                        }

                        else -> continue
                    }
                    event.consume()
                    inputImplWrapper.setCursorPosition(x_coord, y_coord)
                    inputImplWrapper.addEvent(fakeEvent)
                }

                event.isKeyUpEvent && event.eventValue == Keyboard.KEY_LCONTROL -> {
                    when (event.eventValue) {
                        Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL -> {
                            event.consume()
                            fakeEvent = LeftClick.Up(x_coord, y_coord)
                            inputImplWrapper.addEvent(fakeEvent)
                        }
                    }
                }
            }

            lastEvent = fakeEvent
        }

        if (events.isEmpty()) {
            val lastEvent = lastEvent
            val type = lastEvent?.type as? MouseEvent.EventType.Click
            if (type?.state == true) {
                val fakeEvent = LeftClick.Up(x_coord, y_coord)
                inputImplWrapper.addEvent(fakeEvent)
                this.lastEvent = fakeEvent
            }
        }
    }

    override fun processCampaignInputPreFleetControl(events: MutableList<InputEventAPI>?) = Unit

    override fun processCampaignInputPostCore(events: MutableList<InputEventAPI>?) = Unit
}
