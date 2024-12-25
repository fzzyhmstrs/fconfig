/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.internal

import me.fzzyhmstrs.fzzy_config.config.ConfigSpec
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.context.ContextHandler
import me.fzzyhmstrs.fzzy_config.screen.widget.ClickableTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessActionWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DoneButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.DirectionalLayoutWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import org.lwjgl.glfw.GLFW
import java.util.function.Function

//client
internal class TestConfigScreen(
    title: Text,
    private val manager: UpdateManager,
    private val list: DynamicListWidget) : PopupWidgetScreen(title) {

    private val listWidth = 290

    override fun close() {
        manager.apply(true)
        this.client?.setScreen(null)
    }

    override fun init() {
        super.init()
        list.setDimensions(listWidth, this.height)
        list.setPosition( (this.width / 2) - (listWidth / 2), 0)
        list.onReposition()
        addDrawableChild(list)
    }

    override fun setInitialFocus() {
        this.focused = list
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (list.keyPressed(keyCode, scanCode, modifiers)) return true
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}