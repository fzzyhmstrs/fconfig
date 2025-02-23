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

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.PopupParentElement
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.MouseClickResult
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DividerWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.RenderUtil
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawNineSlice
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.renderBlur
import me.fzzyhmstrs.fzzy_config.util.pos.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.*
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigation.Arrow
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.navigation.NavigationDirection
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.function.*
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

/**
 * A widget comprised of a collection of child elements that "Pops up" onto screens that implement [me.fzzyhmstrs.fzzy_config.screen.PopupParentElement]
 *
 * Multiple popups can stack onto PopupParentElements. They display last-added in front, first added in back, and are removed in reverse of the order they were added (First In Last Out)
 * @see Builder
 * @author fzzyhmstrs
 * @since 0.2.0, incorporates [LayoutWidget] as of 0.6.0
 */
//client
class PopupWidget
    private constructor(
        private var message: Text,
        private val width: Int,
        private val height: Int,
        private val blurBackground: Boolean,
        private val closeOnOutOfBounds: Boolean,
        private val background: Identifier,
        private val positionX: BiFunction<Int, Int, Int>,
        private val positionY: BiFunction<Int, Int, Int>,
        private val positioner: BiConsumer<Int, Int>,
        private val onClose: Runnable,
        private val onClick: MouseClickResult,
        private val children: List<Element>,
        private val selectables: List<Selectable>,
        private val drawables: List<Drawable>)
    :
    ParentElement,
    Narratable,
    Drawable,
    SuggestionWindowListener
{

    private var x: Int = 0
    private var y: Int = 0
    private var focused: Element? = null
    private var focusedSelectable: Selectable? = null
    private var dragging = false
    private var suggestionWindowElement: Element? = null

    init {
        for (child in children) {
            if (child is SuggestionWindowProvider)
                child.addListener(this)
        }
    }

    override fun setSuggestionWindowElement(element: Element?) {
        this.suggestionWindowElement
    }

    fun onClose() {
        this.onClose.run()
    }

    fun closesOnMissedClick(): Boolean {
        return closeOnOutOfBounds
    }

     fun blur() {
        val guiNavigationPath = this.focusedPath
        guiNavigationPath?.setFocused(false)
    }

    fun position(screenWidth: Int, screenHeight: Int) {
        this.x = positionX.apply(screenWidth, width) //screenWidth/2 - width/2
        this.y = positionY.apply(screenHeight, height) //screenHeight/2 - height/2
        positioner.accept(this.x, this.y)
        for (el in children) {
            if (el is RepositioningWidget) {
                el.onReposition()
            }
        }
    }

    override fun children(): List<Element> {
        return children
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (blurBackground) {
            renderBlur(context, x.toFloat(), y.toFloat(), delta)
        }
        RenderSystem.enableBlend()
        RenderSystem.disableDepthTest()
        context.drawNineSlice(background, x, y, width, height)
        for (drawable in drawables) {
            /*RenderSystem.disableDepthTest()
            RenderSystem.disableBlend()*/
            drawable.render(context, mouseX, mouseY, delta)
        }

    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return suggestionWindowElement?.mouseClicked(mouseX, mouseY, button)?.takeIf { it } ?: super.mouseClicked(mouseX, mouseY, button).takeIf { it } ?: isMouseOver(mouseX, mouseY)
    }

    fun preClick(mouseX: Double, mouseY: Double, button: Int): ClickResult {
        return onClick.onClick(mouseX, mouseY, isMouseOver(mouseX, mouseY), button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return suggestionWindowElement?.mouseReleased(mouseX, mouseY, button) ?: super.mouseReleased(mouseX, mouseY, button)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x.toDouble() && mouseY >= y.toDouble() && mouseX < (x + width).toDouble() && mouseY < (y + height).toDouble()
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return suggestionWindowElement?.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) ?: super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return suggestionWindowElement?.mouseScrolled(mouseX, mouseY, amount) ?: super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (suggestionWindowElement?.keyPressed(keyCode, scanCode, modifiers) ?: super.keyPressed(keyCode, scanCode, modifiers)) {
            return true
        }
        val guiNavigation: GuiNavigation? = when(keyCode) {
            GLFW.GLFW_KEY_LEFT -> getArrowNavigation(NavigationDirection.LEFT)
            GLFW.GLFW_KEY_RIGHT -> getArrowNavigation(NavigationDirection.RIGHT)
            GLFW.GLFW_KEY_UP -> getArrowNavigation(NavigationDirection.UP)
            GLFW.GLFW_KEY_DOWN -> getArrowNavigation(NavigationDirection.DOWN)
            GLFW.GLFW_KEY_TAB ->  getTabNavigation()
            else -> null
        }
        if(guiNavigation != null) {
            var guiNavigationPath = super.getNavigationPath(guiNavigation)
            if (guiNavigationPath == null && guiNavigation is GuiNavigation.Tab) {
                blur()
                guiNavigationPath = super.getNavigationPath(guiNavigation)
            }
            if (guiNavigationPath != null) {
                this.switchFocus(guiNavigationPath)
            }
        }
        return false
    }

    private fun getTabNavigation(): GuiNavigation.Tab {
        val bl = !Screen.hasShiftDown()
        return GuiNavigation.Tab(bl)
    }
    private fun getArrowNavigation(direction: NavigationDirection): Arrow {
        return Arrow(direction)
    }
    private fun switchFocus(path: GuiNavigationPath) {
        blur()
        path.setFocused(true)
    }

    private fun trySetFocused(focused: Element) {
        if (!children().contains(focused)) return
        setFocused(focused)
    }

    override fun setFocused(focused: Element?) {
        this.focused?.let { it.isFocused = false }
        focused?.let { it.isFocused = true }
        this.focused = focused
    }

    override fun setFocused(focused: Boolean) {
    }

    override fun isFocused(): Boolean {
        return getFocused() != null
    }

    override fun isDragging(): Boolean {
        return dragging
    }

    override fun setDragging(dragging: Boolean) {
        this.dragging = dragging
    }

    override fun getFocused(): Element? {
        return focused
    }

    override fun appendNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, message)
        val list: List<Selectable> = this.selectables.filter { it.isNarratable }
        val selectedElementNarrationData = Screen.findSelectedElementData(list, focusedSelectable)
        if (selectedElementNarrationData != null) {
            if (selectedElementNarrationData.selectType.isFocused) {
                focusedSelectable = selectedElementNarrationData.selectable
            }
            if (list.size > 1) {
                builder.put(NarrationPart.POSITION, Text.translatable("narrator.position.object_list", selectedElementNarrationData.index + 1, list.size))
                if (selectedElementNarrationData.selectType.isFocused) {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.component_list.usage"))
                }
            }
            selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage())
        }
    }

    /**
     * API for PopupWidgets. Add, remove, and interact with popups here.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    companion object Api {
        /**
         * Sets a [PopupWidget] to the current screen, if the current screen is a [me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen]
         * @param popup [PopupWidget] or null. If null, the widget will be cleared, otherwise the current widget will be set to the passed one.
         * @author fzzyhmstrs
         * @since 0.2.0, added mouse overloads 0.6.0
         */
        @JvmOverloads
        fun push(popup: PopupWidget?, mouseX: Double? = null, mouseY: Double? = null) {
            MinecraftClient.getInstance().currentScreen?.nullCast<PopupParentElement>()?.setPopup(popup, mouseX, mouseY)
        }

        /**
         * Removes the top widget from the current [me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen] widget stack, if any
         *
         * The closed widget will have its [PopupWidget.onClose] method called
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun pop() {
            push(null)
        }

        /**
         * Removes the top widget from the current [me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen] widget stack, if any
         *
         * The closed widget will have its [PopupWidget.onClose] method called
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun pop(mouseX: Double, mouseY: Double) {
            push(null, mouseX, mouseY)
        }

        /**
         * Provides an element for the current popup widget to focus on.
         *
         * Must be an existing child of the [PopupWidget] for focusing to succeed
         * @param element [Element] the element to focus on
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun focusElement(element: Element) {
            (MinecraftClient.getInstance().currentScreen as? PopupWidgetScreen)?.popupWidgets?.peek()?.trySetFocused(element)
        }

        init {
            RenderUtil.addBackground("widget/popup/background".fcId(), RenderUtil.Background(4, 4, 64, 64))
        }
    }

    /**
     * Builds a [PopupWidget] from provided Elements and layout
     *
     * All popups will come with a title widget Top Center of the layout. The next added element will key off this, or to manually layout off of it, use "title" as the element parent
     * @param title [Text] - the header title shown at the top of the popup
     * @param spacingW Int, optional - Defines the default horizontal padding between elements. Defaults to 4
     * @param spacingH Int, optional - Defines the default vertical padding between elements. Defaults to using the value from spacingW
     * @sample me.fzzyhmstrs.fzzy_config.examples.PopupWidgetExamples.popupExample
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Suppress("DEPRECATION", "UNUSED")
    //client
    class Builder @JvmOverloads constructor(private val title: Text, spacingW: Int = 4, spacingH: Int = spacingW) {

        private val layoutWidget = LayoutWidget(spacingW = spacingW, spacingH = spacingH)
        private var positionX: BiFunction<Int, Int, Int> = BiFunction { sw, w -> sw/2 - w/2 }
        private var positionY: BiFunction<Int, Int, Int> = BiFunction { sw, w -> sw/2 - w/2 }

        private var onClose = Runnable { }
        private var onClick: MouseClickResult = MouseClickResult { _, _, _, _ -> ClickResult.USE }
        private var blurBackground = true
        private var closeOnOutOfBounds = true
        private var background = "widget/popup/background".fcId()
        private var additionalTitleNarration: MutableList<Text> = mutableListOf()

        private val titleElement: TextWidget

        init {
            val hh = if(spacingH < 4)
                MinecraftClient.getInstance().textRenderer.fontHeight + ((4 - spacingH) * 2)
            else
                MinecraftClient.getInstance().textRenderer.fontHeight
            val tw = TextWidget(MinecraftClient.getInstance().textRenderer.getWidth(title), hh, title, MinecraftClient.getInstance().textRenderer)
            titleElement = tw

            layoutWidget.add("title", tw, LayoutWidget.Position.ALIGN_CENTER)
        }

        /**
         * Adds an element with custom vertical and horizontal padding, keyed off a manually defined parent element.
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param parent String - the id of the parent to key layout of this new element off of.
         * @param spacingW Int - the custom horizontal padding
         * @param spacingH Int - the custom vertical padding
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
         */
        @Deprecated("Use 'add' and 'push/popSpacing' instead")
        fun <E> addElementSpacedBoth(id: String, element: E, parent: String, spacingW: Int, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            layoutWidget.pushSpacing({ _ -> spacingW }, { _ -> spacingH })
            layoutWidget.add(id, element, parent, *positions)
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Adds an element with custom horizontal padding, keyed off a manually defined parent element.
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param parent String - the id of the parent to key layout of this new element off of.
         * @param spacingW Int - the custom horizontal padding
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
         */
        @Deprecated("Use 'add' and 'push/popSpacing' instead")
        fun <E> addElementSpacedW(id: String, element: E, parent: String, spacingW: Int, vararg positions: Position): Builder where E: Widget {
            layoutWidget.pushSpacing({ _ -> spacingW }, UnaryOperator.identity())
            layoutWidget.add(id, element, parent, *positions)
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Adds an element with custom vertical padding, keyed off a manually defined parent element.
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param parent String - the id of the parent to key layout of this new element off of.
         * @param spacingH Int - the custom vertical padding
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
         */
        @Deprecated("Use 'add' and 'push/popSpacing' instead")
        fun <E> addElementSpacedH(id: String, element: E, parent: String, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            layoutWidget.pushSpacing(UnaryOperator.identity()) { _ -> spacingH }
            layoutWidget.add(id, element, parent, *positions)
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Adds an element with custom vertical and horizontal padding, automatically keyed off the last added element (or "title" if this is the first added element)
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param spacingW Int - the custom horizontal padding
         * @param spacingH Int - the custom vertical padding
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
         */
        @Deprecated("Use 'add' and 'push/popSpacing' instead")
        fun <E> addElementSpacedBoth(id: String, element: E, spacingW: Int, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            layoutWidget.pushSpacing({ _ -> spacingW }, { _ -> spacingH })
            layoutWidget.add(id, element, *positions)
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Adds an element with custom horizontal padding, automatically keyed off the last added element (or "title" if this is the first added element)
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param spacingW Int - the custom horizontal padding
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
         */
        @Deprecated("Use 'add' and 'push/popSpacing' instead")
        fun <E> addElementSpacedW(id: String, element: E, spacingW: Int, vararg positions: Position): Builder where E: Widget {
            layoutWidget.pushSpacing({ _ -> spacingW }, UnaryOperator.identity())
            layoutWidget.add(id, element, *positions)
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Adds an element with custom vertical padding, automatically keyed off the last added element (or "title" if this is the first added element)
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param spacingH Int - the custom vertical padding
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
         */
        @Deprecated("Use 'add' and 'push/popSpacing' instead")
        fun <E> addElementSpacedH(id: String, element: E, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            layoutWidget.pushSpacing(UnaryOperator.identity()) { _ -> spacingH }
            layoutWidget.add(id, element, *positions)
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Adds an element, keyed off a manually defined parent element. Uses the default padding.
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param parent String - the id of the parent to key layout of this new element off of.
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Deprecated("Use 'add' instead")
        fun <E> addElement(id: String, element: E, parent: String, vararg positions: Position): Builder where E: Widget {
            layoutWidget.add(id, element, parent, *positions)
            return this
        }
        /**
         * Adds an element, automatically keyed off the last added element (or "title" if this is the first added element). Uses the default padding.
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Deprecated("Use 'add' instead")
        fun <E> addElement(id: String, element: E, vararg positions: Position): Builder where E: Widget {
            layoutWidget.add(id, element, *positions)
            return this
        }
        /**
         * Adds an element, keyed off a manually defined parent element. Uses the default padding.
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param parent String - the id of the parent to key layout of this new element off of.
         * @param positions vararg [LayoutWidget.Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun <E> add(id: String, element: E, parent: String, vararg positions: LayoutWidget.Position): Builder where E: Widget {
            layoutWidget.add(id, element, parent, *positions)
            return this
        }
        /**
         * Adds an element, automatically keyed off the last added element (or "title" if this is the first added element). Uses the default padding.
         *
         * NOTE: "element" here refers to a piece of a PopupWidget layout. "Elements" do NOT necessarily have to be minecraft [Element]
         * @param E - Any subclass of [Widget]
         * @param id String - the id of this element, used when an element refers to this one as a parent
         * @param element E - the widget
         * @param positions vararg [LayoutWidget.Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun <E> add(id: String, element: E, vararg positions: LayoutWidget.Position): Builder where E: Widget {
            layoutWidget.add(id, element, *positions)
            return this
        }

        /**
         * Push a custom element spacing to this widgets spacing stack. any elements added after this push will be spaced using the top h/w spacing on that stack, or the default spacing provided in the widget constructor if no custom spacing exists on the stack
         * @param w [UnaryOperator] that passes the current horizontal spacing (top of the stack) and returns what the new spacing should be
         * @param h [UnaryOperator] that passes the current vertival spacing (top of the stack) and returns what the new spacing should be
         * @return this widget
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun pushSpacing(w: UnaryOperator<Int>, h: UnaryOperator<Int>): Builder {
            layoutWidget.pushSpacing(w, h)
            return this
        }

        /**
         * Pops a set of custom spacing off this widgets spacing stack. If all custom spacings are popped, will revert to the default spacing provided in the constructor
         * @return this widget
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun popSpacing(): Builder {
            layoutWidget.popSpacing()
            return this
        }

        /**
         * Adds a horizontal divider below the last element, or defined parent
         *
         * The divider automatically uses the layout BELOW, ALIGN_JUSTIFY_WEAK
         * @param parent String?, optional - default value is null. If parent isn't null, the divider will be keyed off the defined parent
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmOverloads
        fun addDivider(parent: String? = null): Builder {
            val trueParent = parent ?: layoutWidget.lastElement()
            add("divider_for_$trueParent", DividerWidget(10), trueParent, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY_WEAK)
            return this
        }
        /**
         * Adds a "Done" button below the previously added element, or below the defined parent
         *
         * The button automatically uses the layout BELOW, ALIGN_JUSTIFY_WEAK
         * @param pressAction [ButtonWidget.PressAction] - defines the buttons action when clicked
         * @param parent String, optional. defines the parent element for this button. by default (null), will be the previously added element.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Deprecated("Use addDoneWidget instead")
        @JvmOverloads
        fun addDoneButton(pressAction: ButtonWidget.PressAction = ButtonWidget.PressAction{ pop() }, parent: String? = null, spacingH: Int = 4): Builder {
            val bw = ButtonWidget.builder(ScreenTexts.DONE, pressAction).build()
            val trueParent = parent ?: layoutWidget.lastElement()
            layoutWidget.pushSpacing(UnaryOperator.identity()) { _ -> spacingH }
            add("done_for_$trueParent",
            CustomButtonWidget.builder(ScreenTexts.DONE) { cbw ->
                bw.width = cbw.width
                bw.setPosition(cbw.x, cbw.y)
                bw.tooltip = cbw.tooltip
                bw.message = cbw.message
                pressAction.onPress(bw)
                cbw.tooltip = bw.tooltip
                cbw.message = bw.message
                cbw.width = bw.width
                cbw.setPosition(bw.x, bw.y) }
                .size(50, 20)
                .build(),
                trueParent,
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_JUSTIFY_WEAK)
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Adds a "Done" button below the previously added element, or below the defined parent
         *
         * The button automatically uses the layout BELOW, ALIGN_JUSTIFY_WEAK
         * @param pressAction [ButtonWidget.PressAction] - defines the buttons action when clicked
         * @param parent String, optional. defines the parent element for this button. by default (null), will be the previously added element.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmOverloads
        fun addDoneWidget(pressAction: Consumer<CustomButtonWidget> = Consumer { pop() }, parent: String? = null, spacingH: Int = 4): Builder {
            val trueParent = parent ?: layoutWidget.lastElement()
            layoutWidget.pushSpacing(UnaryOperator.identity()) { _ -> spacingH }
            add(
                "done_for_$trueParent",
                CustomButtonWidget.builder(ScreenTexts.DONE, pressAction).size(50, 20).build(),
                trueParent,
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_JUSTIFY_WEAK
            )
            layoutWidget.popSpacing()
            return this
        }
        /**
         * Defines a manual width for the widget. Will override any automatic sizing computations for width
         * @param width Int - the manual width of the Popup
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun width(width: Int): Builder {
            layoutWidget.setWidthQuiet(width)
            return this
        }
        /**
         * Defines a manual content width for the widget, adding padding on each side for the popup borders. Will override any automatic sizing computations for width
         * @param width Int - the manual width of the Popup
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        fun contentWidth(width: Int): Builder {
            layoutWidget.setWidthQuiet(width + layoutWidget.getGeneralHorizontalPadding() * 2)
            return this
        }
        /**
         * Defines a manual height for the widget. Will override any automatic sizing computations for height
         * @param height Int - the manual height of the Popup
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun height(height: Int): Builder {
            layoutWidget.setHeightQuiet(height)
            return this
        }
        /**
         * Defines the X positioner function for this widget, X being the left edge of the widget, border included.
         *
         * The default position function centers the widget horizontally on the screen
         * @param positionX [BiFunction]<Int, Int, Int> - The X position BiFunction: (Screen Width, Popup Width) -> X position, globally on the screen
         * @return Builder - this builder for further use
         * @see at
         * @see popupContext
         * @see screenContext
         * @see center
         * @see centerOffset
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun positionX(positionX: BiFunction<Int, Int, Int>): Builder {
            this.positionX = positionX
            return this
        }
        /**
         * Defines the Y positioner function for this widget, Y being the top edge of the widget, border included.
         *
         * The default position function centers the widget vertically on the screen
         * @param positionY [BiFunction]<Int, Int, Int> - The Y position BiFunction: (Screen Height, Popup Height) -> Y position, globally on the screen
         * @return Builder - this builder for further use
         * @see at
         * @see popupContext
         * @see screenContext
         * @see center
         * @see centerOffset
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun positionY(positionY: BiFunction<Int, Int, Int>): Builder {
            this.positionY = positionY
            return this
        }

        /**
         * Defines an action to perform when this widget is closed
         * @param onClose [Runnable] - the action to be performed
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun onClose(onClose: Runnable): Builder {
            this.onClose = onClose
            return this
        }

        /**
         * Defines an action to perform when this widget is clicked on. This will run *before* any click actions of child elements, and will not run at all if a suggestion window is open.
         * @param onClick [Runnable] - the action to be performed
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun onClick(onClick: MouseClickResult): Builder {
            this.onClick = onClick
            return this
        }

        /**
         * The widget won't apply a layer of blur behind it when rendering.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun noBlur(): Builder {
            this.blurBackground = false
            return this
        }
        /**
         * The widget won't close if a click misses its bounding box. Normal behavior closes the popup on a missed click.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun noCloseOnClick(): Builder {
            this.closeOnOutOfBounds = false
            return this
        }
        /**
         * Defines a custom background texture for the popup. Should be a Nine Slice texture
         *
         * NOTE: The border padding on a Popup is 8, inclusive of visual border and any "blank space" between the border and edges of elements.
         * @param id Identifier - the sprite identifier of the custom background
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun background(id: Identifier): Builder {
            this.background = id
            return this
        }
        /**
         * Appends a custom narration message to the end of the Popup title.
         *
         * This narration will be in the TITLE narration part, so it will narrate directly after the main title, and before any other narration such as USAGE, HINT, etc. There is a pause in narration between the title and each appended message
         * @param message [Text] - the massage to be appended
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun additionalNarration(message: Text): Builder {
            additionalTitleNarration.add(message)
            return this
        }

        /**
         * Builds this builder
         * @return [PopupWidget] - the built widget
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun build(): PopupWidget {

            val narratedTitle = title.copy()
            for (additional in additionalTitleNarration) {
                narratedTitle.append(", ".lit()).append(additional)
            }

            val positioner: BiConsumer<Int, Int> = BiConsumer { x, y ->
                layoutWidget.setPosition(x, y)
            }
            layoutWidget.compute(true)
            val children: MutableList<Element> = mutableListOf()
            val selectables: MutableList<Selectable> = mutableListOf()
            val drawables: MutableList<Drawable> = mutableListOf()
            layoutWidget.categorize(children, drawables, selectables) { el ->
                if (el is AbstractTextWidget) {
                    val msg = el.message
                    if (msg != title) {
                        narratedTitle.append(", ".lit()).append(msg)
                    }
                }
            }

            return PopupWidget(narratedTitle, layoutWidget.width, layoutWidget.height, blurBackground, closeOnOutOfBounds, background, positionX, positionY, positioner, onClose, onClick, children, selectables, drawables)
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Default position BiFunctions that can be used with [positionX] and [positionY]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        //client
        companion object Positioners {
            /**
             * Positions a Popup dimension at an absolute location
             *
             * The position will not change on resize or other events, so use wisely.
             * @param a Int - the position to apply
             * @return BiFunction<Int, Int, Int> - the positioner function to apply to positionX or positionY in the Builder
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun abs(a: Int): BiFunction<Int, Int, Int> {
                return BiFunction { _, _ -> a }
            }

            /**
             * Positions a Popup dimension at a specific location
             *
             * The position will be bound to the screen dimensions automatically (the popup won't overflow "off-screen")
             * @param a Supplier<Int> - the position to apply
             * @return BiFunction<Int, Int, Int> - the positioner function to apply to positionX or positionY in the Builder
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun at(a: Supplier<Int>): BiFunction<Int, Int, Int> {
                return BiFunction { sd, d -> max(min(sd - d, a.get()), 0) }
            }

            /**
             * Positions a Popup dimension based on Popup dimension context
             *
             * The position will be bound to the screen dimensions automatically (the popup won't overflow "off-screen")
             * @param f Function<Int, Int> - function to provide the dimension. Supplies the corresponding widget size in the relevant dimension (PositionX -> widget width, etc)
             * @return BiFunction<Int, Int, Int> - the positioner function to apply to positionX or positionY in the Builder
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun popupContext(f: Function<Int, Int>): BiFunction<Int, Int, Int> {
                return BiFunction { sd, d -> max(min(sd - d, f.apply(d)), 0) }
            }
            /**
             * Positions a Popup dimension at an absolute location, bounded by the screen
             *
             * The position will not change on resize or other events, so use wisely.
             * @param a Int - the position to apply
             * @return BiFunction<Int, Int, Int> - the positioner function to apply to positionX or positionY in the Builder
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            fun absScreen(a: Int): BiFunction<Int, Int, Int> {
                return BiFunction { sd, d -> max(min(sd - d, a), 0) }
            }
            /**
             * Positions a Popup dimension based on screen dimension context
             *
             * The position will be bound to the screen dimensions automatically (the popup won't overflow "off-screen")
             * @param f Function<Int, Int> - function to provide the dimension. Supplies the corresponding screen size in the relevant dimension (PositionX -> screen width, etc)
             * @return BiFunction<Int, Int, Int> - the positioner function to apply to positionX or positionY in the Builder
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun screenContext(f: Function<Int, Int>): BiFunction<Int, Int, Int> {
                return BiFunction { sd, d -> max(min(sd - d, f.apply(sd)), 0) }
            }
            /**
             * Positions a Popup in the center of the screen
             *
             * This is the default behavior of the Builder, so typically won't be needed separately.
             * @return BiFunction<Int, Int, Int> - the positioner function to apply to positionX or positionY in the Builder
             * @author fzzyhmstrs
             * @since 0.2.0
             */

            fun center(): BiFunction<Int, Int, Int> {
                return BiFunction { sd, d -> sd/2 - d/2 }
            }
            /**
             * Positions a Popup in the center of the screen, offset by the provided amount
             *
             * The position will be bound to the screen dimensions automatically (the popup won't overflow "off-screen")
             * @param o Supplier<Int> - the position offset to apply
             * @return BiFunction<Int, Int, Int> - the positioner function to apply to positionX or positionY in the Builder
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun centerOffset(o: Supplier<Int>): BiFunction<Int, Int, Int> {
                return BiFunction { sd, d -> max(min(sd - d, (sd/2 - d/2 + o.get())), 0) }
            }
        }

        /**
         * A layout position to apply to a popup element
         *
         * Typical implementation requires at least two positions, a relative position and an alignment
         * Positions are broken down into 3 sub-categories:
         * - [LayoutWidget.PositionRelativePos] - How to generally position an element relative to its parent
         * - [LayoutWidget.PositionRelativeAlignment] - How to align an element in relation to the dimension features of its parent (top, bottom, left, and right edges etc.)
         * - [LayoutWidget.PositionGlobalAlignment] - How to align an element in relation to the global dimensions of the Popup as a whole
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
         */
        //client
        @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
        sealed interface Position: LayoutWidget.Position {
            override fun position(parent: LayoutWidget.LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos>

            /**
             * Collection of all implemented [Position]. Preferred practice is to use this collection rather than referring directly to the underlying Enums
             * @author fzzyhmstrs
             * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
             */
            @Suppress("DEPRECATION", "UNUSED")
            @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
            companion object Impl {
                /**
                 * Positions an element below its parent. Does not define horizontal alignment or positioning.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val BELOW: Position = LayoutWidget.PositionRelativePos.BELOW
                /**
                 * Positions an element to the left of its parent. Does not define vertical alignment or positioning.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val LEFT: Position = LayoutWidget.PositionRelativePos.LEFT
                /**
                 * Positions an element to the right of its parent. Does not define vertical alignment or positioning.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val RIGHT: Position = LayoutWidget.PositionRelativePos.RIGHT
                /**
                 * Aligns an elements top edge horizontally with the top edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val HORIZONTAL_TO_TOP_EDGE: Position = LayoutWidget.PositionRelativeAlignment.HORIZONTAL_TO_TOP_EDGE
                /**
                 * Aligns an elements bottom edge horizontally with the bottom edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val HORIZONTAL_TO_BOTTOM_EDGE: Position = LayoutWidget.PositionRelativeAlignment.HORIZONTAL_TO_BOTTOM_EDGE
                /**
                 * Aligns an elements left edge vertically with the left edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val VERTICAL_TO_LEFT_EDGE: Position = LayoutWidget.PositionRelativeAlignment.VERTICAL_TO_LEFT_EDGE
                /**
                 * Aligns an elements right edge vertically with the right edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val VERTICAL_TO_RIGHT_EDGE: Position = LayoutWidget.PositionRelativeAlignment.VERTICAL_TO_RIGHT_EDGE
                /**
                 * Centers an element vertically relative to the vertical dimensions of its parent (top and bottom edges). Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val CENTERED_VERTICALLY: Position = LayoutWidget.PositionRelativeAlignment.CENTERED_VERTICALLY
                /**
                 * Centers an element horizontally relative to the horizontal dimensions of its parent (left and right edge). Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val CENTERED_HORIZONTALLY: Position = LayoutWidget.PositionRelativeAlignment.CENTERED_HORIZONTALLY
                /**
                 * Aligns an element to the left side of the Popup widget. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val ALIGN_LEFT: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT
                /**
                 * Aligns an element to the right side of the Popup widget. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val ALIGN_RIGHT: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT
                /**
                 * Centers an element relative to the width of the Popup widget. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val ALIGN_CENTER: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_CENTER
                /**
                 * Centers an element relative to the width of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
                 *
                 * Justification of this element won't take any overlapping elemnts into consideration, it will justify to the global left and right edges of the Popup regardless.
                 *
                 * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val ALIGN_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_JUSTIFY
                /**
                 * Aligns an element to the left side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
                 *
                 * Justification of this element WILL take elements to the right of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
                 *
                 * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val ALIGN_LEFT_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_AND_JUSTIFY
                /**
                 * Aligns an element to the right side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
                 *
                 * Justification of this element WILL take elements to the left of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
                 *
                 * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
                 * @author fzzyhmstrs
                 * @since 0.2.0, deprecated 0.6.0 & scheduled for removal 0.7.0
                 */
                @Deprecated("Moved to LayoutWidget in 0.6.0, scheduled for removal 0.7.0")
                val ALIGN_RIGHT_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY
            }
        }

        //client
        sealed interface PositionAlignment: Position

        @Internal
        //client
        data class PosSet(val x: Pos, val y: Pos, val w: Pos, val h: Pos, val spacingW: Int, val spacingH: Int)
    }


    /**
     * Applies a click action from provided inputs and returns a [ClickResult]. Used to perform an action in a Popup besides actions laid-out widgets may trigger, such as a special action if a click misses the popup.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @FunctionalInterface
    fun interface MouseClickResult {

        /**
         * Apply a click and return a result.
         * @param mouseX Double - x position of the mouse click
         * @param mouseY Double - y position of the mouse click
         * @param isMouseOver whether the click is over this popup or missing it
         * @param button Int code of the button click
         * @return [ClickResult] for the action.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun onClick(mouseX: Double, mouseY: Double, isMouseOver: Boolean, button: Int): ClickResult
    }

    /**
     * A result of a click action.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    enum class ClickResult {

        /**
         * Indicates that the action is "passing" the click to the screen underneath rather than using it on the popup. This should typically not be returned if isMouseOver is true in [MouseClickResult.onClick]
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        PASS,
        /**
         * Indicates that the action has been "consumed" by the click, the screen will do nothing. The default return
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        USE
    }
}