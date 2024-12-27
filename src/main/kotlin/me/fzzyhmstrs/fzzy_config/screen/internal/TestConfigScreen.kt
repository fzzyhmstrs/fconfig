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
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
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
    private val list: DynamicListWidget) : PopupWidgetScreen(title), ContextHandler
{

    private val listWidth = 290

    private var mX: Double = 0.0
    private var mY: Double = 0.0

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

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        this.mX = mouseX
        this.mY = mouseY
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val contextType = ContextHandler.getRelevantContext(button, ContextInput.MOUSE, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextType != null) {
            return handleContext(contextType,
                Position(
                    ContextInput.MOUSE,
                    mouseX.toInt(),
                    mouseY.toInt(),
                    0,
                    0,
                    this.width,
                    this.height,
                    this.width,
                    this.height
                )
            )
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val contextType = ContextHandler.getRelevantContext(keyCode, ContextInput.KEYBOARD, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextType != null) {
            val input = if(MinecraftClient.getInstance().navigationType.isKeyboard) ContextInput.KEYBOARD else ContextInput.MOUSE
            return handleContext(contextType,
                Position(
                    input,
                    mX.toInt(),
                    mY.toInt(),
                    0,
                    0,
                    this.width,
                    this.height,
                    this.width,
                    this.height
                )
            )
        }

        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun handleContext(contextType: ContextHandler.ContextType, position: me.fzzyhmstrs.fzzy_config.screen.context.Position): Boolean {
        return when (contextType) {
            ContextHandler.CONTEXT_KEYBOARD, ContextHandler.CONTEXT_MOUSE -> {
                val actions = hoveredElement?.nullCast<ContextProvider>()?.contextActions(position) ?: emptyList()
                if (actions.isNotEmpty()) {
                    openContextMenuPopup(actions, position)
                    true
                } else {
                    false
                }
            }
            else -> {
                list.handleContext(contextType, position)
            }
        }
    }

    private fun openContextMenuPopup(actions: List<ContextApplier>, positionContext: me.fzzyhmstrs.fzzy_config.screen.context.Position) {
        val popup = PopupWidget.Builder("fc.config.right_click".translate(), 2, 2)
            .addDivider()
            .positionX(PopupWidget.Builder.abs(positionContext.x))
            .positionY(PopupWidget.Builder.abs(positionContext.y))
            .background("widget/popup/background_right_click".fcId())
            .noBlur()
        for ((index, action) in actions.withIndex()) {
            popup.add(
                "$index",
                ContextActionWidget(action, ContextActionWidget.getNeededWidth(action)),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_LEFT)
        }
    }
}