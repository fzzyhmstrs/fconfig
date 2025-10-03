/*
* Copyright (c) 2024-2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.internal

import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.config.SearchConfig
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.isAltDown
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.isControlDown
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.isShiftDown
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.TriState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.DirectionalLayoutWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.util.concurrent.TimeUnit
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

    private var initialInit = false
    private var parent: Screen? = null
    private var globalInputHandler: ((key: Int, released: Boolean, type: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean) -> TriState)? = null

    internal lateinit var layout: ThreePartsLayoutWidget
    private lateinit var searchField: NavigableTextFieldWidget
    private lateinit var doneButton: CustomButtonWidget

    private var mX: Double = 0.0
    private var mY: Double = 0.0

    private val narrator: ConfigScreenNarrator by lazy {
        setElementNarrationDelay(TimeUnit.SECONDS.toMillis(2L))
        ConfigScreenNarrator()
    }
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

    fun getCurrentSearch(): String {
        return try {
            searchField.text
        } catch (e: Throwable) {
            ""
        }
    }

    fun scrollToEntry(scope: String) {
        configList.scrollToEntry(scope)
    }

    fun openEntry(rawEntryString: String) {
        if (processEntry(rawEntryString)) return
        val l = rawEntryString.split('.')
        if (l.isEmpty()) return
        val entry = l[0]
        val args = if (l.size > 1) l.subList(1, l.size) else listOf()
        openEntry(entry, args)
    }

    fun processEntry(rawEntryString: String): Boolean {
        if (rawEntryString.startsWith("::") && rawEntryString.length > 2) {
            //check if I have to also prompt a search field change
            searchField.text = rawEntryString.substring(2)
            return true
        }
        return false
    }

    fun openEntry(entry: String, args: List<String>) {
        val configScopePair = ClientConfigRegistry.getValidClientConfig(this.scope)
        val config = configScopePair.first ?: return
        val returnedScope = configScopePair.second
        val e = if (returnedScope == this.scope)
            entry
        else
            this.scope.removePrefix(returnedScope).removePrefix(".") + "." + entry
        ConfigApiImpl.drill(config, e, '.', ConfigApiImpl.IGNORE_NON_SYNC) { _, _, _, thing, _, _, _, _ ->
            if (thing is EntryOpener) {
                thing.open(args)
            }
        }
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

    override fun onDisplayed() {
        initialInit = true
        super.onDisplayed()
    }

    override fun init() {
        layout = ThreePartsLayoutWidget(this)
        super.init()
        initHeader()
        initFooter()
        initBody()
        initLayout()
        if (isNarratorActive())
            initNarrator()
        initialInit = false
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
            if (drawableElement is LayoutClickableWidget) {
                for (element in drawableElement.children()) {
                    if (element is Drawable && element is Selectable) {
                        addDrawableChild(element)
                    }
                }
            } else {
                addDrawableChild(drawableElement)
            }
        }
        configList.onReposition()
    }
    private fun initFooter() {
        val directionalLayoutWidget = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8))
        //goto button
        directionalLayoutWidget.add(CustomButtonWidget.builder { Popups.openGotoPopup(this.sidebar.getAnchors(), this.sidebar.getAnchorWidth(), this.height) }.size(20, 20).textures(TextureIds.GOTO_SET).narrationSupplier { _, _ -> TextureIds.GOTO_LANG }.activeSupplier { sidebar.needsSidebar() }.tooltip(TextureIds.GOTO_LANG).build()) { p -> p.alignLeft() }
        //info button
        directionalLayoutWidget.add(CustomButtonWidget.builder { Popups.openInfoPopup(this) }.size(20, 20).textures(TextureIds.INFO_SET).narrationSupplier { _, _ -> TextureIds.INFO_LANG }.tooltip(TextureIds.INFO_LANG).build()) { p -> p.alignLeft() }
        //search bar
        val searchText = if (this::searchField.isInitialized && !(SearchConfig.INSTANCE.clearSearch.get() && initialInit)) {
            searchField.text
        } else {
            ""
        }
        val searchFieldText = if (this::searchField.isInitialized) {
            searchField.text
        } else {
            ""
        }
        searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, 109, 20, FcText.EMPTY)
        fun setColor(entries: Int) {
            if(entries > 0)
                searchField.setEditableColor(-1)
            else if (entries == 0)
                searchField.setEditableColor(-43691)
            else
                searchField.setEditableColor(-256)
        }
        searchField.setMaxLength(100)
        searchField.setChangedListener { s -> setColor(configList.search(s)) }
        if (searchText != searchFieldText || searchText.isNotEmpty())
            searchField.text = searchText
        searchField.setTooltip(Tooltip.of("fc.config.search.desc".translate()))

        val layout = LayoutWidget.Builder().paddingBoth(0).spacingBoth(0).build()
        layout.add(
            "search",
            searchField,
            LayoutWidget.Position.LEFT,
            LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
        layout.add(
            "menu",
            CustomButtonWidget.builder(TextureIds.MENU_LANG) {
                Popups.openSearchMenuPopup() }
                .noMessage()
                .size(11, 10)
                .tooltip(TextureIds.MENU_LANG)
                .textures(TextureIds.MENU, TextureIds.MENU_DISABLED, TextureIds.MENU_HIGHLIGHTED)
                .build(),
            LayoutWidget.Position.RIGHT,
            LayoutWidget.Position.ALIGN_RIGHT,
            LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE
        )
        layout.add(
            "clear",
            CustomButtonWidget.builder(TextureIds.MENU_CLEAR_LANG) {
                searchField.text = "" }
                .noMessage()
                .size(11, 10)
                .tooltip(TextureIds.MENU_CLEAR_LANG)
                .textures(TextureIds.KEYBIND_CLEAR, TextureIds.KEYBIND_CLEAR_DISABLED, TextureIds.KEYBIND_CLEAR_HIGHLIGHTED)
                .build(),
            LayoutWidget.Position.BELOW,
            LayoutWidget.Position.ALIGN_RIGHT,
            LayoutWidget.Position.VERTICAL_TO_LEFT_EDGE
        )

        directionalLayoutWidget.add(LayoutClickableWidget(0, 0, 120, 20, layout))
        //forward alert button
        directionalLayoutWidget.add(CustomButtonWidget.builder { manager.forwardsHandler() }.size(20, 20).textures("widget/action/alert".fcId(), "widget/action/alert_inactive".fcId(), "widget/action/alert_highlighted".fcId()).narrationSupplier { a, _ -> if (a) "fc.button.alert.active".translate() else "fc.button.alert.inactive".translate() }.activeSupplier { manager.hasForwards() }.tooltipSupplier { a -> if (a) "fc.button.alert.active".translate() else "fc.button.alert.inactive".translate() }.build()) { p -> p.alignLeft() }
        //directionalLayoutWidget.add(TextlessActionWidget("widget/action/alert".fcId(), "widget/action/alert_inactive".fcId(), "widget/action/alert_highlighted".fcId(), "fc.button.alert.active".translate(), "fc.button.alert.inactive".translate(), { manager.hasForwards() } ) { manager.forwardsHandler() })
        //changes button
        directionalLayoutWidget.add(ChangesWidget(scope, { this.width }, manager))
        //done button
        doneButton = CustomButtonWidget.builder { _ -> if (isShiftDown()) shiftClose() else close() }
            .size(78, 20)
            .messageSupplier {
                if (isShiftDown() || parent !is ConfigScreen) { ScreenTexts.DONE } else { "fc.config.back".translate() }
            }
            .tooltipSupplier {
                if (parent !is ConfigScreen || isShiftDown()) "fc.config.done.desc".translate() else "fc.config.back.desc".translate(parent?.title ?: "")
            }.build()
        directionalLayoutWidget.add(doneButton)
    }

    private fun initLayout() {
        layout.refreshPositions()
        configList.setDimensionsAndPosition(320, layout.contentHeight, (this.width / 2) - 160, layout.headerHeight)
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

    override fun refreshNarrator(previouslyDisabled: Boolean) {
        if (previouslyDisabled) {
            this.setScreenNarrationDelay(TimeUnit.SECONDS.toMillis(2L), false)
        }

        if (this.narratorToggleButton != null) {
            narratorToggleButton?.setValue(client?.options?.narrator?.value)
        }
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
            client?.narratorManager?.narrate(Text.literal(string))
        }
    }

    override fun getUsageNarrationText(): Text {
        return if (client?.navigationType?.isKeyboard == true)
            super.getUsageNarrationText()
        else
            "narrator.screen.usage".translate()
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
        val identifier = if (client?.world == null) menuListBackground else inWorldMenuListBackground
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
        val global = globalInputHandler?.invoke(button, false, ContextInput.MOUSE, isControlDown(), isShiftDown(), isAltDown())
        if (global != null && global != TriState.DEFAULT) return global.asBoolean
        val contextTypes = ContextType.getRelevantContext(button, ContextInput.MOUSE, isControlDown(), isShiftDown(), isAltDown())
        if (contextTypes.isEmpty()) return super.onClick(mouseX, mouseY, button)
        val activeWidget = activeWidget()
        if (activeWidget != null || justClosedWidget) {
            for (contextType in contextTypes) {
                if (contextType == ContextType.CONTEXT_MOUSE) {
                    if (activeWidget != null)
                        PopupWidget.popImmediate()
                    hoveredElement = children().firstOrNull { it.isMouseOver(mX, mY) }
                    val builder = ContextProvider.empty(Position(if (MinecraftClient.getInstance().navigationType.isKeyboard) ContextInput.KEYBOARD else ContextInput.MOUSE, mX.toInt(), mY.toInt(), activeWidget?.x ?: 0, activeWidget?.y ?: 0, this.width, this.height, this.width, this.height))
                    this.provideContext(builder)
                    return if (builder.isNotEmpty()) {
                        Popups.openContextMenuPopup(builder, true)
                        true
                    } else {
                        false
                    }
                }
            }
            return super.onClick(mouseX, mouseY, button)
        }

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
        val global = globalInputHandler?.invoke(button, true, ContextInput.MOUSE, isControlDown(), isShiftDown(), isAltDown())
        if (global != null && global != TriState.DEFAULT) return global.asBoolean
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val global = globalInputHandler?.invoke(keyCode, false, ContextInput.KEYBOARD, isControlDown(), isShiftDown(), isAltDown())
        if (global != null && global != TriState.DEFAULT) return global.asBoolean

        val contextTypes = ContextType.getRelevantContext(keyCode, ContextInput.KEYBOARD, isControlDown(), isShiftDown(), isAltDown())
        if (contextTypes.isEmpty()) return super.keyPressed(keyCode, scanCode, modifiers)

        val activeWidget = activeWidget()
        if (activeWidget != null) {
            for (contextType in contextTypes) {
                if (contextType == ContextType.CONTEXT_KEYBOARD) {
                    PopupWidget.popImmediate()
                    hoveredElement = children().firstOrNull { it.isMouseOver(mX, mY) }
                    val builder = ContextProvider.empty(Position(if (MinecraftClient.getInstance().navigationType.isKeyboard) ContextInput.KEYBOARD else ContextInput.MOUSE, mX.toInt(), mY.toInt(), activeWidget.x, activeWidget.y, this.width, this.height, this.width, this.height))
                    this.provideContext(builder)
                    return if (builder.isNotEmpty()) {
                        Popups.openContextMenuPopup(builder, true)
                        true
                    } else {
                        false
                    }
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers)
        }

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
        val global = globalInputHandler?.invoke(keyCode, true, ContextInput.KEYBOARD, isControlDown(), isShiftDown(), isAltDown())
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
                Popups.openInfoPopup(this)
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


}