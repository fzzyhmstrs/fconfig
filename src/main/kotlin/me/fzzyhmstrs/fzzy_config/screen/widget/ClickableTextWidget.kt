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
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.ActiveTextCollector.ClickableStyleFinder
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/**
 * A [CustomTextWidget] that can process click and hover actions on rendered text
 * @param parent Screen - this widgets parent screen
 * @param message Text - the text to render with this widget
 * @param textRenderer [TextRenderer] - text renderer instance
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ClickableTextWidget(private val parent: Screen, message: Component, textRenderer: Font): CustomTextWidget(0, 0, textRenderer.width(message.visualOrderText), textRenderer.lineHeight, message, textRenderer) {

    override fun onPress(event: CustomWidget.MouseEvent): Boolean {
        val clickHandler = ClickableStyleFinder(this.font, event.x().toInt(), event.y().toInt())
        this.visitLines(clickHandler)
        val style = clickHandler.result()
        if (style != null && style.clickEvent != null) {
            val ce = style.clickEvent ?: return false
            return ConfigAction.handleClickEvent(ce)
        }
        return false
    }
}