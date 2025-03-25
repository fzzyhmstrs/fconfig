/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.decoration.AbstractDecorationWidget
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.decoration.DecorationWidget
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry.ContentBuilder
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomMultilineTextWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.isEmpty
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.*
import net.minecraft.client.gui.navigation.GuiNavigationType
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.FocusedTooltipPositioner
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.OrderedText
import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Consumer
import java.util.function.UnaryOperator
import kotlin.math.min

/**
 * An entry for a config setting list. Presents the setting name, any prefix text, as well as content buttons and decorations.
 * @param parentElement [DynamicListWidget] the settings widget the entry is being added to. This is provided internally; Fzzy Config asks for instances of Function$lt;[DynamicListWidget], [ConfigEntry]&gt; so it can finish construction lazily when needed.
 * @param content [ContentBuilder.BuildResult] built contents of this entry.
 * @param texts [Translatable.Result] translation information for this entry. This is provided by [EntryCreator.CreatorContext]
 * @see [ContentBuilder]
 * @sample me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators.createConfigEntry
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class ConfigEntry(parentElement: DynamicListWidget, content: ContentBuilder.BuildResult, texts: Translatable.Result) :
    DynamicListWidget.Entry(parentElement, texts, content.scope, content.visibility)
{

    private val layout: LayoutWidget = if (content.groupTypes.isEmpty()) {
        content.layoutWidget.setPos(this.x, this.top).compute()
    } else {
        val lo = LayoutWidget(paddingW = 0, spacingW = 0)
        for ((index, bl) in content.groupTypes.withIndex()) {
            lo.add("$index", GroupLineWidget(bl), LayoutWidget.Position.RIGHT, LayoutWidget.Position.ALIGN_LEFT_OF_AND_STRETCH, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        }
        lo.add("layout", content.layoutWidget, LayoutWidget.Position.RIGHT, LayoutWidget.Position.POSITION_RIGHT_OF_AND_JUSTIFY, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        lo.setPos(this.x, this.top)
        lo.compute()
    }

    private val groupOffset = content.groupTypes.size * 7
    private val actions: List<AbstractDecorationWidget> = content.actionWidgets
    private var children: MutableList<Element> = mutableListOf()
    private var drawables: List<Drawable> = emptyList()
    private var selectables: List<SelectableElement> = emptyList()
    private var narratables: List<AbstractTextWidget> = emptyList()
    private var tooltipProviders: List<TooltipChild> = emptyList()

    private val searchResults: Function<String, List<Translatable.Result>> = content.searchResults
    private val contextBuilders: Map<String, Map<ContextType, ContextAction.Builder>> = content.contextActions
    private val context: Map<ContextType, ContextAction> by lazy {
        contextBuilders.entries.stream().collect(
            { mutableMapOf() },
            { map, entry -> map.putAll(entry.value.mapValues { it.value.build() }) },
            { m1, m2 -> m1.putAll(m2) })
    }

    private val tooltip: List<OrderedText> by lazy {
        createTooltip(texts.desc ?: FcText.empty())
    }

    override fun init() {
        layout.setPos(this.x, this.top)
        layout.setWidth(this.w.get())
        val c: MutableList<Element> = mutableListOf()
        val d: MutableList<Drawable> = mutableListOf()
        val s: MutableList<Selectable> = mutableListOf()
        val n: MutableList<AbstractTextWidget> = mutableListOf()
        val t: MutableList<TooltipChild> = mutableListOf()
        layout.categorize(c, d, s) { w ->
            if (w is AbstractTextWidget && w !is OnClickTextFieldWidget)
                n.add(w)
            if (w is TooltipChild)
                t.add(w)
            if (w is SuggestionWindowProvider)
                w.addListener(parentElement)
        }
        narratables = n.filter { widget -> widget.message != texts.name }
        c.addAll(actions.mapNotNull { it.nullCast() })
        t.addAll(actions.mapNotNull { it.nullCast() })
        children = c
        drawables = d
        selectables = s.filterNot { it is AbstractTextWidget && it !is OnClickTextFieldWidget }.filterIsInstance<Element>().cast()
        tooltipProviders = t
    }

    override fun onResize() {
        layout.setWidth(this.w.get())
        layout.update()
    }

    override fun onScroll(dY: Int) {
        layout.update()
    }

    override fun entrySearchResults(searchInput: String): List<Translatable.Result> {
        return searchResults.apply(searchInput)
    }

    override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        for (drawable in drawables) {
            drawable.render(context, mouseX, mouseY, delta)
        }
        val keyboardFocused = focused && MinecraftClient.getInstance().navigationType.isKeyboard

        val tooltipList: MutableList<OrderedText> = mutableListOf()
        if (tooltipPrefix.isNotEmpty()) {
            tooltipList.addAll(tooltipPrefix.map { it.asOrderedText() }
            tooltipList.add(OrderedText.EMPTY)
        }
        for ((index, provider) in tooltipProviders.withIndex()) {
            val tt = provider.provideTooltipLines(mouseX, mouseY, hovered || keyboardFocused, keyboardFocused)
            for (t in tt) {
                val ttt = createTooltip(t)
                if (ttt.isNotEmpty())
                    tooltipList.addAll(ttt)
            }
            if (tt.isNotEmpty() && index != tooltipProviders.lastIndex) {
                tooltipList.add(OrderedText.EMPTY)
            }
        }

        if ((hovered && !MinecraftClient.getInstance().navigationType.isKeyboard) || (focused && MinecraftClient.getInstance().navigationType.isKeyboard)) {
            tooltipList.addAll(tooltip)
        }
        if (tooltipList.isNotEmpty()) {
            if (tooltipList.last() == OrderedText.EMPTY) {
                tooltipList.removeLast()
            }
            if (keyboardFocused) {
                MinecraftClient.getInstance().currentScreen?.setTooltip(tooltipList, FocusedTooltipPositioner(ScreenRect(x + 2, y + 4, width, height)), true)
            } else {
                MinecraftClient.getInstance().currentScreen?.setTooltip(tooltipList, HoveredTooltipPositioner.INSTANCE, true)
            }
        }

    }

    override fun renderExtras(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (actions.isNotEmpty()) {
            var offset = -24
            val offsetIncrement = if(actions.size == 1) 19 else min(19, (x - 24) / (actions.size - 1))
            for (action in actions) {
                action.x = x + offset
                action.y = (layout.getElement("title")?.getTop() ?: y)
                action.render(context, mouseX, mouseY, delta)
                offset -= offsetIncrement
            }
        }
    }

    override fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if ((hovered && !MinecraftClient.getInstance().navigationType.isKeyboard) || (focused && MinecraftClient.getInstance().navigationType.isKeyboard))
            context.drawBorder(x - 2 + groupOffset, y - 2, width + 4 - groupOffset, height + 4, -1)
        else if (focused || hovered)
            context.drawBorder(x - 2 + groupOffset, y - 2, width + 4 - groupOffset, height + 4, -6250336)
    }

    override fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (hovered)
            context.fill(x - 1 + groupOffset, y - 1, x + width + 1, y + height + 1, 1684300900)
    }

    @Internal
    override fun appendNarrations(builder: NarrationMessageBuilder) {
        super.appendNarrations(builder)
        val narratablesNarrations = narratables.filter { it.isNarratable }.map { it.message.asOrderedText() }
        val childNarrations = tooltipProviders.flatMap { it.provideNarrationLines() }.map { it.asOrderedText() }
        val str = createTooltipString( narratablesNarrations + childNarrations + tooltip)
        if (str.isNotEmpty()) {
            builder.put(NarrationPart.HINT, str)
            if (childNarrations.isNotEmpty())
                builder.put(NarrationPart.HINT, str) //put twice to force re-narration even if the message between two settings is the same
        }
    }

    private fun createTooltip(input: Text): List<OrderedText> {
        val list: MutableList<OrderedText> = mutableListOf()
        if (input.isEmpty()) return list
        list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(input, 190))
        return list
    }


    private fun createTooltipString(tt: List<OrderedText>): String {
        val builder = StringBuilder()
        for (tip in tt) {
            tip.accept { _, _, codepoint ->
                builder.appendCodePoint(codepoint)
                true
            }
            builder.append(". ")
        }
        return builder.toString()
    }

    @Internal
    override fun children(): MutableList<out Element> {
        return children
    }

    @get:Internal
    @set:Internal
    override var h: Int
        get() = layout.height
        set(value) {
            layout.height = value
        }

    @Internal
    override fun selectableChildren(): List<SelectableElement> {
        return selectables
    }

    override fun provideContext(builder: ContextResultBuilder) {
        val content = layout.getElement("content")
        builder.move { position ->
            if (content != null && position.contextInput == ContextInput.KEYBOARD) {
                position.copy(x = content.getLeft(), y = content.getTop(), width = content.elWidth(), height = content.elHeight())
            } else if (content != null) {
                position.copy(width = content.elWidth(), height = content.elHeight())
            } else {
                position
            }
        }
        for ((g, actions) in contextBuilders) {
            builder.addAll(g, actions.filter { (_, builder) -> builder.isForMenu() })
        }
    }

    override fun handleContext(contextType: ContextType, position: Position): Boolean {
        val action = context[contextType] ?: return false
        val content = layout.getElement("content")
        val newPosition = if (content != null && position.contextInput == ContextInput.KEYBOARD) {
            position.copy(x = content.getLeft(), y = content.getTop(), width = content.elWidth(), height = content.elHeight() )
        } else if (content != null) {
            position.copy(width = content.elWidth(), height = content.elHeight())
        } else {
            position
        }
        return action.action.apply(newPosition)
    }

    //////////////////////////////////////

    /**
     * Builds content information for constructing a [ConfigEntry]
     * @param context [EntryCreator.CreatorContext] creator context used by this builder for a variety of information
     * @param actionWidgets List$lt;[AbstractDecorationWidget]$gt; decorations drawn on the left side of the screen to provide usage alerts (requires restart etc.). the creator context provides a list of actions that can be converted into [ActionDecorationWidget] for this purpose. See [ActionDecorationWidget.setting] and other methods for details.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    class ContentBuilder(private val context: EntryCreator.CreatorContext, private val actionWidgets: List<AbstractDecorationWidget>) {

        /**
         * Builds content information for constructing a [ConfigEntry]
         * @param context [EntryCreator.CreatorContext] creator context used by this builder for a variety of information
         * @param actions Set&lt;[Action]&gt; set of actions to transform into [ActionDecorationWidget] using the [ActionDecorationWidget.setting] method. See the constructor overload with just context for automatically passing actions from the context itself.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        constructor(context: EntryCreator.CreatorContext, actions: Set<Action>): this(context, actions.map { ActionDecorationWidget.setting(it) })

        /**
         * Builds content information for constructing a [ConfigEntry]. Automatically uses the set of [Action] inside the context to build action widgets.
         * @param context [EntryCreator.CreatorContext] creator context used by this builder for a variety of information
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        constructor(context: EntryCreator.CreatorContext): this(context, context.actions.map { ActionDecorationWidget.setting(it) })

        private var mainLayout: LayoutWidget = LayoutWidget(paddingW = 0, spacingW = 0)
        private var contentLayout: LayoutWidget = LayoutWidget(paddingW = 0).clampWidth(110)
        private val decorationWidget = DecorationWidget()
        private var group: String = ""
        private var visibility: DynamicListWidget.Visibility = DynamicListWidget.Visibility.VISIBLE
        private var contextActions: Map<String, Map<ContextType, ContextAction.Builder>> = mapOf()
        private val popStart = context.groups.size - context.annotations.filterIsInstance<ConfigGroup.Pop>().size
        private var searchResults: Function<String, List<Translatable.Result>> = EMPTY_RESULTS

        init {
            val nameSupplier = { context.texts.name }
            val titleWidget = SuppliedTextWidget(nameSupplier, MinecraftClient.getInstance().textRenderer, 70, 20).supplyTooltipOnOverflow(nameSupplier).align(0.0f)

            mainLayout.add("content", contentLayout, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_RIGHT)
            mainLayout.add("deco", decorationWidget, "content", LayoutWidget.Position.LEFT)
            mainLayout.add("title", titleWidget, "deco", LayoutWidget.Position.POSITION_LEFT_OF_AND_JUSTIFY, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        }

        /**
         * Modify the main layout of the entry. This should only be used if the structure of the entry needs to change substantially. For most circumstances, [layoutContent] is proper
         * @param layoutOperations [UnaryOperator]&lt;[LayoutWidget]&gt; modifiers to apply to the main layout of the entry. This can fully replace the layout; doing so will invalidate any changes to the content layout made before or after this replacement.
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @Deprecated("Consider layoutContent unless total structural change of the entry is needed")
        fun layoutMain(layoutOperations: UnaryOperator<LayoutWidget>): ContentBuilder {
            mainLayout = layoutOperations.apply(mainLayout)
            return this
        }

        /**
         * Modify the content area of the entry. The content area is the "button" area of a standard entry layout. It is common to apply one widget to this area, which is the entire "setting button" for the entry.
         *
         * The content area is bounded to 110 pixels wide, and can be an arbitrary height, but the standard is 20px.
         * ```
         * [Prefix text goes above the entry]
         * [Title here       ][Deco][Content] <- lays out here
         * ```
         * @param layoutOperations [UnaryOperator]&lt;[LayoutWidget]&gt; modifiers to apply to the content layout of the entry. This can fully replace the layout; though that doesn't serve particularly much purpose as the content layout starts empty already
         * @return this builder
         * @author fzzyhmstrs
         * @since
         */
        fun layoutContent(layoutOperations: UnaryOperator<LayoutWidget>): ContentBuilder {
            contentLayout = layoutOperations.apply(contentLayout)
            return this
        }

        /**
         * Applies a decoration to this entry.
         * @param decoration [Decorated] deco to render. [TextureDeco] has some builtin options.
         * @param offsetX horizontal offset for rendering. Many standard decos need 2 pixels of offset, as they are 16x and this area is 20x
         * @param offsetY vertical offset for rendering. Many standard decos need 2 pixels of offset, as they are 16x and this area is 20x
         * @return this builder
         * @see [TextureDeco]
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun decoration(decoration: Decorated, offsetX: Int = 0, offsetY: Int = 0): ContentBuilder {
            this.decorationWidget.setDeco(decoration, offsetX, offsetY)
            return this
        }

        internal fun group(group: String): ContentBuilder {
            this.group = group
            return this
        }

        /**
         * Defines a starting visibility for the entry. Default is [DynamicListWidget.Visibility.VISIBLE]
         * @param visibility [DynamicListWidget.Visibility.VISIBLE] new starting visibility to apply
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun visibility(visibility: DynamicListWidget.Visibility): ContentBuilder {
            this.visibility = visibility
            return this
        }

        /**
         * A map of context actions for the entry to use in context handling. These are the right-click and keybind actions relevant to the setting.
         * @param contextActions Map&lt;String, Map&lt;[ContextType], [ContextAction.Builder]&gt;&gt; - context action map. This is the same used in a [ContextResultBuilder], and like the builder this map should be linked ([LinkedHashMap] etc.)
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun contextActions(contextActions: Map<String, Map<ContextType, ContextAction.Builder>>): ContentBuilder {
            this.contextActions = contextActions
            return this
        }

        /**
         * Search results to "pass up" to the parent list when requested. This is used to determine what children should stay visible by indirect search matching.
         * @param searchResults [Function]&lt;String, List&lt;[Translatable.Result]&gt;&gt; the serach result provider for this entry. Using a [Searcher] is prucent, as the provided string is raw, with special characters still included.
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun searchResults(searchResults: Function<String, List<Translatable.Result>>): ContentBuilder {
            this.contextActions = contextActions
            return this
        }

        /**
         * Builds a [BuildResult] for construction of an Entry
         * @return [BuildResult]
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun build(): BuildResult {
            val prefixWidget = context.texts.prefix?.let { CustomMultilineTextWidget(it, 10, 10, 4) }
            val finalLayout = if (prefixWidget != null) {
                val fl = LayoutWidget(paddingW = 0, spacingW = 0)
                fl.add("prefix", prefixWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY)
                if (!mainLayout.isEmpty()) {
                    fl.add("main", mainLayout, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
                }
                fl
            } else {
                mainLayout
            }

            val groupTypes: MutableList<Boolean> = mutableListOf()
            for (i in 0 until context.groups.size) {
                if (context.groups[i] == group) continue
                groupTypes.add(i >= popStart)
            }

            return BuildResult(
                finalLayout,
                actionWidgets,
                DynamicListWidget.Scope(context.scope, group, context.groups),
                groupTypes,
                visibility,
                contextActions,
                searchResult)
        }

        /**
         * Built entry content for construction of a [ConfigEntry]. These values are used internally by the entry during instantiation.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        class BuildResult internal constructor(
            internal val layoutWidget: LayoutWidget,
            internal val actionWidgets: List<AbstractDecorationWidget>,
            internal val scope: DynamicListWidget.Scope,
            internal val groupTypes: List<Boolean>,
            internal val visibility: DynamicListWidget.Visibility,
            internal val contextActions: Map<String, Map<ContextType, ContextAction.Builder>>,
            internal val searchResults: Function<String, List<Translatable.Result>>)

    }

    ////////////////////////////////

    /**
     * A decoration widget that renders an action icon from an [Action] and displays its tooltip
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    class ActionDecorationWidget private constructor(private val action: Action, private val actionTooltip: Text = action.settingTooltip): AbstractDecorationWidget(), TooltipChild {

        companion object {
            /**
             * Creates a [ActionDecorationWidget] that uses the setting tooltip for the given action
             * @param action [Action] the config action to render
             * @return [ActionDecorationWidget]
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            fun setting(action: Action): ActionDecorationWidget {
                return ActionDecorationWidget(action)
            }

            /**
             * Creates a [ActionDecorationWidget] that uses the section tooltip for the given action
             * @param action [Action] the config action to render
             * @return [ActionDecorationWidget]
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            fun section(action: Action): ActionDecorationWidget {
                return ActionDecorationWidget(action, action.sectionTooltip)
            }

            /**
             * Creates a [ActionDecorationWidget] that uses the config tooltip for the given action
             * @param action [Action] the config action to render
             * @return [ActionDecorationWidget]
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            fun config(action: Action): ActionDecorationWidget {
                return ActionDecorationWidget(action, action.configTooltip)
            }
        }

        @Internal
        fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return ((mouseX >= x.toDouble()) && (mouseY >= y.toDouble()) && (mouseX < (x + this.width).toDouble()) && (mouseY < (y + this.height).toDouble()))
        }

        /**
         * @suppress
         */
        override fun getWidth(): Int {
            return 19
        }

        /**
         * @suppress
         */
        override fun forEachChild(consumer: Consumer<ClickableWidget>?) {}

        /**
         * @suppress
         */
        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            context.drawTex(action.sprite, x, y, 20, 20)
        }

        /**
         * @suppress
         */
        override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
            val bl1 = isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && MinecraftClient.getInstance().navigationType == GuiNavigationType.MOUSE
            if (!bl1 && !keyboardFocused) return TooltipChild.EMPTY
            return listOf(actionTooltip)
        }
    }

    ////////////////////////////////

    private inner class GroupLineWidget(private val end: Boolean): Drawable, Widget, Scalable {

        private var x: Int = 0
        private var y: Int = 0
        private var height = 20

        override fun setX(x: Int) { this.x = x }

        override fun setY(y: Int) { this.y = y }

        override fun getX(): Int { return x }

        override fun getY(): Int { return y }

        override fun getWidth(): Int { return 7 }

        override fun getHeight(): Int { return height }

        override fun setWidth(width: Int) {
        }

        override fun setHeight(height: Int) {
            this.height = height
        }

        override fun forEachChild(consumer: Consumer<ClickableWidget>?) {
        }

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val p = this@ConfigEntry.parentElement.verticalPadding
            if (end) {
                context.fill(x, y - p, x + 1, y + height - 1, -1)
                context.fill(x + 1, y - p, x + 2, y + height - 2, -12698050)
                context.fill(x + 1, y + height - 2, x + 3, y + height - 1, -1)
                context.fill(x + 1, y + height - 1, x + 4, y + height, -12698050)
            } else {
                context.fill(x, y - p, x + 1, y + height, -1)
                context.fill(x + 1, y - p, x + 2, y + height, -12698050)
            }
        }

    }
}
