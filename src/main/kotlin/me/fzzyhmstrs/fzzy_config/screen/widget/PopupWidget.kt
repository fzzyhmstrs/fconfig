package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.pos.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.*
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigation.Arrow
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.navigation.NavigationDirection
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

@Environment(EnvType.CLIENT)
open class PopupWidget
    private constructor(
        private var message: Text,
        private val width: Int,
        private val height: Int,
        private val closeOnOutOfBounds: Boolean,
        private val background: Identifier,
        private val positionX: BiFunction<Int,Int,Int>,
        private val positionY: BiFunction<Int,Int,Int>,
        private val positioner: BiConsumer<Int,Int>,
        private val onClose: Runnable,
        private val children: List<Element>,
        private val selectables: List<Selectable>,
        private val drawables: List<Drawable>)
    :
    ParentElement,
    Narratable,
    Drawable
{

    private var x: Int = 0
    private var y: Int = 0
    private var focused: Element? = null
    private var focusedSelectable: Selectable? = null
    private var dragging = false

    open fun onClose(){
        this.onClose.run()
    }

    fun closesOnMissedClick(): Boolean{
        return closeOnOutOfBounds
    }

    open fun blur() {
        val guiNavigationPath = this.focusedPath
        guiNavigationPath?.setFocused(false)
    }

    fun position(screenWidth: Int, screenHeight: Int){
        this.x = positionX.apply(screenWidth,width) //screenWidth/2 - width/2
        this.y = positionY.apply(screenHeight,height) //screenHeight/2 - height/2
        positioner.accept(this.x, this.y)
    }

    override fun children(): List<Element> {
        return children
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        RenderSystem.enableBlend()
        context.drawGuiTexture(background, x, y, width, height)
        for (drawable in drawables) {
            RenderSystem.disableDepthTest()
            drawable.render(context, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super.mouseClicked(mouseX, mouseY, button).takeIf { it } ?: isMouseOver(mouseX, mouseY)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x.toDouble() && mouseY >= y.toDouble() && mouseX < (x + width).toDouble() && mouseY < (y + height).toDouble()
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        println(deltaX)
        println(deltaY)
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true
        }
        val guiNavigation: GuiNavigation? = when(keyCode){
            GLFW.GLFW_KEY_LEFT -> getArrowNavigation(NavigationDirection.LEFT)
            GLFW.GLFW_KEY_RIGHT -> getArrowNavigation(NavigationDirection.RIGHT)
            GLFW.GLFW_KEY_UP -> getArrowNavigation(NavigationDirection.UP)
            GLFW.GLFW_KEY_DOWN -> getArrowNavigation(NavigationDirection.DOWN)
            GLFW.GLFW_KEY_TAB ->  getTabNavigation()
            else -> null
        }
        if(guiNavigation != null){
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
    open fun switchFocus(path: GuiNavigationPath) {
        blur()
        path.setFocused(true)
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

    companion object Api{
        /**
         * Sets a [PopupWidget] to the current screen, if the current screen is a [me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen]
         *
         * If a popup is already displayed, [PopupWidget.onClose] will be called on it before the new value is input.
         * @param popup [PopupWidget] or null. If null, the widget will be cleared, otherwise the current widget will be set to the passed one.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun setPopup(popup: PopupWidget?) {
            (MinecraftClient.getInstance().currentScreen as? PopupWidgetScreen)?.setPopup(popup)
        }
    }

    @Suppress("DEPRECATION","UNUSED")
    @Environment(EnvType.CLIENT)
    class Builder(private val title: Text, spacingW: Int = 4, spacingH: Int = 4) {

        private var width: Int = MinecraftClient.getInstance().textRenderer.getWidth(title) + 16
        private var height: Int = 21
        private var positionX: BiFunction<Int,Int,Int> = BiFunction { sw, w -> sw/2 - w/2 }
        private var positionY: BiFunction<Int,Int,Int> = BiFunction { sw, w -> sw/2 - w/2 }
        private var onClose = Runnable { }
        private var closeOnOutOfBounds = true
        private var background = "widget/popup/background".fcId()
        private var additionalTitleNarration: MutableList<Text> = mutableListOf()

        private val xPos = RelPos(ImmutablePos(8),0)
        private val yPos = RelPos(ImmutablePos(8),0)
        private val wPos = RelPos(xPos,width - 16)
        private val hPos = RelPos(yPos,height - 16)
        private val set = PosSet(xPos,yPos,wPos,hPos,spacingW,spacingH)

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

        private fun createInitialElement(): PositionedElement<TextWidget>{
            val widget = TextWidget(title,MinecraftClient.getInstance().textRenderer)
            if(set.spacingH < 4)
                widget.height = widget.height + ((4 - set.spacingH) * 2)
            val posX = SuppliedPos(xPos,0) { (wPos.get() - xPos.get()) / 2 - widget.width / 2 }
            val posY = RelPos(yPos, 0)
            return PositionedElement(widget,posX,posY,Position.ALIGN_CENTER)
        }

        private fun<E> createPositionedElement(set: PosSet, el: E, parent: String, positions: Array<out Position>): PositionedElement<E> where E: Widget{
            var newX: Pos = RelPos(set.x,set.spacingW)
            var newY: Pos = RelPos(set.y,set.spacingH)
            val parentEl = elements[parent] ?: titleElement
            var alignment: PositionGlobalAlignment = parentEl.alignment
            for(pos in positions){
                val pair = pos.position(parentEl,el,set,newX,newY)
                newX = pair.first
                newY = pair.second
                if (pos is PositionGlobalAlignment){
                    alignment = pos
                }
            }
            return PositionedElement(el, newX, newY, alignment)
        }

        private fun attemptRecomputeDims(){
            var maxW = 0
            var maxH = 0
            for ((name,posEl) in elements){
                println("[$name, ${posEl.x}, ${posEl.elWidth()}]")
                maxW = (posEl.getRight() + 8 - ((posEl.getLeft() - 8).takeIf { it < 0 } ?: 0)).takeIf { it > maxW } ?: maxW //6 = outer edge padding
                maxH = (posEl.getBottom() + 8).takeIf { it > maxH } ?: maxH //6 = outer edge padding
                println(maxW)
                println(maxH)
            }
            updateWidth(maxW)
            updateHeight(maxH)
        }

        private var lastEl = "title"

        fun <E> addElementSpacedBoth(id: String, element: E, parent: String, spacingW: Int, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW, spacingH = spacingH), element, parent, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        fun <E> addElementSpacedW(id: String, element: E, parent: String, spacingW: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW), element, parent, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        fun <E> addElementSpacedH(id: String, element: E, parent: String, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingH = spacingH), element, parent, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        fun <E> addElementSpacedBoth(id: String, element: E, spacingW: Int, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW, spacingH = spacingH), element, lastEl, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        fun <E> addElementSpacedW(id: String, element: E, spacingW: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW), element, lastEl, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        fun <E> addElementSpacedH(id: String, element: E, spacingH: Int, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set.copy(spacingH = spacingH), element, lastEl, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        fun <E> addElement(id: String, element: E, parent: String, vararg positions: Position): Builder where E: Widget {
            val posEl = createPositionedElement(set, element, parent, positions)
            elements[id] = posEl
            lastEl = id
            return this
        }
        fun <E> addElement(id: String, element: E, vararg positions: Position): Builder where E: Widget {
            return addElement(id, element, lastEl, *positions)
        }

        @JvmOverloads
        fun addDivider(parent: String? = null): Builder{
            val trueParent = parent ?: lastEl
            addElement("divider_for_$trueParent", DividerWidget(10), trueParent, Position.BELOW, Position.ALIGN_JUSTIFY)
            return this
        }

        fun width(width: Int): Builder{
            this.width = width
            return this
        }
        fun height(height: Int): Builder{
            this.height = height
            return this
        }

        fun positionX(positionX: BiFunction<Int,Int,Int>): Builder{
            this.positionX = positionX
            return this
        }
        fun positionY(positionY: BiFunction<Int,Int,Int>): Builder{
            this.positionY = positionY
            return this
        }

        fun onClose(onClose: Runnable): Builder{
            this.onClose = onClose
            return this
        }

        fun noCloseOnClick(): Builder{
            this.closeOnOutOfBounds = false
            return this
        }

        fun background(id: Identifier): Builder{
            this.background = id
            return this
        }

        fun additionalNarration(message: Text){
            additionalTitleNarration.add(message)
        }

        fun build(): PopupWidget {
            attemptRecomputeDims()
            attemptRecomputeDims() // we'll do two passes to try to cover weird cases where first pass doesn't cover everything
            val children: MutableList<Element> = mutableListOf()
            val drawables: MutableList<Drawable> = mutableListOf()
            val selectables: MutableList<Selectable> = mutableListOf()
            val narratedTitle = title.copy()
            for (additional in additionalTitleNarration){
                narratedTitle.append(", ".lit()).append(additional)
            }
            for ((name,posEl) in elements){
                if(posEl.element is Element)
                    children.add(posEl.element)
                if(posEl.element is Drawable)
                    drawables.add(posEl.element)
                if(posEl.element is Selectable)
                    selectables.add(posEl.element)
                if(posEl.element is TextWidget && name != "title")
                    narratedTitle.append(". ".lit()).append(posEl.element.message)
            }
            for (posEl in elements.values){
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
                            if(posElRight.getLeft() < rightPos){
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
                            if(posElLeft.getRight() > leftPos){
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
                for (posEl in elements.values){
                    posEl.update()
                }
            }


            return PopupWidget(narratedTitle, width, height, closeOnOutOfBounds, background, positionX, positionY, positioner, onClose, children, selectables, drawables)
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Environment(EnvType.CLIENT)
        companion object {

            fun at(a: Supplier<Int>): BiFunction<Int,Int,Int>{
                return BiFunction { sd, d -> max(min(sd - d, a.get()),0) }
            }
            fun popupContext(f: Function<Int, Int>): BiFunction<Int,Int,Int>{
                return BiFunction { sd, d -> max(min(sd - d, f.apply(d)),0) }
            }
            fun screenContext(f: Function<Int, Int>): BiFunction<Int,Int,Int>{
                return BiFunction { sd, d -> max(min(sd - d, f.apply(sd)),0) }
            }
            fun center(): BiFunction<Int,Int,Int> {
                return BiFunction { sd, d -> sd/2 - d/2 }
            }
        }

        @Environment(EnvType.CLIENT)
        sealed interface Position {
            fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos,Pos>

            @Suppress("DEPRECATION","UNUSED")
            companion object Impl {
                val BELOW = PositionRelativePos.BELOW
                val LEFT = PositionRelativePos.LEFT
                val RIGHT = PositionRelativePos.RIGHT
                val HORIZONTAL_TO_TOP_EDGE = PositionRelativeAlignment.HORIZONTAL_TO_TOP_EDGE
                val HORIZONTAL_TO_BOTTOM_EDGE = PositionRelativeAlignment.HORIZONTAL_TO_BOTTOM_EDGE
                val VERTICAL_TO_LEFT_EDGE = PositionRelativeAlignment.VERTICAL_TO_LEFT_EDGE
                val VERTICAL_TO_RIGHT_EDGE = PositionRelativeAlignment.VERTICAL_TO_RIGHT_EDGE
                val CENTERED_VERTICALLY = PositionRelativeAlignment.CENTERED_VERTICALLY
                val CENTERED_HORIZONTALLY = PositionRelativeAlignment.CENTERED_HORIZONTALLY
                val ALIGN_LEFT = PositionGlobalAlignment.ALIGN_LEFT
                val ALIGN_RIGHT = PositionGlobalAlignment.ALIGN_RIGHT
                val ALIGN_CENTER = PositionGlobalAlignment.ALIGN_CENTER
                val ALIGN_JUSTIFY = PositionGlobalAlignment.ALIGN_JUSTIFY
                val ALIGN_LEFT_AND_JUSTIFY = PositionGlobalAlignment.ALIGN_LEFT_AND_JUSTIFY
                val ALIGN_RIGHT_AND_JUSTIFY = PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY
            }
        }

        @Environment(EnvType.CLIENT)
        sealed interface PositionAlignment: Position

        @Environment(EnvType.CLIENT)
        enum class PositionRelativePos: Position {
            @Deprecated("Use Positions Impl values")
            BELOW {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y,globalSet.spacingH + parent.elHeight()))
                }
            },
            @Deprecated("Use Positions Impl values")
            LEFT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x,-el.width - globalSet.spacingW),prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            RIGHT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x,parent.elWidth() + globalSet.spacingW),prevY)
                }
            }
        }

        @Environment(EnvType.CLIENT)
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

        @Environment(EnvType.CLIENT)
        enum class PositionGlobalAlignment: PositionAlignment {
            @Deprecated("Use Positions Impl values")
            ALIGN_LEFT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(globalSet.x), prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_RIGHT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.w,0) {-el.width}, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_CENTER {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.x,0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_JUSTIFY{
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.x,0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_LEFT_AND_JUSTIFY{
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(globalSet.x), prevY)
                }
            },
            @Deprecated("Use Positions Impl values")
            ALIGN_RIGHT_AND_JUSTIFY{
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.w,0) {-el.width}, prevY)
                }
            }
        }

        @Internal
        @Suppress("UNUSED")
        @Environment(EnvType.CLIENT)
        class PositionedElement<T>(val element: T, var x: Pos, var y: Pos, val alignment: PositionGlobalAlignment) where T: Widget{
            private fun upDown(): IntRange{
                return IntRange(getTop(),getBottom())
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
            fun update(){
                element.x = x.get()
                element.y = y.get()
            }
            fun otherIsLeftwards(element: PositionedElement<*>): Boolean{
                return inUpDownBounds(element.upDown()) && element.getRight() <= getLeft()
            }
            fun otherIsRightwards(element: PositionedElement<*>): Boolean{
                return inUpDownBounds(element.upDown()) && element.getLeft() >= getRight()
            }
            private fun inUpDownBounds(chk: IntRange): Boolean{
                return upDown().intersect(chk).isNotEmpty()
            }
        }

        @Internal
        @Environment(EnvType.CLIENT)
        data class PosSet(val x: Pos, val y: Pos, val w: Pos, val h: Pos, val spacingW: Int, val spacingH: Int)
    }
}