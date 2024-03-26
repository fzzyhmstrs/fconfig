package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.pos.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.*
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.math.min

@Environment(EnvType.CLIENT)
open class PopupWidget
    private constructor(
        private var message: Text,
        private val width: Int,
        private val height: Int,
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

    companion object{
        private val BACKGROUND = "widget/popup/background".fcId()
    }

    private var x: Int = 0
    private var y: Int = 0
    private var focused: Element? = null
    private var focusedSelectable: Selectable? = null
    private var dragging = false

    open fun onClose(){
        this.onClose.run()
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
        context.drawGuiTexture(BACKGROUND, x, y, width, height)
        for (drawable in drawables) {
            drawable.render(context, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super.mouseClicked(mouseX, mouseY, button).takeIf { it } ?: isMouseOver(mouseX, mouseY)
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
                if (selectedElementNarrationData.selectType == Selectable.SelectionType.FOCUSED) {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.component_list.usage"))
                }
            }
            selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage())
        }
    }

    @Environment(EnvType.CLIENT)
    class Builder(private val title: Text, spacingW: Int = 4, spacingH: Int = 4) {

        private var width: Int = MinecraftClient.getInstance().textRenderer.getWidth(title) + 8
        private var height: Int = 17
        private var positionX: BiFunction<Int,Int,Int> = BiFunction { sw, w -> sw/2 - w/2 }
        private var positionY: BiFunction<Int,Int,Int> = BiFunction { sw, w -> sw/2 - w/2 }
        private var onClose = Runnable { }

        private val xPos = RelPos(ImmutablePos(4),0)
        private val yPos = RelPos(ImmutablePos(4),0)
        private val wPos = RelPos(xPos,width)
        private val hPos = RelPos(yPos,height)
        private val set = PosSet(xPos,yPos,wPos,hPos,spacingW,spacingH)

        private val titleElement: PositionedElement<TextWidget> = createInitialElement()
        private val elements: MutableMap<String, PositionedElement<*>> = mutableMapOf(
            "title" to titleElement
        )

        private fun updateWidth(newWidth: Int) {
            if(newWidth <= width) return
            width = newWidth
            wPos.set(newWidth)
        }
        private fun updateHeight(newHeight: Int) {
            if(newHeight <= height) return
            height = newHeight
            hPos.set(newHeight)
        }

        private fun createInitialElement(): PositionedElement<TextWidget>{
            val widget = TextWidget(title,MinecraftClient.getInstance().textRenderer)
            if(set.spacingH < 4)
                widget.height = widget.height + ((4 - set.spacingH) * 2)
            val posX = SuppliedPos(xPos,0) { (wPos.get() - xPos.get()) / 2 - widget.width / 2 }
            val posY = RelPos(yPos, set.spacingH)
            return PositionedElement(widget,posX,posY,PositionGlobalAlignment.ALIGN_CENTERED)
        }

        private fun<E> createPositionedElement(set: PosSet, el: E, parent: String, positions: Array<out Position>): PositionedElement<E> where E: Element, E: Widget{
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
            var maxW = width
            var maxH = height
            for (posEl in elements.values){
                maxW = (posEl.getRight() + 4).takeIf { it > maxW } ?: maxW //4 = outer edge padding
                maxW += ((posEl.getLeft() -4).takeIf { it < 0 } ?: 0) * -1 //4 = outer edge padding
                maxH = (posEl.getBottom() + 4).takeIf { it > maxH } ?: maxH //4 = outer edge padding
            }
            updateWidth(maxW)
            updateHeight(maxH)
        }

        fun <E> addElementSpacedBoth(id: String, element: E, parent: String, spacingW: Int, spacingH: Int, vararg positions: Position): Builder where E: Element, E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW, spacingH = spacingH), element, parent, positions)
            elements[id] = posEl
            return this
        }
        fun <E> addElementSpacedW(id: String, element: E, parent: String, spacingW: Int, vararg positions: Position): Builder where E: Element, E: Widget {
            val posEl = createPositionedElement(set.copy(spacingW = spacingW), element, parent, positions)
            elements[id] = posEl
            return this
        }
        fun <E> addElementSpacedH(id: String, element: E, parent: String, spacingH: Int, vararg positions: Position): Builder where E: Element, E: Widget {
            val posEl = createPositionedElement(set.copy(spacingH = spacingH), element, parent, positions)
            elements[id] = posEl
            return this
        }
        fun <E> addElement(id: String, element: E, parent: String, vararg positions: Position): Builder where E: Element, E: Widget {
            val posEl = createPositionedElement(set, element, parent, positions)
            elements[id] = posEl
            return this
        }
        fun <E> addElement(id: String, element: E, vararg positions: Position): Builder where E: Element, E: Widget {
            return addElement(id, element, "", *positions)
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

        fun build(): PopupWidget{
            attemptRecomputeDims()
            attemptRecomputeDims() // we'll do two passes to try to cover weird cases where first pass doesn't cover everything
            val children: MutableList<Element> = mutableListOf()
            val drawables: MutableList<Drawable> = mutableListOf()
            val selectables: MutableList<Selectable> = mutableListOf()
            for (posEl in elements.values){
                children.add(posEl.element)
                if(posEl.element is Drawable)
                    drawables.add(posEl.element)
                if(posEl.element is Selectable)
                    selectables.add(posEl.element)
            }
            val positioner: BiConsumer<Int, Int> = BiConsumer { x, y ->
                xPos.set(x)
                yPos.set(y)
                for (posEl in elements.values){
                    posEl.update()
                }
            }
            return PopupWidget(title, width, height, positionX, positionY, positioner, onClose, children, selectables, drawables)
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        companion object {
            fun boundedByScreen(f: Function<Int, Int>): BiFunction<Int,Int,Int>{
                return BiFunction { sd, d -> min(sd - d, f.apply(d)) }
            }
            fun centered(): BiFunction<Int,Int,Int>{
                return BiFunction { sd, d -> sd/2 - d/2 }
            }
        }

        sealed interface Position {
            fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos,Pos>
        }

        sealed interface PositionAlignment: Position
        enum class PositionRelativePos: Position {
            BELOW {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y,globalSet.spacingH + parent.elHeight()))
                }
            },
            LEFT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x,-el.width - globalSet.spacingW),prevY)
                }
            },
            RIGHT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x,el.width + globalSet.spacingW),prevY)
                }
            }
        }

        enum class PositionRelativeAlignment: PositionAlignment {
            HORIZONTAL_TO_TOP_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y))
                }
            },
            HORIZONTAL_TO_BOTTOM_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y, parent.elHeight() - el.height))
                }
            },
            VERTICAL_TO_LEFT_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x), prevY)
                }
            },
            VERTICAL_TO_RIGHT_EDGE {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x, parent.elWidth() - el.width), prevY)
                }
            },
            CENTERED_HORIZONTALLY {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(parent.x, parent.elWidth()/2 - el.width/2), prevY)
                }
            },
            CENTERED_VERTICALLY {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(prevX, RelPos(parent.y, parent.elHeight()/2 - el.height/2))
                }
            }
        }
        enum class PositionGlobalAlignment: PositionAlignment {
            ALIGN_RIGHT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(globalSet.w,-el.width), prevY)
                }
            },
            ALIGN_LEFT {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(RelPos(globalSet.x), prevY)
                }
            },
            ALIGN_JUSTIFY {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.x,0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
                }
            },
            ALIGN_CENTERED {
                override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                    return Pair(SuppliedPos(globalSet.x,0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
                }
            }
        }

        @Internal
        class PositionedElement<T>(val element: T, var x: Pos, var y: Pos, val alignment: PositionGlobalAlignment) where T: Element, T: Widget{
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
        }

        @Internal
        data class PosSet(val x: Pos, val y: Pos, val w: Pos, val h: Pos, val spacingW: Int, val spacingH: Int)
    }
}