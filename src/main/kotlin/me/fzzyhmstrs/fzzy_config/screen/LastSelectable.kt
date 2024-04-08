package me.fzzyhmstrs.fzzy_config.screen

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Element

@Environment(EnvType.CLIENT)
interface LastSelectable {
    var lastSelected: Element?
    fun pushLast()
    fun popLast()
}