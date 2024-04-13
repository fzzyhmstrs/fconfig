package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.screen.LastSelectable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement

object PopupWidgetExamples {

    fun lastSelectable(){

        //simple stub example of a LastSelectable, passing its focused element back and forth with its lastSelected cache
        abstract class ExampleLastSelectable: ParentElement, LastSelectable{
            override var lastSelected: Element? = null
            override fun pushLast() {
                lastSelected = focused
            }
            override fun popLast() {
                focused = lastSelected
            }
        }


    }



}