/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.*
import me.fzzyhmstrs.fzzy_config.util.pos.*
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.Consumer
import java.util.function.UnaryOperator
import kotlin.math.max
import kotlin.math.min

//TODO
class LayoutWidget(private var x: Pos = AbsPos(0), private var y: Pos = AbsPos(0), private val paddingW: Int = 8, private val paddingH: Int = paddingW, private val spacingW: Int = 4, private val spacingH: Int = spacingW): Widget, Scalable {

    private var width: Int = 0
    private var height: Int = 0
    private var manualWidth: Int = -1
    private var manualHeight: Int = -1

    private val xPos = MutableReferencePos(x, paddingW)
    private val yPos = MutableReferencePos(y, paddingH)
    private val wPos = RelPos(xPos, width - (2 * paddingW))
    private val hPos = RelPos(yPos, height - (2 * paddingH))
    private val sets: Deque<PosSet> = LinkedList(listOf(PosSet(xPos, yPos, wPos, hPos, spacingW, spacingH)))

    private val elements: MutableMap<String, PositionedElement<*>> = mutableMapOf()

    /**
     * Recursively retrieves the named element. Starts with this layout's elements, then burrows into nested layouts' elements as applicable.
     *
     * If multiple elements in the layout tree happen to have the same id, it will return the first matching element encountered.
     * @param name String element id
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun getElement(name: String): LayoutElement? {
        return elements[name] ?: elements.values.firstNotNullOfOrNull { it.element.nullCast<LayoutWidget>()?.getElement(name) }
    }

    //TODO
    fun getGeneralVerticalPadding(): Int {
        return paddingH
    }
    //TODO
    fun getGeneralHorizontalPadding(): Int {
        return paddingW
    }
    //TODO
    fun getGeneralVerticalSpacing(): Int {
        return spacingH
    }
    //TODO
    fun getGeneralHorizontalSpacing(): Int {
        return spacingW
    }

    private fun updateElementWidths(delta: Int) {
        for ((_, e) in elements) {
            e.updateWidth(delta)
        }
    }

    private fun updateElements() {
        for ((_, e) in elements) {
            e.update()
        }
    }

    private fun updateWidth(newWidth: Int) {
        width = newWidth
        wPos.set(newWidth - (2 * paddingW))
    }
    private fun updateHeight(newHeight: Int) {
        height = newHeight
        hPos.set(newHeight - (2 * paddingH))
    }

    //TODO
    fun setPos(x: Pos, y: Pos): LayoutWidget {
        this.x = RelPos(x)
        this.y = RelPos(y)
        xPos.parent = this.x
        yPos.parent = this.y
        updateElements()
        return this
    }

    //TODO
    override fun setX(x: Int) {
        this.x = AbsPos(x)
        xPos.parent = this.x
        updateElements()
    }

    //TODO
    override fun setY(y: Int) {
        this.y = AbsPos(y)
        yPos.parent = this.y
        updateElements()
    }

    //TODO
    override fun setPosition(x: Int, y: Int) {
        this.x = AbsPos(x)
        this.y = AbsPos(y)
        xPos.parent = this.x
        yPos.parent = this.y
        updateElements()
    }

    //TODO
    override fun getX(): Int {
        return x.get()
    }

    //TODO
    override fun getY(): Int {
        return y.get()
    }

    //TODO
    override fun getWidth(): Int {
        return if(manualWidth != -1) manualWidth else width
    }

    //TODO
    override fun getHeight(): Int {
        return if (manualHeight != -1) manualHeight else height
    }

    //TODO
    override fun setWidth(width: Int) {
        val bl = manualWidth != width
        val oldWidth = if (bl && this.width != 0) getWidth() else width
        manualWidth = width
        if (bl) {
            updateElementWidths(width - oldWidth)
            this.width = width
            compute(true)
        }
    }

    internal fun setWidthQuiet(width: Int) {
        manualWidth = width
    }

    //TODO
    override fun setHeight(height: Int) {
        val bl = manualHeight != height
        manualHeight = height
        if (bl) {
            this.height = height
            compute()
        }
    }

    internal fun setHeightQuiet(height: Int) {
        manualHeight = height
    }

    //TODO
    fun setDimensions(width: Int, height: Int) {
        val bl1 = manualWidth != width
        val oldWidth = if (bl1 && this.width != 0) getWidth() else width
        val bl2 = manualHeight != height
        manualWidth = width
        manualHeight = height
        if (bl1) {
            updateElementWidths(width - oldWidth)
            this.width = width
        }
        if (bl2) {
            this.height = height
        }
        if (bl1 || bl2) {
            compute()
        }
    }

    //TODO
    fun clampWidth(width: Int): LayoutWidget {
        val bl = manualWidth != width
        val oldWidth = if (bl && this.width != 0) getWidth() else width
        manualWidth = width
        if (elements.isNotEmpty() && bl) {
            updateElementWidths(width - oldWidth)
            this.width = width
            compute()
        }
        return this
    }

    //TODO
    fun clampHeight(height: Int): LayoutWidget {
        val bl = manualHeight != height
        manualHeight = height
        if (elements.isNotEmpty() && bl) {
            this.height = height
            compute()
        }
        return this
    }

    @Internal
    override fun forEachChild(consumer: Consumer<ClickableWidget>?) {
        for ((_, e) in elements) {
            e.element.forEachChild(consumer)
        }
    }

    //TODO
    fun categorize(children: MutableList<Element>, drawables: MutableList<Drawable>, selectables: MutableList<Selectable>, other: Consumer<Widget> = Consumer { _-> }) {
        for ((_, posEl) in elements) {
            if (posEl.element is LayoutWidget) { //child layouts ship their children flat, since layouts just position things, they don't actually manage them like parent elements
                posEl.element.categorize(children, drawables, selectables, other)
                other.accept(posEl.element)
                continue
            }
            if(posEl.element is Element)
                children.add(posEl.element)
            if(posEl.element is Drawable)
                drawables.add(posEl.element)
            if(posEl.element is Selectable)
                selectables.add(posEl.element)
            other.accept(posEl.element)
        }
    }

    private fun <E> createPositionedElement(set: PosSet, el: E, parent: String, positions: Array<out Position>): PositionedElement<E> where E: Widget {

        val parentEl = elements[parent]
        if (parentEl == null) { //initial element
            var newX: Pos = RelPos(set.x)
            var newY: Pos = RelPos(set.y)
            var alignment: PositionGlobalAlignment = PositionGlobalAlignment.ALIGN_CENTER
            for (pos in positions) {
                if (pos is PositionAlignment) {
                    val pair = pos.positionInitial(el, set, newX, newY)
                    newX = pair.first
                    newY = pair.second
                    if (pos is PositionGlobalAlignment) {
                        alignment = pos
                    }
                }
            }
            return PositionedElement(el, set, newX, newY, alignment)
        }
        var newX: Pos = RelPos(set.x, set.spacingW)
        var newY: Pos = RelPos(set.y, set.spacingH)
        //subsequent elements
        var alignment: PositionGlobalAlignment = parentEl.alignment
        for(pos in positions) {
            val pair = pos.position(parentEl, el, set, newX, newY)
            newX = pair.first
            newY = pair.second
            if (pos is PositionGlobalAlignment) {
                alignment = pos
            }
        }
        return PositionedElement(el, set, newX, newY, alignment)
    }

    private var lastEl = ""

    /**
     * Push a custom element spacing to this widgets spacing stack. any elements added after this push will be spaced using the top h/w spacing on that stack, or the default spacing provided in the widget constructor if no custom spacing exists on the stack
     * @param w [UnaryOperator] that passes the current horizontal spacing (top of the stack) and returns what the new spacing should be
     * @param h [UnaryOperator] that passes the current vertival spacing (top of the stack) and returns what the new spacing should be
     * @return this widget
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun pushSpacing(w: UnaryOperator<Int>, h: UnaryOperator<Int>): LayoutWidget {
        val prev = sets.peek()
        sets.push(prev.copy(spacingW = w.apply(prev.spacingW), spacingH = h.apply(prev.spacingH)))
        return this
    }

    /**
     * Pops a set of custom spacing off this widgets spacing stack. If all custom spacings are popped, will revert to the default spacing provided in the constructor
     * @return this widget
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun popSpacing(): LayoutWidget {
        if (sets.size > 1) {
            sets.pop()
        }
        return this
    }

    /**
     * Returns the id of the last element added to this widget, or "" if none have been added yet
     * @return String id of the last element added
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun lastElement(): String {
        return lastEl
    }

    /**
     * Adds an element, keyed off a manually defined parent element. Uses the default padding.
     *
     * NOTE: "element" here refers to a piece of a layout. "Elements" do NOT necessarily have to be minecraft [Element]
     * @param E - Any subclass of [Widget]
     * @param id String - the id of this element, used when an element refers to this one as a parent
     * @param element E - the widget
     * @param parent String - the id of the parent to key layout of this new element off of.
     * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
     * @return Builder - this builder for further use
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun <E: Widget> add(id: String, element: E, parent: String, vararg positions: Position): LayoutWidget {
        val posEl = createPositionedElement(sets.peek(), element, parent, positions)
        elements[id] = posEl
        lastEl = id
        return this
    }
    /**
     * Adds an element, automatically keyed off the last added element (or "title" if this is the first added element). Uses the default padding.
     * @param E - Any subclass of [Widget]
     * @param id String - the id of this element, used when an element refers to this one as a parent
     * @param element E - the widget
     * @param positions vararg [Position] - defines the layout arrangement of this element compared to its parent. See the doc for Position for details.
     * @return Builder - this builder for further use
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun <E: Widget> add(id: String, element: E, vararg positions: Position): LayoutWidget {
        return add(id, element, lastEl, *positions)
    }

    private fun attemptRecomputeDims(debug: Boolean = false) {
        if (manualHeight > 0 && manualWidth > 0) {
            updateWidth(manualWidth)
            updateHeight(manualHeight)
            return
        }
        var minW = 1000000
        var maxW = -1000000
        var maxH = -1000000
        for ((_, posEl) in elements) {
            minW = min(max(posEl.getLeft(), paddingW), minW)
        }
        for ((_, posEl) in elements) {
            maxW = max(max(posEl.getRight() - minW, posEl.elWidth()), maxW)
            maxH = max(posEl.getBottom(), maxH)
        }
        if (manualWidth <= 0) {
            maxW += paddingW * 2
            maxW -= this.x.get()
            updateWidth(maxW)
        } else {
            updateWidth(manualWidth)
        }
        if (manualHeight <= 0) {
            maxH += paddingH
            maxH -= this.y.get()
            updateHeight(maxH)
        } else {
            updateHeight(manualHeight)
        }
    }

    fun update() {
        updateElements()
    }

    fun compute(debug: Boolean = false): LayoutWidget {
        for (posEl in elements.values) {
            if (posEl.element is LayoutWidget) {
                posEl.element.compute(debug)
            }
        }
        attemptRecomputeDims(debug)
        for (posEl in elements.values) {
            if (posEl.alignment == Position.ALIGN_JUSTIFY) {
                if (posEl.element is ClickableWidget) {
                    posEl.element.width = width - (2 * paddingW)
                } else if (posEl.element is Scalable) {
                    posEl.element.setWidth(width - (2 * paddingW))
                }
            } else if (posEl.alignment == Position.ALIGN_LEFT_AND_JUSTIFY || posEl.alignment == Position.ALIGN_LEFT_OF_AND_JUSTIFY) {
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
                        posEl.element.width = closestRightEl.getLeft() - posEl.getLeft() - posEl.set.spacingW
                    } else if (posEl.element is Scalable) {
                        posEl.element.setWidth(closestRightEl.getLeft() - posEl.getLeft() - posEl.set.spacingW)
                    }
                } else {
                    if (posEl.element is ClickableWidget) {
                        posEl.element.width = posEl.set.w.get() - posEl.getLeft()
                    } else if (posEl.element is Scalable) {
                        posEl.element.setWidth(posEl.set.w.get() - posEl.getLeft())
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
                        posEl.element.width = posEl.getRight() - closestLeftEl.getRight() - posEl.set.spacingW
                    } else if (posEl.element is Scalable) {
                        val prevRight = posEl.getRight()
                        posEl.getX().dec(posEl.getLeft() - closestLeftEl.getRight())
                        posEl.getX().inc(posEl.set.spacingW)
                        posEl.element.setWidth(prevRight - posEl.getLeft())
                    }
                } else {
                    if (posEl.element is ClickableWidget) {
                        val prevRight = posEl.getRight()
                        posEl.getX().dec(posEl.getLeft() - posEl.set.x.get())
                        posEl.element.width = prevRight - posEl.getLeft()
                    } else if (posEl.element is Scalable) {
                        val prevRight = posEl.getRight()
                        posEl.getX().dec(posEl.getLeft() - posEl.set.x.get())
                        posEl.element.setWidth(prevRight - posEl.getLeft())
                    }
                }
            } else if (posEl.alignment == Position.ALIGN_LEFT_AND_STRETCH
                || posEl.alignment == Position.ALIGN_RIGHT_AND_STRETCH
                || posEl.alignment == Position.ALIGN_LEFT_OF_AND_STRETCH) {
                var closestDownEl: PositionedElement<*>? = null
                var downPos = 1000000000
                for (posElDown in elements.values) {
                    if (posEl.otherIsBelow(posElDown)) {
                        if(posElDown.getTop() < downPos) {
                            closestDownEl = posElDown
                            downPos = posElDown.getTop()
                        }
                    }
                }
                if(closestDownEl != null) {
                    if (posEl.element is ClickableWidget) {
                        posEl.element.height = closestDownEl.getTop() - posEl.getTop() - posEl.set.spacingH
                    } else if (posEl.element is Scalable) {
                        posEl.element.setHeight(closestDownEl.getTop() - posEl.getTop() - posEl.set.spacingH)
                    }
                } else {
                    if (posEl.element is ClickableWidget) {
                        posEl.element.height = posEl.set.h.get() - posEl.getTop()
                    } else if (posEl.element is Scalable) {
                        posEl.element.setHeight(posEl.set.h.get() - posEl.getTop())
                    }
                }
            }
        }
        attemptRecomputeDims(debug)
        updateElements()
        return this
    }


    /**
     * A layout position to apply to a popup element
     *
     * Typical implementation requires at least two positions, a relative position and an alignment
     * Positions are broken down into 3 sub-categories:
     * - [PositionRelativePos] - How to generally position an element relative to its parent
     * - [PositionRelativeAlignment] - How to align an element in relation to the dimension features of its parent (top, bottom, left, and right edges etc.)
     * - [LayoutWidget.PositionGlobalAlignment] - How to align an element in relation to the global dimensions of the Popup as a whole
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    //client
    sealed interface Position {
        fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos>

        /**
         * Collection of all implemented [Position]. Preferred practice is to use this collection rather than referring directly to the underlying Enums
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @Suppress("DEPRECATION", "UNUSED")
        companion object Impl {
            /**
             * Positions an element below its parent. Does not define horizontal alignment or positioning.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val BELOW: Position = PositionRelativePos.BELOW
            /**
             * Positions an element to the left of its parent. Does not define vertical alignment or positioning.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val LEFT: Position = PositionRelativePos.LEFT
            /**
             * Positions an element to the right of its parent. Does not define vertical alignment or positioning.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val RIGHT: Position = PositionRelativePos.RIGHT
            /**
             * Aligns an elements top edge horizontally with the top edge of its parent. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val HORIZONTAL_TO_TOP_EDGE: Position = LayoutWidget.PositionRelativeAlignment.HORIZONTAL_TO_TOP_EDGE
            /**
             * Aligns an elements bottom edge horizontally with the bottom edge of its parent. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val HORIZONTAL_TO_BOTTOM_EDGE: Position = LayoutWidget.PositionRelativeAlignment.HORIZONTAL_TO_BOTTOM_EDGE
            /**
             * Aligns an elements left edge vertically with the left edge of its parent. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val VERTICAL_TO_LEFT_EDGE: Position = LayoutWidget.PositionRelativeAlignment.VERTICAL_TO_LEFT_EDGE
            /**
             * Aligns an elements right edge vertically with the right edge of its parent. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val VERTICAL_TO_RIGHT_EDGE: Position = LayoutWidget.PositionRelativeAlignment.VERTICAL_TO_RIGHT_EDGE
            /**
             * Centers an element vertically relative to the vertical dimensions of its parent (top and bottom edges). Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val CENTERED_VERTICALLY: Position = LayoutWidget.PositionRelativeAlignment.CENTERED_VERTICALLY
            /**
             * Centers an element horizontally relative to the horizontal dimensions of its parent (left and right edge). Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val CENTERED_HORIZONTALLY: Position = LayoutWidget.PositionRelativeAlignment.CENTERED_HORIZONTALLY
            /**
             * Aligns an element to the left side of the Popup widget. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_LEFT: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT
            /**
             * Aligns an element to the left side of the Popup widget. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            //TODO
            val ALIGN_LEFT_OF: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_OF
            /**
             * Aligns an element to the right side of the Popup widget. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_RIGHT: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT
            /**
             * Centers an element relative to the width of the Popup widget. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_CENTER: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_CENTER
            /**
             * Centers an element relative to the width of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element won't take any overlapping elemnts into consideration, it will justify to the global left and right edges of the Popup regardless.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_JUSTIFY: Position = PositionGlobalAlignment.ALIGN_JUSTIFY
            /**
             * Aligns an element to the left side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the right of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_LEFT_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_AND_JUSTIFY
            /**
             * Aligns an element to the left side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the right of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            //TODO
            val ALIGN_LEFT_OF_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_OF_AND_JUSTIFY
            /**
             * Aligns an element to the right side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the left of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_RIGHT_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY
            /**
             * Aligns an element to the left side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the right of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            //TODO
            val ALIGN_LEFT_AND_STRETCH: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_AND_STRETCH
            /**
             * Aligns an element to the left side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the right of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            //TODO
            val ALIGN_LEFT_OF_AND_STRETCH: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_OF_AND_STRETCH
            /**
             * Aligns an element to the right side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the left of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            //TODO
            val ALIGN_RIGHT_AND_STRETCH: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT_AND_STRETCH
        }
    }

    //client
    internal enum class PositionRelativePos: PopupWidget.Builder.Position, Position {
        @Deprecated("Use Positions Impl values")
        BELOW {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(prevX, ImmutableSuppliedPos(parent.getY()) { globalSet.spacingH + parent.elHeight() })
            }
        },
        @Deprecated("Use Positions Impl values")
        LEFT {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(ImmutableSuppliedPos(parent.getX()) { -el.width - globalSet.spacingW }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        RIGHT {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(ImmutableSuppliedPos(parent.getX()) { parent.elWidth() + globalSet.spacingW }, prevY)
            }
        }
    }

    //client
    internal enum class PositionRelativeAlignment: Builder.Position, Position {
        @Deprecated("Use Positions Impl values")
        HORIZONTAL_TO_TOP_EDGE {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(prevX, RelPos(parent.getY()))
            }
        },
        @Deprecated("Use Positions Impl values")
        HORIZONTAL_TO_BOTTOM_EDGE {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(prevX, RelPos(parent.getX(), parent.elHeight() - el.height))
            }
        },
        @Deprecated("Use Positions Impl values")
        VERTICAL_TO_LEFT_EDGE {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(parent.getX()), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        VERTICAL_TO_RIGHT_EDGE {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), 0){ parent.elWidth() - el.width }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        CENTERED_HORIZONTALLY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), 0) { parent.elWidth()/2 - el.width/2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        CENTERED_VERTICALLY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(prevX, RelPos(parent.getY(), parent.elHeight()/2 - el.height/2))
            }
        }
    }

    //client
    sealed interface PositionAlignment: Builder.Position, Position {
        fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos>
    }

    //client
    internal enum class PositionGlobalAlignment: PositionAlignment {
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_OF {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), globalSet.spacingW) { parent.elWidth() }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_CENTER {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_AND_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_OF_AND_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), globalSet.spacingW) { parent.elWidth() }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT_AND_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_AND_STRETCH {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_OF_AND_STRETCH {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), globalSet.spacingW) { parent.elWidth() }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT_AND_STRETCH {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }
        }
    }

    @Internal
    @Suppress("UNUSED")
    //client
    internal class PositionedElement<T: Widget>(val element: T, val set: PosSet, private var x: Pos, private var y: Pos, val alignment: PositionGlobalAlignment): LayoutElement {
        private fun upDown(): IntRange {
            return IntRange(getTop(), getBottom())
        }
        private fun leftRight(): IntRange {
            return IntRange(getLeft(), getRight())
        }

        override fun getX(): Pos {
            return x
        }
        override fun getY(): Pos {
            return y
        }
        override fun getLeft(): Int {
            return x.get()
        }
        override fun getRight(): Int {
            return x.get() + element.width
        }
        override fun getTop(): Int {
            return y.get()
        }
        override fun getBottom(): Int {
            return y.get() + element.height
        }
        override fun elWidth(): Int {
            return element.width
        }
        override fun elHeight(): Int {
            return element.height
        }

        fun update() {
            element.x = x.get()
            element.y = y.get()
        }
        fun updateWidth(delta: Int) {
            if (alignment == Position.ALIGN_JUSTIFY
                || alignment == Position.ALIGN_LEFT_AND_JUSTIFY
                || alignment == Position.ALIGN_LEFT_OF_AND_JUSTIFY
                || alignment == Position.ALIGN_RIGHT_AND_JUSTIFY)
            {
                if (element is ClickableWidget) {
                    element.width += delta
                } else if (element is Scalable) {
                    element.setWidth(elWidth() + delta)
                }
            }
        }
        fun otherIsLeftwards(element: PositionedElement<*>): Boolean {
            return inUpDownBounds(element.upDown()) && element.getRight() <= getLeft()
        }
        fun otherIsRightwards(element: PositionedElement<*>): Boolean {
            return inUpDownBounds(element.upDown()) && element.getLeft() >= getRight()
        }
        private fun inUpDownBounds(chk: IntRange): Boolean {
            val ud = upDown()
            return chk == ud || ud.contains(chk.first) || ud.contains(chk.last) || chk.contains(ud.first) || chk.contains(ud.last)
        }
        fun otherIsBelow(element: PositionedElement<*>): Boolean {
            return inLeftRightBounds(element.leftRight()) && element.getTop() >= getBottom()
        }
        private fun inLeftRightBounds(chk: IntRange): Boolean {
            val lr = leftRight()
            return chk == lr || lr.contains(chk.first) || lr.contains(chk.last) || chk.contains(lr.first) || chk.contains(lr.last)
        }

        override fun toString(): String {
            return "PosEl(${element::class.java.simpleName} | x: $x, y: $y, w: ${elWidth()}, h: ${elHeight()} | $alignment)"
        }
    }

    interface LayoutElement {
        fun getX(): Pos
        fun getY(): Pos
        fun getLeft(): Int
        fun getRight(): Int
        fun getTop(): Int
        fun getBottom(): Int
        fun elWidth(): Int
        fun elHeight(): Int
    }

    private class MutableReferencePos(var parent: Pos, val offset: Int): Pos {

        override fun get(): Int {
            return parent.get() + offset
        }

        override fun set(new: Int) {
        }

        override fun inc(amount: Int) {
        }

        override fun dec(amount: Int) {
        }

        override fun toString(): String {
            return "MutableRef(${get()})[$parent + $offset]"
        }

    }
}