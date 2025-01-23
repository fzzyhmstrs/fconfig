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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
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
 * @param renderBackground Default false. If true, will render a standard MC button background behind your icon.
 * @author fzzyhmstrs
 * @since 0.2.0, added optional bg rendering 0.6.0, deprecated 0.6.3 for removal by 0.7.0
 */
//client
@Deprecated("Unused internally. Switch to CustomButtonWidget#builder. Scheduled for removal 0.7.0")
open class TextlessActionWidget(
    private val activeIcon: Identifier,
    private val inactiveIcon: Identifier,
    private val highlightedIcon: Identifier,
    private val activeNarration: Text,
    private val inactiveNarration: Text,
    private val activeSupplier: Supplier<Boolean>,
    private val pressAction: Consumer<TextlessActionWidget>,
    private val renderBackground: Boolean)
    :
    CustomPressableWidget(0, 0, 20, 20, FcText.EMPTY)
{

    constructor(
        activeIcon: Identifier,
        inactiveIcon: Identifier,
        highlightedIcon: Identifier,
        activeNarration: Text,
        inactiveNarration: Text,
        activeSupplier: Supplier<Boolean>,
        pressAction: Consumer<TextlessActionWidget>
    ): this(
        activeIcon, inactiveIcon, highlightedIcon,
        activeNarration, inactiveNarration,
        activeSupplier,
        pressAction,
        false)

    constructor(
        icon: Identifier,
        activeNarration: Text,
        inactiveNarration: Text,
        activeSupplier: Supplier<Boolean>,
        pressAction: Consumer<TextlessActionWidget>
    ): this(
        icon, icon, icon,
        activeNarration, inactiveNarration,
        activeSupplier,
        pressAction,
        true)

    init {
        FC.LOGGER.error("This class is marked for removal. Please reimplement as a CustomButtonWidget or use CustomButtonWidget#builder")
    }

    private fun getTex(): Identifier {
        if(!active)
            return inactiveIcon
        return if (hovered || isFocused)
            highlightedIcon
        else
            activeIcon
    }

    override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = activeSupplier.get()
        super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
        if (renderBackground) {
            context.drawTex(DEFAULT_TEXTURES.get(active, hovered || isFocused), x, y, getWidth(), getHeight())
        }
        context.drawTex(getTex(), x, y, getWidth(), getHeight())
        if (this.active && activeNarration.string != "") {
            tooltip = Tooltip.of(activeNarration)
        } else if (inactiveNarration.string != "") {
            tooltip = Tooltip.of(inactiveNarration)
        }
    }

    override fun getNarrationMessage(): MutableText {
        return if(active) activeNarration.copy() else inactiveNarration.copy()
    }

    override fun onPress() {
       pressAction.accept(this)
    }
}