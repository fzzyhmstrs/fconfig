package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import net.minecraft.client.gui.Element
import net.minecraft.client.gui.navigation.NavigationDirection
import java.util.*

//client
@JvmDefaultWithCompatibility
interface Neighbor: Element {
    val neighbor: EnumMap<NavigationDirection, Neighbor>

    fun setNeighbor(direction: NavigationDirection, neighbor: Neighbor?) {
        if (neighbor == null) {
            this.neighbor.remove(direction)
            return
        }
        this.neighbor[direction] = neighbor
    }

    fun getNeighbor(direction: NavigationDirection): Neighbor? {
        return neighbor[direction]
    }
}