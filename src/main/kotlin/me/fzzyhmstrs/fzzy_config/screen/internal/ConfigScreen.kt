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

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DirectionalLayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import org.lwjgl.glfw.GLFW
import java.util.function.Function

//client
internal class ConfigScreen(title: Text, private val scope: String, private val manager: UpdateManager, entriesWidget: Function<ConfigScreen, ConfigListWidget>, private val parentScopesButtons: List<Function<ConfigScreen, ClickableWidget>>) : PopupWidgetScreen(title) {

    private var parent: Screen? = null

    internal val layout = ThreePartsLayoutWidget(this)
    private val searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, 110, 20, FcText.empty())
    private val doneButton = ButtonWidget.builder(ScreenTexts.DONE) { _ -> close() }.size(70, 20).build()
    private val configList: ConfigListWidget by lazy {
        entriesWidget.apply(this)
    }

    fun setParent(screen: Screen?) {
        this.parent = screen
        if (screen !is ConfigScreen) {
            doneButton.tooltip = Tooltip.of("fc.config.done.desc".translate())
            return
        }
        doneButton.message = "fc.config.back".translate()
        doneButton.tooltip = Tooltip.of("fc.config.back.desc".translate(screen.title))
    }

    override fun close() {
        if(this.parent == null || this.parent !is ConfigScreen) {
            manager.apply(true)
            this.client?.narratorManager?.clear()
        }
        this.client?.setScreen(parent)
    }

    override fun init() {
        super.init()
        initHeader()
        initFooter()
        initBody()
        initTabNavigation()
    }
    private fun initHeader() {
        val directionalLayoutWidget = layout.addHeader(DirectionalLayoutWidget.horizontal().spacing(2))
        for (scopeButton in parentScopesButtons) {
            directionalLayoutWidget.add(scopeButton.apply(this))
            directionalLayoutWidget.add(TextWidget(textRenderer.getWidth(" > ".lit()), 20, " > ".lit(), this.textRenderer))
        }
        directionalLayoutWidget.add(TextWidget(textRenderer.getWidth(this.title), 20, this.title, this.textRenderer))

    }
    private fun initBody() {
        this.addDrawableChild(configList)
        layout.forEachChild { drawableElement: ClickableWidget? ->
            addDrawableChild(drawableElement)
        }
        configList.scrollAmount = 0.0
    }
    private fun initFooter() {
        val directionalLayoutWidget = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8))
        //info button
        directionalLayoutWidget.add(TextlessActionWidget("widget/action/info".fcId(), "widget/action/info_inactive".fcId(), "widget/action/info_highlighted".fcId(), "fc.button.info".translate(), "fc.button.info".translate(), { true } ) { openInfoPopup() }) { p -> p.alignLeft() }
        //search bar
        fun setColor(entries: Int) {
            if(entries > 0)
                searchField.setEditableColor(Colors.WHITE)
            else
                searchField.setEditableColor(0xFF5555)
        }
        searchField.setMaxLength(100)
        searchField.text = ""
        searchField.setChangedListener { s -> setColor(configList.updateSearchedEntries(s)) }
        directionalLayoutWidget.add(searchField)
        //forward alert button
        directionalLayoutWidget.add(TextlessActionWidget("widget/action/alert".fcId(), "widget/action/alert_inactive".fcId(), "widget/action/alert_highlighted".fcId(), "fc.button.alert.active".translate(), "fc.button.alert.inactive".translate(), { manager.hasForwards() } ) { manager.forwardsHandler() })
        //changes button
        directionalLayoutWidget.add(ChangesWidget(scope, { this.width }, manager))
        //done button
        directionalLayoutWidget.add(doneButton)
    }

    override fun initTabNavigation() {
        layout.refreshPositions()
        configList.position(width, layout)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            configList.page(true)
            return true
        } else if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            configList.page(false)
            return true
        } else if (isCopy(keyCode)) {
            configList.copy()
            return true
        } else if (isPaste(keyCode)) {
            configList.paste()
            return true
        } else if (keyCode == GLFW.GLFW_KEY_Z && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            manager.revertLast()
            return true
        } else if (keyCode == GLFW.GLFW_KEY_F && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            this.focused = searchField
            return true
        } else if (keyCode == GLFW.GLFW_KEY_S && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            manager.apply(false)
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    private fun openInfoPopup() {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val popup = PopupWidget.Builder("fc.button.info".translate())
            .addDivider()
            .addElement("header", ClickableTextWidget(this, "fc.button.info.fc".translate("Fzzy Config".lit().styled { style ->
                style.withFormatting(Formatting.AQUA, Formatting.UNDERLINE)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://fzzyhmstrs.github.io/fconfig/"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, "fc.button.info.fc.tip".translate()))
            }), textRenderer), Position.BELOW, Position.ALIGN_CENTER)
            .addDivider()
            .addElement("undo", TextWidget("fc.button.info.undo".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("find", TextWidget("fc.button.info.find".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("copy", TextWidget("fc.button.info.copy".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("paste", TextWidget("fc.button.info.paste".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("save", TextWidget("fc.button.info.save".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("page", TextWidget("fc.button.info.page".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addDivider()
            .addElement("click", TextWidget("fc.button.info.click".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("click_kb", TextWidget("fc.button.info.click_kb".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("click_kb2", TextWidget("fc.button.info.click_kb2".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addDivider()
            .addElement("alert", TextWidget("fc.button.info.alert".translate(), textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addDoneButton()
            .build()
        PopupWidget.push(popup)
    }
}