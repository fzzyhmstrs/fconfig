package me.fzzyhmstrs.fzzy_config.screen

import net.minecraft.client.gui.Element

interface LastSelectable {
    var lastSelected: Element?
    fun pushLast()
    fun popLast()
}