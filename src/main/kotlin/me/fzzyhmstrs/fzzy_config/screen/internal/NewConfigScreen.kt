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
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ChangesWidget
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
import java.util.function.Function

//client
internal class NewConfigScreen(
    title: Text,
    private val scope: String,
    private val manager: UpdateManager,
    entriesWidget: Function<NewConfigScreen, DynamicListWidget>,
    private val parentScopesButtons: List<Function<NewConfigScreen, ClickableWidget>>)
    :
    PopupWidgetScreen(title), ContextHandler
{

    private var parent: Screen? = null

    internal val layout = ThreePartsLayoutWidget(this)
    private var searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, 110, 20, FcText.EMPTY)
    private var doneButton = DoneButtonWidget { _ -> if (hasShiftDown()) shiftClose() else close() }
    private val configList: DynamicListWidget = entriesWidget.apply(this)

    private var mX: Double = 0.0
    private var mY: Double = 0.0

    fun setParent(screen: Screen?) {
        this.parent = screen
        if (screen !is NewConfigScreen) {
            doneButton.tooltip = Tooltip.of("fc.config.done.desc".translate())
            return
        }
        doneButton.message = "fc.config.back".translate()
        doneButton.tooltip = Tooltip.of("fc.config.back.desc".translate(screen.title))
    }

    override fun close() {
        if(this.parent == null || this.parent !is NewConfigScreen) {
            manager.apply(true)
            this.client?.narratorManager?.clear()
        }
        this.client?.setScreen(parent)
    }

    private fun shiftClose() {
        val p = this.parent
        if(p is NewConfigScreen) {
            var parentParent = p.parent
            while (parentParent is NewConfigScreen) {
                parentParent = parentParent.parent
            }
            this.parent = parentParent
        }
        close()
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        super.resize(client, width, height)
    }

    override fun init() {
        super.init()
        initHeader()
        initFooter()
        initBody()
        initLayout()
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
        configList.scrollToTop()
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
        searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, 110, 20, FcText.EMPTY)
        searchField.setMaxLength(100)
        searchField.text = ""
        searchField.setChangedListener { s -> setColor(configList.search(s)) }
        searchField.tooltip = Tooltip.of("fc.config.search.desc".translate())
        directionalLayoutWidget.add(searchField)
        //forward alert button
        directionalLayoutWidget.add(TextlessActionWidget("widget/action/alert".fcId(), "widget/action/alert_inactive".fcId(), "widget/action/alert_highlighted".fcId(), "fc.button.alert.active".translate(), "fc.button.alert.inactive".translate(), { manager.hasForwards() } ) { manager.forwardsHandler() })
        //changes button
        directionalLayoutWidget.add(ChangesWidget(scope, { this.width }, manager))
        //done button
        val msg = doneButton.message
        val tt = doneButton.tooltip
        doneButton = DoneButtonWidget { _ -> if (hasShiftDown()) shiftClose() else close() }
        doneButton.message = msg
        doneButton.tooltip = tt
        directionalLayoutWidget.add(doneButton)
    }

    private fun initLayout() {
        layout.refreshPositions()
        configList.setDimensionsAndPosition(320, layout.contentHeight, (this.width / 2) - 160, layout.headerHeight)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        this.mX = mouseX
        this.mY = mouseY
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val contextType = ContextHandler.getRelevantContext(button, ContextInput.MOUSE, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextType != null) {
            return handleContext(contextType, Position(ContextInput.MOUSE, mouseX.toInt(), mouseY.toInt(), 0, 0, this.width, this.height, this.width, this.height))
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val contextType = ContextHandler.getRelevantContext(keyCode, ContextInput.KEYBOARD, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextType != null) {
            val input = if(MinecraftClient.getInstance().navigationType.isKeyboard) ContextInput.KEYBOARD else ContextInput.MOUSE
            return handleContext(contextType, Position(input, mX.toInt(), mY.toInt(), 0, 0, this.width, this.height, this.width, this.height))
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun handleContext(contextType: ContextHandler.ContextType, position: Position): Boolean {
        return when (contextType) {
            ContextHandler.CONTEXT_KEYBOARD, ContextHandler.CONTEXT_MOUSE -> {
                val contextResult = hoveredElement?.nullCast<ContextProvider>()?.provideContext(position) ?: ContextProvider.empty(position)
                if (contextResult.appliers.isNotEmpty()) {
                    openContextMenuPopup(contextResult.appliers, contextResult.position)
                    true
                } else {
                    false
                }
            }
            ContextHandler.UNDO -> {
                manager.revertLast()
                true
            }
            ContextHandler.FIND -> {
                this.focused = searchField
                true
            }
            ContextHandler.SAVE -> {
                manager.apply(false)
                true
            }
            else -> {
                configList.handleContext(contextType, position)
            }
        }
    }

    private fun openContextMenuPopup(actions: List<ContextApplier>, positionContext: Position) {
        val popup = PopupWidget.Builder("fc.config.right_click".translate(), 2, 2)
            .addDivider()
            .positionX(PopupWidget.Builder.abs(if (positionContext.contextInput == ContextInput.KEYBOARD) positionContext.x else positionContext.mX))
            .positionY(PopupWidget.Builder.abs(if (positionContext.contextInput == ContextInput.KEYBOARD) positionContext.y else positionContext.mY))
            .background("widget/popup/background_right_click".fcId())
            .noBlur()
        for ((index, action) in actions.withIndex()) {
            popup.add(
                "$index",
                ContextActionWidget(action, ContextActionWidget.getNeededWidth(action)),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_LEFT
            )
        }
        PopupWidget.push(popup.build())
    }

    private fun openInfoPopup() {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val popup = PopupWidget.Builder("fc.button.info".translate())
            .addDivider()
            .add("header", ClickableTextWidget(this, "fc.button.info.fc".translate("Fzzy Config".lit().styled { style ->
                style.withFormatting(Formatting.AQUA, Formatting.UNDERLINE)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://fzzyhmstrs.github.io/fconfig/"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, "fc.button.info.fc.tip".translate()))
            }), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            .addDivider()
            .add("undo", TextWidget("fc.button.info.undo".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("find", TextWidget("fc.button.info.find".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("copy", TextWidget("fc.button.info.copy".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("paste", TextWidget("fc.button.info.paste".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("save", TextWidget("fc.button.info.save".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("page", TextWidget("fc.button.info.page".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .addDivider()
            .add("click", TextWidget("fc.button.info.click".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("click_kb", TextWidget("fc.button.info.click_kb".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("click_kb2", TextWidget("fc.button.info.click_kb2".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .addDivider()
            .add("alert", TextWidget("fc.button.info.alert".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .addDoneWidget()
            .build()
        PopupWidget.push(popup)
    }
}