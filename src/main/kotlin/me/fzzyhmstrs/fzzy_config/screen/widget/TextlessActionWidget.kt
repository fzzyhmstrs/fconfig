/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawGuiTexture
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A button widget that is 20x20 pixels and displays a sprite instead of a textual message
 * @param activeIcon Identifier - sprite id for active-but-not-hovered state
 * @param inactiveIcon Identifier - sprite id for inactive/disabled state, controlled via [activeSupplier]
 * @param highlightedIcon Identifier - sprite id for the active-and-hovered state
 * @param activeNarration Text - the tooltip and narration for active states
 * @param inactiveNarration Text - narration and tooltip to display if the button is inactive
 * @param activeSupplier [Supplier]&lt;Boolean&gt; - supplies whether this widget should be active
 * @param pressAction [Consumer]&lt;TextlessActionWidget&gt; - action to take on press
 * @author fzzyhmstrs
 * @since 0.2.0
 */
//client
class TextlessActionWidget(
    private val activeIcon: Identifier,
    private val inactiveIcon: Identifier,
    private val highlightedIcon: Identifier,
    private val activeNarration: Text,
    private val inactiveNarration: Text,
    private val activeSupplier: Supplier<Boolean>,
    private val pressAction: Consumer<TextlessActionWidget>)
    :
    PressableWidget(0, 0, 20, 20, FcText.empty())
{

    private fun getTex(): Identifier {
        if(!active)
            return inactiveIcon
        return if (hovered || isFocused)
            highlightedIcon
        else
            activeIcon
    }

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = activeSupplier.get()
        super.renderButton(context, mouseX, mouseY, delta)
        context.drawGuiTexture(getTex(), x, y, getWidth(), getHeight())
        if (this.active && activeNarration.string != "") {
            tooltip = Tooltip.of(activeNarration)
        } else if (inactiveNarration.string != "") {
            tooltip = Tooltip.of(inactiveNarration)
        }
    }

    override fun getNarrationMessage(): MutableText {
        return if(active) activeNarration.copy() else inactiveNarration.copy()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun onPress() {
       pressAction.accept(this)
    }


}