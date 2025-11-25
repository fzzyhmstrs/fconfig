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

import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.font.Alignment
import net.minecraft.client.font.DrawnTextConsumer
import net.minecraft.client.font.DrawnTextConsumer.ClickHandler
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.DrawContext.HoverType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import kotlin.math.roundToInt

/**
 * A [CustomTextWidget] that can process click and hover actions on rendered text
 * @param parent Screen - this widgets parent screen
 * @param message Text - the text to render with this widget
 * @param textRenderer [TextRenderer] - text renderer instance
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ClickableTextWidget(private val parent: Screen, message: Text, textRenderer: TextRenderer): CustomTextWidget(0, 0, textRenderer.getWidth(message.asOrderedText()), textRenderer.fontHeight, message, textRenderer) {

    override fun onPress(event: CustomWidget.MouseEvent): Boolean {
        val clickHandler = ClickHandler(this.textRenderer, event.x().toInt(), event.y().toInt())
        this.draw(clickHandler)
        val style = clickHandler.style
        if (style != null && style.clickEvent != null) {
            val ce = style.clickEvent ?: return false
            return ConfigAction.handleClickEvent(ce)
        }
        return false
    }
}