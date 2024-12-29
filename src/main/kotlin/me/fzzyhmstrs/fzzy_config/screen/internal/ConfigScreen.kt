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
import me.fzzyhmstrs.fzzy_config.screen.PopupParentElement
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DoneButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.util.function.Function
import java.util.function.Supplier

//client
internal class ConfigScreen(
    title: Text,
    val scope: String,
    private val manager: UpdateManager,
    private val configList: DynamicListWidget,
    private val parentScopesButtons: List<Supplier<ClickableWidget>>,
    private val anchors: List<Function<DynamicListWidget, out DynamicListWidget.Entry>>,
    private val anchorWidth: Int)
    :
    PopupWidgetScreen(title), ContextHandler, ContextProvider
{

    private var parent: Screen? = null

    internal val layout = ThreePartsLayoutWidget(this)
    private var searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, 110, 20, FcText.EMPTY)
    private var doneButton = DoneButtonWidget { _ -> if (hasShiftDown()) shiftClose() else close() }

    private var mX: Double = 0.0
    private var mY: Double = 0.0

    private val MENU_LIST_BACKGROUND_TEXTURE: Identifier = "textures/gui/menu_list_background.png".simpleId()
    private val INWORLD_MENU_LIST_BACKGROUND_TEXTURE: Identifier = "textures/gui/inworld_menu_list_background.png".simpleId()

    fun setParent(screen: Screen?): ConfigScreen {
        this.parent = screen
        if (screen !is ConfigScreen) {
            doneButton.tooltip = Tooltip.of("fc.config.done.desc".translate())
            return this
        }
        doneButton.message = "fc.config.back".translate()
        doneButton.tooltip = Tooltip.of("fc.config.back.desc".translate(screen.title))
        return this
    }

    fun scrollToGroup(g: String) {
        configList.scrollToGroup(g)
    }

    override fun close() {
        if(this.parent == null || this.parent !is ConfigScreen) {
            manager.apply(true)
            this.client?.narratorManager?.clear()
        }
        this.client?.setScreen(parent)
    }

    private fun shiftClose() {
        val p = this.parent
        if(p is ConfigScreen) {
            var parentParent = p.parent
            while (parentParent is ConfigScreen) {
                parentParent = parentParent.parent
            }
            this.parent = parentParent
        }
        close()
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
            directionalLayoutWidget.add(scopeButton.get())
            directionalLayoutWidget.add(TextWidget(textRenderer.getWidth(" > ".lit()), 20, " > ".lit(), this.textRenderer))
        }
        directionalLayoutWidget.add(TextWidget(textRenderer.getWidth(this.title), 20, this.title, this.textRenderer))

    }
    private fun initBody() {
        this.addDrawableChild(configList)
        layout.forEachChild { drawableElement: ClickableWidget? ->
            addDrawableChild(drawableElement)
        }
        configList.onReposition()
    }
    private fun initFooter() {
        val directionalLayoutWidget = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8))
        //goto button
        directionalLayoutWidget.add(TextlessActionWidget("widget/action/goto".fcId(), "widget/action/goto_inactive".fcId(), "widget/action/goto_highlighted".fcId(), "fc.button.goto".translate(), "fc.button.goto".translate(), { anchors.size > 1 } ) { Popups.openGotoPopup(anchors, anchorWidth, this.height) }) { p -> p.alignLeft() }
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

    override fun renderContents(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        drawMenuListBackground(context)
        super.renderContents(context, mouseX, mouseY, delta)
        drawHeaderAndFooterSeparators(context)
    }

    private fun drawHeaderAndFooterSeparators(context: DrawContext) {
        val identifier = if (client?.world == null) HEADER_SEPARATOR_TEXTURE else INWORLD_HEADER_SEPARATOR_TEXTURE
        val identifier2 = if (client?.world == null) FOOTER_SEPARATOR_TEXTURE else INWORLD_FOOTER_SEPARATOR_TEXTURE
        context.drawTex(identifier, 0, layout.headerHeight - 2, 0.0f, 0.0f, this.width, 2, 32, 2)
        context.drawTex(identifier2, 0, layout.headerHeight + layout.contentHeight, 0.0f, 0.0f, this.width, 2, 32, 2)
    }

    private fun drawMenuListBackground(context: DrawContext) {
        val identifier = if (client?.world == null) MENU_LIST_BACKGROUND_TEXTURE else INWORLD_MENU_LIST_BACKGROUND_TEXTURE
        context.drawTex(identifier, 0, layout.headerHeight, 0f, 0f, this.width, layout.contentHeight, 32, 32)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        this.mX = mouseX
        this.mY = mouseY
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val popup = activeWidget()
        if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }
        return configList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val contextType = ContextType.getRelevantContext(button, ContextInput.MOUSE, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextType != null) {
            return if(handleContext(contextType, Position(ContextInput.MOUSE, mouseX.toInt(), mouseY.toInt(), 0, 0, this.width, this.height, this.width, this.height)))
                true
            else
                super.onClick(mouseX, mouseY, button)
        }
        return super.onClick(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val contextType = ContextType.getRelevantContext(keyCode, ContextInput.KEYBOARD, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextType != null) {
            val input = if(MinecraftClient.getInstance().navigationType.isKeyboard) ContextInput.KEYBOARD else ContextInput.MOUSE
            return if(handleContext(contextType, Position(input, mX.toInt(), mY.toInt(), 0, 0, this.width, this.height, this.width, this.height))) {
                true
            } else {
                val bl = super.keyPressed(keyCode, scanCode, modifiers)
                if (!bl && contextType == ContextType.BACK && parent is ConfigScreen) {
                    this.close()
                    true
                } else {
                    bl
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun handleContext(contextType: ContextType, position: Position): Boolean {
        return when (contextType) {
            ContextType.CONTEXT_KEYBOARD, ContextType.CONTEXT_MOUSE -> {
                val builder = ContextProvider.empty(position)
                this.provideContext(builder)
                if (builder.isNotEmpty()) {
                    openContextMenuPopup(builder)
                    true
                } else {
                    false
                }
            }
            ContextType.FIND -> {
                this.focused = searchField
                true
            }
            ContextType.SEARCH -> {
                if (anchors.size > 1) {
                    Popups.openGotoPopup(anchors, anchorWidth, this.height)
                    true
                } else {
                    false
                }
            }
            ContextType.INFO ->{
                openInfoPopup()
                true
            }
            ContextType.SAVE -> {
                manager.apply(false)
                true
            }
            ContextType.UNDO -> {
                manager.revertLast()
                true
            }
            else -> {
                configList.handleContext(contextType, position)
            }
        }
    }

    override fun provideContext(builder: ContextResultBuilder) {
        hoveredElement?.nullCast<ContextProvider>()?.provideContext(builder)
        val save = ContextAction.Builder("fc.button.save".translate()) { manager.apply(false); true }
            .active { manager.hasChanges() }
            .icon(TextureDeco.CONTEXT_SAVE)
        val find = ContextAction.Builder("fc.config.search".translate()) { this.focused = searchField; true }
            .icon(TextureDeco.CONTEXT_FIND)
        builder.add("config", ContextType.SAVE, save)
        builder.add("config", ContextType.FIND, find)
    }

    private fun openContextMenuPopup(builder: ContextResultBuilder) {
        val positionContext = builder.position()
        val popup = PopupWidget.Builder("fc.config.right_click".translate(), 2, 2)
            .positionX(PopupWidget.Builder.absScreen(
                if (positionContext.contextInput == ContextInput.KEYBOARD)
                    positionContext.x
                else
                    positionContext.mX))
            .positionY(PopupWidget.Builder.absScreen(
                if (positionContext.contextInput == ContextInput.KEYBOARD)
                    positionContext.y
                else
                    positionContext.mY))
            .background("widget/popup/background_right_click".fcId())
            .noBlur()
            .onClick { mX, mY, over, button ->
                if (ContextType.CONTEXT_MOUSE.relevant(button, ctrl = false, shift = false, alt = false) && !over) {
                    PopupWidget.pop(mX, mY)
                    PopupParentElement.ClickResult.PASS
                } else {
                    PopupParentElement.ClickResult.USE
                }
            }
        for ((group, actions) in builder.apply()) {
            if (actions.isEmpty()) continue
            popup.addDivider()
            for ((type, action) in actions) {
                popup.add(
                    "${group}_$type",
                    ContextActionWidget(action, positionContext, ContextActionWidget.getNeededWidth(action)),
                    LayoutWidget.Position.BELOW,
                    LayoutWidget.Position.ALIGN_LEFT
                )
            }
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