/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.screen.LastSelectable
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

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


    fun popupExample(){
        // builds and pushes an example popup widget to a PopupParentElement
        // start with the builder. The title will be displayed at the top of the popup like a windows window title
        // dy default the horizontal and vertical padding will be 4px; it can be changed here too
        val popup = PopupWidget.Builder(Text.translatable("my.cool.popup"))
            //you can add horizontal dividers between content. it automatically justifies to the popup width
            .addDivider()
            //adds a basic element. give it a unique name, and position it
            //this element is positioned below the last added element (the divider in this case), and aligned centered in the popup
            .addElement("text_element",TextWidget(Text.translatable("my.cool.popup.text"),MinecraftClient.getInstance().textRenderer).alignCenter(),
                PopupWidget.Builder.Position.BELOW, PopupWidget.Builder.Position.ALIGN_CENTER)
            //adds a button, this element is below the "text_element" one, and aligned left in the popup bounds
            .addElement("button_1",ButtonWidget.builder(Text.translatable("my.cool.popup.button1")) { b-> }.size(50,44).build(),
                PopupWidget.Builder.Position.BELOW, PopupWidget.Builder.Position.ALIGN_LEFT )
            //a second button, this one is aligned horizontal to the top edge of the "button_1" element,
            //aligns to the right of the widget window and is stretched to meet the closest element to its left ("button_1" in this case)
            .addElement("button_2", ButtonWidget.builder(Text.translatable("my.cool.popup.button2")) { b-> }.size(50,20).build(),
                PopupWidget.Builder.Position.ALIGN_RIGHT_AND_JUSTIFY, PopupWidget.Builder.Position.HORIZONTAL_TO_TOP_EDGE)
            //repeat that with button 3, this time aligned to "button_1" bottom edge
            //this element has a defined parent (button_1), instead of automatically picking the last element
            .addElement("button_3", ButtonWidget.builder(Text.translatable("my.cool.popup.button3")) { b-> }.size(50,20).build(), "button_1",
                PopupWidget.Builder.Position.ALIGN_RIGHT_AND_JUSTIFY, PopupWidget.Builder.Position.HORIZONTAL_TO_BOTTOM_EDGE)
            //there is a special method for adding a "Done" button, this also takes "button_1" as its parent in this case
            //it will automatically align below the parent element and justify across the entire width of the widget
            .addDoneButton(parent = "button_1")
            //popups can be positioned relative to various contexts.
            //in this case we are using the screen as context, positioning it 100px from the bottom right edge of the screen
            //the context method will bound the popup within the screen if the widget is bigger than 100x100
            .positionX(PopupWidget.Builder.screenContext{ w -> w - 100 })
            .positionY(PopupWidget.Builder.screenContext{ h -> h - 100 })
            //tells the popup to not render a blur behind it
            .noBlur()
            //clicking outside of this widget won't close it automatically (which is default behavior)
            .noCloseOnClick()
            //provide a custom background like so.
            //make sure the texture is a nine-patch texture, and the popup will expect 8 pixels of border and padding before content
            .background(Identifier.of("my_mod","my_custom_background"))
            //create the popup!
            .build()

        //Last step: push the popup to screen, the PopupWidget API will push the popup onto an open PopupParentElement screen, if any
        PopupWidget.push(popup)
    }


}