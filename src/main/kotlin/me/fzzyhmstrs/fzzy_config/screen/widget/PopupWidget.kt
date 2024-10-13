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
import com.mojang.serialization.Codec
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DividerWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
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
import net.minecraft.util.StringIdentifiable
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

/**
 * A widget comprised of a collection of child elements that "Pops up" onto screens that implement [me.fzzyhmstrs.fzzy_config.screen.PopupParentElement]
 *
 * Multiple popups can stack onto PopupParentElements. They display last-added in front, first added in back, and are removed in reverse of the order they were added (First In Last Out)
 * @see Builder
 * @author fzzyhmstrs
 * @since 0.2.0
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
    private val fillColor = Color(30, 30, 30, 90).rgb
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

    fun applyBlur(delta: Float) {
        MinecraftClient.getInstance().gameRenderer.renderBlur()
        MinecraftClient.getInstance().framebuffer.beginWrite(false)
    }

    fun position(screenWidth: Int, screenHeight: Int) {
        this.x = positionX.apply(screenWidth, width) //screenWidth/2 - width/2
        this.y = positionY.apply(screenHeight, height) //screenHeight/2 - height/2
        positioner.accept(this.x, this.y)
    }

    override fun children(): List<Element> {
        return children
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.matrices.push()
        context.matrices.translate(x.toFloat(), y.toFloat(), 0f)
        if (blurBackground) {
            context.fill(0, 0, width, height, fillColor)
            applyBlur(delta)
        }
        context.matrices.pop()
        RenderSystem.enableBlend()
        RenderSystem.disableDepthTest()
        context.drawTex(background, x, y, width, height)
        for (drawable in drawables) {
            RenderSystem.disableDepthTest()
            drawable.render(context, mouseX, mouseY, delta)
        }

    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return suggestionWindowElement?.mouseClicked(mouseX, mouseY, button)?.takeIf { it } ?: super.mouseClicked(mouseX, mouseY, button).takeIf { it } ?: isMouseOver(mouseX, mouseY)
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return suggestionWindowElement?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
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
         * @since 0.2.0
         */
        fun push(popup: PopupWidget?) {
            (MinecraftClient.getInstance().currentScreen as? PopupWidgetScreen)?.setPopup(popup)
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

        private var width: Int = MinecraftClient.getInstance().textRenderer.getWidth(title) + 16
        private var height: Int = 21
        private var manualWidth: Int = -1
        private var manualHeight: Int = -1
        private var positionX: BiFunction<Int, Int, Int> = BiFunction { sw, w -> sw/2 - w/2 }
        private var positionY: BiFunction<Int, Int, Int> = BiFunction { sw, w -> sw/2 - w/2 }
        private var onClose = Runnable { }
        private var blurBackground = true
        private var closeOnOutOfBounds = true
        private var background = "widget/popup/background".fcId()
        private var additionalTitleNarration: MutableList<Text> = mutableListOf()

        private val xPos = RelPos(ImmutablePos(8), 0)
        private val yPos = RelPos(ImmutablePos(8), 0)
        private val wPos = RelPos(xPos, width - 16)
        private val hPos = RelPos(yPos, height - 16)
        private val set = PosSet(xPos, yPos, wPos, hPos, spacingW, spacingH)

        private val titleElement: PositionedElement<TextWidget> = createInitialElement()
        private val elements: MutableMap<String, PositionedElement<*>> = mutableMapOf(
            "title" to titleElement
        )

        private fun updateWidth(newWidth: Int) {
            width = newWidth
            wPos.set(newWidth - 16)
        }
        private fun updateHeight(newHeight: Int) {
            height = newHeight
            hPos.set(newHeight - 16)
        }

        private fun createInitialElement(): PositionedElement<TextWidget> {
            val widget = TextWidget(title, MinecraftClient.getInstance().textRenderer)
            if(set.spacingH < 4)
                widget.height = widget.height + ((4 - set.spacingH) * 2)
            val posX = SuppliedPos(xPos, 0) { (wPos.get() - xPos.get()) / 2 - widget.width / 2 }
            val posY = RelPos(yPos, 0)
            return PositionedElement(widget, posX, posY, Position.ALIGN_CENTER)
        }

        private fun<E> createPositionedElement(set: PosSet, el: E, parent: String, positions: Array<out Position>): PositionedElement<E> where E: Widget {
            var newX: Pos = RelPos(set.x, set.spacingW)
            var newY: Pos = RelPos(set.y, set.spacingH)
            val parentEl = elements[parent] ?: titleElement
            var alignment: PositionGlobalAlignment = parentEl.alignment
            for(pos in positions) {
                val pair = pos.position(parentEl, el, set, newX, newY)
                newX = pair.first
                newY = pair.second
                if (pos is PositionGlobalAlignment) {
                    alignment = pos
                }
            }
            return PositionedElement(el, newX, newY, alignment)
        }

        private var lastEl = "title"

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
         * @since 0.2.0
         */
        fun <E> addElementSpacedBoth(id: String, element: E, parent: String, spacingW: Int, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW, spacingH = spacingH), element, parent, positions)
            elements[id] = posEl
            lastEl = id
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
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to it's parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <E> addElementSpacedW(id: String, element: E, parent: String, spacingW: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW), element, parent, positions)
            elements[id] = posEl
            lastEl = id
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
         * @param positions vararg [Position] - defines the layout arrangement of this element compared to it's parent. See the doc for Position for details.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <E> addElementSpacedH(id: String, element: E, parent: String, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingH = spacingH), element, parent, positions)
            elements[id] = posEl
            lastEl = id
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
         * @since 0.2.0
         */
        fun <E> addElementSpacedBoth(id: String, element: E, spacingW: Int, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW, spacingH = spacingH), element, lastEl, positions)
            elements[id] = posEl
            lastEl = id
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
         * @since 0.2.0
         */
        fun <E> addElementSpacedW(id: String, element: E, spacingW: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW), element, lastEl, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        /**
         * Adds an element with custom vertical padding, automatically keyed off the last added element (or "title" if this is the first added element)
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
         * @since 0.2.0
         */
        fun <E> addElementSpacedH(id: String, element: E, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingH = spacingH), element, lastEl, positions)
            elements[id] = posEl
            lastEl = id
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
        fun <E> addElement(id: String, element: E, parent: String, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set, element, parent, positions)
            elements[id] = posEl
            lastEl = id
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
        fun <E> addElement(id: String, element: E, vararg positions: Position): Builder where E: Widget {
            return addElement(id, element, lastEl, *positions)
        }

        /**
         * Adds a horizontal divider below the last element, or defined parent
         *
         * The divider automatically uses the layout BELOW, ALIGN_JUSTIFY
         * @param parent String?, optional - default value is null. If parent isn't null, the divider will be keyed off the defined parent
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmOverloads
        fun addDivider(parent: String? = null): Builder {
            val trueParent = parent ?: lastEl
            addElement("divider_for_$trueParent", DividerWidget(10), trueParent, Position.BELOW, Position.ALIGN_JUSTIFY)
            return this
        }
        /**
         * Adds a "Done" button below the previously added element, or below the defined parent
         *
         * The button automatically uses the layout BELOW, ALIGN_JUSTIFY
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
            val trueParent = parent ?: lastEl
            addElementSpacedH(
                "done_for_$trueParent",
                CustomButtonWidget.builder(ScreenTexts.DONE) { cbw ->
                    bw.setDimensions(cbw.width, cbw.height)
                    bw.setPosition(cbw.x, cbw.y)
                    bw.tooltip = cbw.tooltip
                    bw.message = cbw.message
                    pressAction.onPress(bw)
                    cbw.tooltip = bw.tooltip
                    cbw.message = bw.message
                    cbw.setDimensions(bw.width, bw.height)
                    cbw.setPosition(bw.x, bw.y)
                }.size(50, 20).build(),
                trueParent,
                spacingH,
                Position.BELOW,
                Position.ALIGN_JUSTIFY
            )
            return this
        }
        /**
         * Adds a "Done" button below the previously added element, or below the defined parent
         *
         * The button automatically uses the layout BELOW, ALIGN_JUSTIFY
         * @param pressAction [ButtonWidget.PressAction] - defines the buttons action when clicked
         * @param parent String, optional. defines the parent element for this button. by default (null), will be the previously added element.
         * @return Builder - this builder for further use
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmOverloads
        fun addDoneWidget(pressAction: Consumer<CustomButtonWidget> = Consumer { pop() }, parent: String? = null, spacingH: Int = 4): Builder {
            val trueParent = parent ?: lastEl
            addElementSpacedH(
                "done_for_$trueParent",
                CustomButtonWidget.builder(ScreenTexts.DONE, pressAction).size(50, 20).build(),
                trueParent,
                spacingH,
                Position.BELOW,
                Position.ALIGN_JUSTIFY
            )
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
            this.manualWidth = width
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
            this.manualHeight = height
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

        private fun attemptRecomputeDims() {
            if (manualHeight > 0 && manualWidth > 0) {
                updateWidth(manualWidth)
                updateHeight(manualHeight)
                return
            }
            var maxW = 0
            var maxH = 0
            for ((name, posEl) in elements) {
                maxW = (posEl.getRight() + 8 - ((posEl.getLeft() - 8).takeIf { it < 0 } ?: 0)).takeIf { it > maxW } ?: maxW //6 = outer edge padding
                maxH = (posEl.getBottom() + 8).takeIf { it > maxH } ?: maxH //6 = outer edge padding
            }
            if (manualWidth <= 0)
                updateWidth(maxW)
            else
                updateWidth(manualWidth)
            if (manualHeight <= 0)
                updateHeight(maxH)
            else
                updateHeight(manualHeight)
        }

        /**
         * Builds this builder
         * @return [PopupWidget] - the built widget
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun build(): PopupWidget {
            attemptRecomputeDims()
            attemptRecomputeDims() // we'll do two passes to try to cover weird cases where first pass doesn't cover everything
            val children: MutableList<Element> = mutableListOf()
            val drawables: MutableList<Drawable> = mutableListOf()
            val selectables: MutableList<Selectable> = mutableListOf()
            val narratedTitle = title.copy()
            for (additional in additionalTitleNarration) {
                narratedTitle.append(", ".lit()).append(additional)
            }
            for ((name, posEl) in elements) {
                if(posEl.element is Element)
                    children.add(posEl.element)
                if(posEl.element is Drawable)
                    drawables.add(posEl.element)
                if(posEl.element is Selectable)
                    selectables.add(posEl.element)
                if(posEl.element is AbstractTextWidget && name != "title")
                    narratedTitle.append(". ".lit()).append(posEl.element.message)
            }
            for (posEl in elements.values) {
                if (posEl.alignment == Position.ALIGN_JUSTIFY) {
                    if (posEl.element is ClickableWidget) {
                        posEl.element.width = width - 16
                    } else if (posEl.element is Scalable) {
                        posEl.element.setWidth(width - 16)
                    }
                } else if (posEl.alignment == Position.ALIGN_LEFT_AND_JUSTIFY) {
                    var closestRightEl: PositionedElement<*>? = null
                    var rightPos = 1000000000
                    for (posElRight in elements.values) {
                        if (posEl.otherIsRightwards(posElRight)) {
                            if(posElRight.getLeft() < rightPos) {
                                closestRightEl = posElRight
                                rightPos = posElRight.getLeft()
                            }
                        }
                    }
                    if(closestRightEl != null) {
                        if (posEl.element is ClickableWidget) {
                            posEl.element.width = closestRightEl.getLeft() - posEl.getLeft() - set.spacingW
                        } else if (posEl.element is Scalable) {
                            posEl.element.setWidth(closestRightEl.getLeft() - posEl.getLeft() - set.spacingW)
                        }
                    } else {
                        if (posEl.element is ClickableWidget) {
                            posEl.element.width = set.w.get() - posEl.getLeft()
                        } else if (posEl.element is Scalable) {
                            posEl.element.setWidth(set.w.get() - posEl.getLeft())
                        }
                    }
                } else if (posEl.alignment == Position.ALIGN_RIGHT_AND_JUSTIFY) {
                    var closestLeftEl: PositionedElement<*>? = null
                    var leftPos = -1000000000
                    for (posElLeft in elements.values) {
                        if (posEl.otherIsLeftwards(posElLeft)) {
                            if(posElLeft.getRight() > leftPos) {
                                closestLeftEl = posElLeft
                                leftPos = posElLeft.getLeft()
                            }
                        }
                    }
                    if(closestLeftEl != null) {
                        if (posEl.element is ClickableWidget) {
                            posEl.element.width = posEl.getRight() - closestLeftEl.getRight() - set.spacingW
                        } else if (posEl.element is Scalable) {
                            val prevRight = posEl.getRight()
                            posEl.x.dec(posEl.getLeft() - closestLeftEl.getRight())
                            posEl.x.inc(set.spacingW)
                            posEl.element.setWidth(prevRight - posEl.getLeft())
                        }
                    } else {
                        if (posEl.element is ClickableWidget) {
                            val prevRight = posEl.getRight()
                            posEl.x.dec(posEl.getLeft() - set.x.get())
                            posEl.element.width = prevRight - posEl.getLeft()
                        } else if (posEl.element is Scalable) {
                            val prevRight = posEl.getRight()
                            posEl.x.dec(posEl.getLeft() - set.x.get())
                            posEl.element.setWidth(prevRight - posEl.getLeft())
                        }
                    }
                }
            }
            val positioner: BiConsumer<Int, Int> = BiConsumer { x, y ->
                xPos.set(x)
                yPos.set(y)
                for (posEl in elements.values) {
                    posEl.update()
                }
            }
            return PopupWidget(narratedTitle, width, height, blurBackground, closeOnOutOfBounds, background, positionX, positionY, positioner, onClose, children, selectables, drawables)
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
             * Positions a Popup dimention at an absolute location
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
         * - [PositionRelativePos] - How to generally position an element relative to its parent
         * - [PositionRelativeAlignment] - How to align an element in relation to the dimension features of its parent (top, bottom, left, and right edges etc.)
         * - [PositionGlobalAlignment] - How to align an element in relation to the global dimensions of the Popup as a whole
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        //client
        sealed interface Position {
            fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos>

            /**
             * Collection of all implemented [Position]. Preferred practice is to use this collection rather than referring directly to the underlying Enums
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            @Suppress("DEPRECATION", "UNUSED")
            companion object Impl {
                /**
                 * Positions an element below its parent. Does not define horizontal alignment or positioning.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val BELOW = PositionRelativePos.BELOW
                /**
                 * Positions an element to the left of its parent. Does not define vertical alignment or positioning.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val LEFT = PositionRelativePos.LEFT
                /**
                 * Positions an element to the right of its parent. Does not define vertical alignment or positioning.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val RIGHT = PositionRelativePos.RIGHT
                /**
                 * Aligns an elements top edge horizontally with the top edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val HORIZONTAL_TO_TOP_EDGE = PositionRelativeAlignment.HORIZONTAL_TO_TOP_EDGE
                /**
                 * Aligns an elements bottom edge horizontally with the bottom edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val HORIZONTAL_TO_BOTTOM_EDGE = PositionRelativeAlignment.HORIZONTAL_TO_BOTTOM_EDGE
                /**
                 * Aligns an elements left edge vertically with the left edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val VERTICAL_TO_LEFT_EDGE = PositionRelativeAlignment.VERTICAL_TO_LEFT_EDGE
                /**
                 * Aligns an elements right edge vertically with the right edge of its parent. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val VERTICAL_TO_RIGHT_EDGE = PositionRelativeAlignment.VERTICAL_TO_RIGHT_EDGE
                /**
                 * Centers an element vertically relative to the vertical dimensions of its parent (top and bottom edges). Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val CENTERED_VERTICALLY = PositionRelativeAlignment.CENTERED_VERTICALLY
                /**
                 * Centers an element horizontally relative to the horizontal dimensions of its parent (left and right edge). Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val CENTERED_HORIZONTALLY = PositionRelativeAlignment.CENTERED_HORIZONTALLY
                /**
                 * Aligns an element to the left side of the Popup widget. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val ALIGN_LEFT = PositionGlobalAlignment.ALIGN_LEFT
                /**
                 * Aligns an element to the right side of the Popup widget. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val ALIGN_RIGHT = PositionGlobalAlignment.ALIGN_RIGHT
                /**
                 * Centers an element relative to the width of the Popup widget. Does not define any other position or alignment.
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val ALIGN_CENTER = PositionGlobalAlignment.ALIGN_CENTER
                /**
                 * Centers an element relative to the width of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
                 *
                 * Justification of this element won't take any overlapping elemnts into consideration, it will justify to the global left and right edges of the Popup regardless.
                 *
                 * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val ALIGN_JUSTIFY = PositionGlobalAlignment.ALIGN_JUSTIFY
                /**
                 * Aligns an element to the left side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
                 *
                 * Justification of this element WILL take elements to the right of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
                 *
                 * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val ALIGN_LEFT_AND_JUSTIFY = PositionGlobalAlignment.ALIGN_LEFT_AND_JUSTIFY
                /**
                 * Aligns an element to the right side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
                 *
                 * Justification of this element WILL take elements to the left of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
                 *
                 * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                val ALIGN_RIGHT_AND_JUSTIFY = PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY
            }
        }

        //client
        sealed interface PositionAlignment: Position

        //client
        enum class PositionRelativePos: Position {
            @Deprecated("Use Positions Impl values")
            BELOW {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y, globalSet.spacingH + parent.elHeight()))
                }
            },
            @Deprecated("Use Positions Impl values")
            LEFT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x, -el.width - globalSet.spacingW), prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            RIGHT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x, parent.elWidth() + globalSet.spacingW), prevY)
                }
            }
        }

        //client
        enum class PositionRelativeAlignment: PositionAlignment {
            @Deprecated("Use Positions Impl values")
            HORIZONTAL_TO_TOP_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y))
                }
            },
            @Deprecated("Use Positions Impl values")
            HORIZONTAL_TO_BOTTOM_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y, parent.elHeight() - el.height))
                }
            },
            @Deprecated("Use Positions Impl values")
            VERTICAL_TO_LEFT_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x), prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            VERTICAL_TO_RIGHT_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(parent.x, 0){ parent.elWidth() - el.width }, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            CENTERED_HORIZONTALLY {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(parent.x, 0) { parent.elWidth()/2 - el.width/2 }, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            CENTERED_VERTICALLY {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y, parent.elHeight()/2 - el.height/2))
                }
            }
        }

        //client
        enum class PositionGlobalAlignment(private val str: String): PositionAlignment, StringIdentifiable {
            @Deprecated("Use Positions Impl values")
            ALIGN_LEFT("left") {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(globalSet.x), prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_RIGHT("right") {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_CENTER("center") {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_JUSTIFY("justify") {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_LEFT_AND_JUSTIFY("left_justify") {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(globalSet.x), prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_RIGHT_AND_JUSTIFY("right_justify") {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
                }
            };

            override fun asString(): String {
                return str
            }

            companion object {
                val CODEC: Codec<PositionGlobalAlignment> = StringIdentifiable.createCodec { PositionGlobalAlignment.entries.toTypedArray() }
            }
        }

        @Internal
        @Suppress("UNUSED")
        //client
        class PositionedElement<T>(val element: T, var x: Pos, var y: Pos, val alignment: PositionGlobalAlignment) where T: Widget {
            private fun upDown(): IntRange {
                return IntRange(getTop(), getBottom())
            }
            fun getLeft(): Int {
                return x.get()
            }
            fun getRight(): Int {
                return x.get() + element.width
            }
            fun getTop(): Int {
                return y.get()
            }
            fun getBottom(): Int {
                return y.get() + element.height
            }
            fun elWidth(): Int {
                return element.width
            }
            fun elHeight(): Int {
                return element.height
            }
            fun update() {
                element.x = x.get()
                element.y = y.get()
            }
            fun otherIsLeftwards(element: PositionedElement<*>): Boolean {
                return inUpDownBounds(element.upDown()) && element.getRight() <= getLeft()
            }
            fun otherIsRightwards(element: PositionedElement<*>): Boolean {
                return inUpDownBounds(element.upDown()) && element.getLeft() >= getRight()
            }
            private fun inUpDownBounds(chk: IntRange): Boolean {
                return upDown().intersect(chk).isNotEmpty()
            }
        }

        @Internal
        //client
        data class PosSet(val x: Pos, val y: Pos, val w: Pos, val h: Pos, val spacingW: Int, val spacingH: Int)
    }
}