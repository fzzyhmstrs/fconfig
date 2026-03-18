package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.navigation.ScreenDirection
import java.util.*

//client
@JvmDefaultWithoutCompatibility
interface Neighbor: GuiEventListener {
    val neighbor: EnumMap<ScreenDirection, Neighbor>

    fun setNeighbor(direction: ScreenDirection, neighbor: Neighbor?) {
        if (neighbor == null) {
            this.neighbor.remove(direction)
            return
        }
        this.neighbor[direction] = neighbor
    }

    fun getNeighbor(direction: ScreenDirection): Neighbor? {
        return neighbor[direction]
    }
}