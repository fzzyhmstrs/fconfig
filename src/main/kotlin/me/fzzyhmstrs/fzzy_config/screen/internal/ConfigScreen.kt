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
import me.fzzyhmstrs.fzzy_config.impl.config.KeybindsConfig
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.entry.InfoKeybindEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DirectionalLayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.TriState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.world.CreateWorldScreen.FOOTER_SEPARATOR_TEXTURE
import net.minecraft.client.gui.screen.world.CreateWorldScreen.HEADER_SEPARATOR_TEXTURE
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction
import java.util.function.Supplier

//client
internal class ConfigScreen(
    title: Text,
    val scope: String,
    private val manager: UpdateManager,
    private val configList: DynamicListWidget,
    private val parentScopesButtons: List<Supplier<ClickableWidget>>,
    private val sidebar: ConfigScreenManager.Sidebar,
    private val onFinalClose: Runnable)
    :
    PopupWidgetScreen(title), ContextHandler, ContextProvider
{

    private var parent: Screen? = null
    private var globalInputHandler: ((key: Int, released: Boolean, type: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean) -> TriState)? = null

    internal lateinit var layout: ThreePartsLayoutWidget
    private lateinit var searchField: NavigableTextFieldWidget
    private lateinit var doneButton: CustomButtonWidget

    private var mX: Double = 0.0
    private var mY: Double = 0.0

    private val narrator: ConfigScreenNarrator = ConfigScreenNarrator()
    private var elementNarrationStartTime = Long.MIN_VALUE
    private var screenNarrationStartTime = Long.MAX_VALUE

    private val menuListBackground: Identifier = "textures/gui/menu_list_background.png".simpleId()
    private val inWorldMenuListBackground: Identifier = "textures/gui/inworld_menu_list_background.png".simpleId()

    fun setGlobalInputHandler(handler: ((key: Int, released: Boolean, type: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean) -> TriState)?) {
        this.globalInputHandler = handler
    }

    fun setParent(screen: Screen?): ConfigScreen {
        this.parent = screen
        return this
    }

    fun scrollToGroup(g: String) {
        configList.scrollToGroup(g)
    }

    override fun close() {
        if(this.parent == null || this.parent !is ConfigScreen) {
            onFinalClose.run()
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
        layout = ThreePartsLayoutWidget(this)
        super.init()
        initHeader()
        initFooter()
        initBody()
        initLayout()
        initNarrator()
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
        directionalLayoutWidget.add(CustomButtonWidget.builder { Popups.openGotoPopup(this.sidebar.getAnchors(), this.sidebar.getAnchorWidth(), this.height) }.size(20, 20).textures(TextureIds.GOTO_SET).narrationSupplier { _, _ -> TextureIds.GOTO_LANG }.activeSupplier { sidebar.needsSidebar() }.tooltip(TextureIds.GOTO_LANG).build()) { p -> p.alignLeft() }
        //directionalLayoutWidget.add(TextlessActionWidget("widget/action/goto".fcId(), "widget/action/goto_inactive".fcId(), "widget/action/goto_highlighted".fcId(), "fc.button.goto".translate(), "fc.button.goto".translate(), { anchors.size > 1 } ) { Popups.openGotoPopup(anchors, anchorWidth, this.height) }) { p -> p.alignLeft() }
        //info button
        directionalLayoutWidget.add(CustomButtonWidget.builder { openInfoPopup() }.size(20, 20).textures(TextureIds.INFO_SET).narrationSupplier { _, _ -> TextureIds.INFO_LANG }.tooltip(TextureIds.INFO_LANG).build()) { p -> p.alignLeft() }
        //directionalLayoutWidget.add(TextlessActionWidget("widget/action/info".fcId(), "widget/action/info_inactive".fcId(), "widget/action/info_highlighted".fcId(), "fc.button.info".translate(), "fc.button.info".translate(), { true } ) { openInfoPopup() }) { p -> p.alignLeft() }
        //search bar
        searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, 110, 20, FcText.EMPTY)
        fun setColor(entries: Int) {
            if(entries > 0)
                searchField.setEditableColor(Colors.WHITE)
            else
                searchField.setEditableColor(0xFF5555)
        }
        searchField.setMaxLength(100)
        searchField.text = ""
        searchField.setChangedListener { s -> setColor(configList.search(s)) }
        searchField.tooltip = Tooltip.of("fc.config.search.desc".translate())
        directionalLayoutWidget.add(searchField)
        //forward alert button
        directionalLayoutWidget.add(CustomButtonWidget.builder { manager.forwardsHandler() }.size(20, 20).textures("widget/action/alert".fcId(), "widget/action/alert_inactive".fcId(), "widget/action/alert_highlighted".fcId()).narrationSupplier { a, _ -> if (a) "fc.button.alert.active".translate() else "fc.button.alert.inactive".translate() }.activeSupplier { manager.hasForwards() }.tooltipSupplier { a -> if (a) "fc.button.alert.active".translate() else "fc.button.alert.inactive".translate() }.build()) { p -> p.alignLeft() }
        //directionalLayoutWidget.add(TextlessActionWidget("widget/action/alert".fcId(), "widget/action/alert_inactive".fcId(), "widget/action/alert_highlighted".fcId(), "fc.button.alert.active".translate(), "fc.button.alert.inactive".translate(), { manager.hasForwards() } ) { manager.forwardsHandler() })
        //changes button
        directionalLayoutWidget.add(ChangesWidget(scope, { this.width }, manager))
        //done button
        doneButton = CustomButtonWidget.builder { _ -> if (hasShiftDown()) shiftClose() else close() }
            .size(78, 20)
            .messageSupplier {
                if (hasShiftDown() || parent !is ConfigScreen) { ScreenTexts.DONE } else { "fc.config.back".translate() }
            }
            .tooltipSupplier {
                if (parent !is ConfigScreen || hasShiftDown()) "fc.config.done.desc".translate() else "fc.config.back.desc".translate(parent?.title ?: "")
            }.build()
        directionalLayoutWidget.add(doneButton)
    }

    private fun initLayout() {
        layout.refreshPositions()
        val contentHeight = this.height - layout.headerHeight - layout.footerHeight
        configList.setDimensionsAndPosition(320, contentHeight, (this.width / 2) - 160, layout.headerHeight)
    }

    private fun initNarrator() {
        narrator.resetNarrateOnce()
        setElementNarrationDelay(TimeUnit.SECONDS.toMillis(2L))
    }

    private fun setScreenNarrationDelay(delayMs: Long, restartElementNarration: Boolean) {
        this.screenNarrationStartTime = Util.getMeasuringTimeMs() + delayMs
        if (restartElementNarration) {
            this.elementNarrationStartTime = Long.MIN_VALUE
        }
    }

    private fun setElementNarrationDelay(delayMs: Long) {
        this.elementNarrationStartTime = Util.getMeasuringTimeMs() + delayMs
    }

    override fun applyMouseMoveNarratorDelay() {
        this.setScreenNarrationDelay(750L, false)
    }

    override fun applyMousePressScrollNarratorDelay() {
        this.setScreenNarrationDelay(200L, true)
    }

    override fun applyKeyPressNarratorDelay() {
        this.setScreenNarrationDelay(200L, true)
    }

    private fun isNarratorActive(): Boolean {
        return client?.narratorManager?.isActive == true
    }

    private var introUsage = true

    override fun hasUsageText(): Boolean {
        val bl = introUsage
        introUsage = false
        return bl
    }

    override fun updateNarrator() {
        if (this.isNarratorActive()) {
            val l = Util.getMeasuringTimeMs()
            if (l > this.screenNarrationStartTime && l > this.elementNarrationStartTime) {
                this.narrateScreen(true)
                this.screenNarrationStartTime = Long.MAX_VALUE
            }
        }
    }

    override fun narrateScreenIfNarrationEnabled(onlyChangedNarrations: Boolean) {
        if (this.isNarratorActive()) {
            this.narrateScreen(onlyChangedNarrations)
        }
    }

    private fun narrateScreen(onlyChangedNarrations: Boolean) {
        this.narrator.buildNarrations { messageBuilder: NarrationMessageBuilder -> this.addScreenNarrations(messageBuilder) }
        val string = this.narrator.buildNarratorText(!onlyChangedNarrations)
        if (string.isNotEmpty()) {
            client?.narratorManager?.narrate(string)
        }
    }

    override fun renderContents(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        drawMenuListBackground(context)
        super.renderContents(context, mouseX, mouseY, delta)
        drawHeaderAndFooterSeparators(context)
    }

    private fun drawHeaderAndFooterSeparators(context: DrawContext) {
        val identifier = if (client?.world == null) HEADER_SEPARATOR_TEXTURE else HEADER_SEPARATOR_TEXTURE
        val identifier2 = if (client?.world == null) FOOTER_SEPARATOR_TEXTURE else FOOTER_SEPARATOR_TEXTURE
        val contentHeight = this.height - layout.headerHeight - layout.footerHeight
        context.drawTex(identifier, 0, layout.headerHeight - 2, 0.0f, 0.0f, this.width, 2, 32, 2)
        context.drawTex(identifier2, 0, layout.headerHeight + contentHeight, 0.0f, 0.0f, this.width, 2, 32, 2)
    }

    private fun drawMenuListBackground(context: DrawContext) {
        renderBackgroundTexture(context)
        context.setShaderColor(0.125f, 0.125f, 0.125f, 1.0f);
        val contentHeight = this.height - layout.headerHeight - layout.footerHeight
        context.drawTex(OPTIONS_BACKGROUND_TEXTURE, 0, configList.y, 0.0f, 0.0f + configList.topDelta(), this.width, contentHeight, 32, 32)
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        /*val identifier = if (client?.world == null) MENU_LIST_BACKGROUND_TEXTURE else INWORLD_MENU_LIST_BACKGROUND_TEXTURE
        val contentHeight = this.height - layout.headerHeight - layout.footerHeight
        context.drawTex(identifier, 0, layout.headerHeight, 0f, 0f, this.width, contentHeight, 32, 32)*/
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        this.mX = mouseX
        this.mY = mouseY
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        val popup = activeWidget()
        if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, verticalAmount)
        }
        return configList.mouseScrolled(mouseX, mouseY, verticalAmount)
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val global = globalInputHandler?.invoke(button, false, ContextInput.MOUSE, hasControlDown(), hasShiftDown(), hasAltDown())
        if (global != null && global != TriState.DEFAULT) return global.asBoolean
        val contextTypes = ContextType.getRelevantContext(button, ContextInput.MOUSE, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextTypes.isEmpty()) return super.onClick(mouseX, mouseY, button)
        var bl = false
        for (contextType in contextTypes) {
            bl = bl || handleContext(contextType, Position(ContextInput.MOUSE, mouseX.toInt(), mouseY.toInt(), 0, 0, this.width, this.height, this.width, this.height))
        }
        return if(bl)
            true
        else
            super.onClick(mouseX, mouseY, button)

    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val global = globalInputHandler?.invoke(button, true, ContextInput.MOUSE, hasControlDown(), hasShiftDown(), hasAltDown())
        if (global != null && global != TriState.DEFAULT) return global.asBoolean
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val global = globalInputHandler?.invoke(keyCode, false, ContextInput.KEYBOARD, hasControlDown(), hasShiftDown(), hasAltDown())
        if (global != null && global != TriState.DEFAULT) return global.asBoolean
        val contextTypes = ContextType.getRelevantContext(keyCode, ContextInput.KEYBOARD, hasControlDown(), hasShiftDown(), hasAltDown())
        if (contextTypes.isEmpty()) return super.keyPressed(keyCode, scanCode, modifiers)
        var bl = false
        val input = if (MinecraftClient.getInstance().navigationType.isKeyboard) ContextInput.KEYBOARD else ContextInput.MOUSE

        for (contextType in contextTypes) {
            bl = bl || handleContext(contextType, Position(input, mX.toInt(), mY.toInt(), 0, 0, this.width, this.height, this.width, this.height))
        }
        return if (bl) {
            true
        } else {
            val bl2 = super.keyPressed(keyCode, scanCode, modifiers)
            if (!bl2 && contextTypes.contains(ContextType.BACK) && parent is ConfigScreen) {
                this.close()
                true
            } else {
                bl2
            }
        }
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val global = globalInputHandler?.invoke(keyCode, true, ContextInput.KEYBOARD, hasControlDown(), hasShiftDown(), hasAltDown())
        if (global != null && global != TriState.DEFAULT) return global.asBoolean
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun handleContext(contextType: ContextType, position: Position): Boolean {
        return when (contextType) {
            ContextType.CONTEXT_KEYBOARD, ContextType.CONTEXT_MOUSE -> {
                val builder = ContextProvider.empty(position)
                this.provideContext(builder)
                if (builder.isNotEmpty()) {
                    Popups.openContextMenuPopup(builder)
                    true
                } else {
                    false
                }
            }
            ContextType.FIND -> {
                if (this::searchField.isInitialized) {
                    this.focused = searchField
                    true
                } else {
                    false
                }
            }
            ContextType.SEARCH -> {
                if (sidebar.needsSidebar()) {
                    Popups.openGotoPopup(this.sidebar.getAnchors(), this.sidebar.getAnchorWidth(), this.height)
                    true
                } else {
                    false
                }
            }
            ContextType.INFO -> {
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
            ContextType.FULL_EXIT -> {
                shiftClose()
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
        val find = ContextAction.Builder("fc.config.search".translate()) {
                if (this::searchField.isInitialized) { this.focused = searchField; true } else false
            }
            .icon(TextureDeco.CONTEXT_FIND)
        builder.add(ContextResultBuilder.CONFIG, ContextType.SAVE, save)
        builder.add(ContextResultBuilder.CONFIG, ContextType.FIND, find)
    }

    private fun openInfoPopup() {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val list: MutableList<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> = mutableListOf()
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "page_up", KeybindsConfig.INSTANCE.pageUp) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "page_down", KeybindsConfig.INSTANCE.pageDown) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "home", KeybindsConfig.INSTANCE.home) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "end", KeybindsConfig.INSTANCE.end) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "copy", KeybindsConfig.INSTANCE.copy) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "paste", KeybindsConfig.INSTANCE.paste) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "find", KeybindsConfig.INSTANCE.find) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "save", KeybindsConfig.INSTANCE.save) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "undo", KeybindsConfig.INSTANCE.undo) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "context_keyboard", KeybindsConfig.INSTANCE.contextKeyboard) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "context_mouse", KeybindsConfig.INSTANCE.contextMouse) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "back", KeybindsConfig.INSTANCE.back) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "search", KeybindsConfig.INSTANCE.search) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "info", KeybindsConfig.INSTANCE.info) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "full_exit", KeybindsConfig.INSTANCE.fullExit) }
        val listWidget = DynamicListWidget(MinecraftClient.getInstance(), list, 0, 0, 10000, 0, DynamicListWidget.ListSpec(leftPadding = 10, rightPadding = 4, listNarrationKey = "fc.narrator.position.list"))
        val popup = PopupWidget.Builder("fc.button.info".translate())
            .addDivider()
            .add("header", ClickableTextWidget(this, "fc.button.info.fc".translate("Fzzy Config".lit().styled { style ->
                style.withFormatting(Formatting.AQUA, Formatting.UNDERLINE)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://moddedmc.wiki/en/project/fzzy-config/docs"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, "fc.button.info.fc.tip".translate()))
            }), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            .addDivider()
            .add("keybinds", listWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY_WEAK)
            .addDivider()
            .add("alert", TextWidget("fc.button.info.alert".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            .addDoneWidget()
            .widthFunction { sw, _ -> (sw * 0.92).toInt() }
            .heightFunction { sh, h ->
                val newHeight = (sh * 0.9).toInt()
                val heightDelta = newHeight - h
                listWidget.setH(listWidget.height + heightDelta)
                newHeight
            }
            .onClose { KeybindsConfig.INSTANCE.save() }
            .build()
        PopupWidget.push(popup)
    }
}