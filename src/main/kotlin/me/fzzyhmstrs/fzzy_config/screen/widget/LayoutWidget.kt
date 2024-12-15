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

import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.*
import me.fzzyhmstrs.fzzy_config.util.pos.*
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import java.util.*
import java.util.function.Consumer
import java.util.function.UnaryOperator

class LayoutWidget(private var x: Pos = AbsPos(0), private var y: Pos = AbsPos(0), private val paddingW: Int = 8, private val paddingH: Int = paddingW, spacingW: Int = 4, spacingH: Int = spacingW): Widget, Scalable {

    private var width: Int = 0
    private var height: Int = 0
    private var manualWidth: Int = -1
    private var manualHeight: Int = -1

    private val xPos = RelPos(ImmutableRelPos(x, paddingW), 0)
    private val yPos = RelPos(ImmutableRelPos(y, paddingH), 0)
    private val wPos = RelPos(xPos, width - (2 * paddingW))
    private val hPos = RelPos(yPos, height - (2 * paddingH))
    private val sets: Deque<PosSet> = LinkedList(listOf(PosSet(xPos, yPos, wPos, hPos, spacingW, spacingH)))

    private val elements: MutableMap<String, PositionedElement<*>> = mutableMapOf()

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

    fun setPos(x: Pos, y: Pos): LayoutWidget {
        this.x = x
        this.y = y
        updateElements()
        return this
    }

    override fun setX(x: Int) {
        this.x.set(x)
        updateElements()
    }

    override fun setY(y: Int) {
        this.y.set(y)
        updateElements()
    }

    override fun setPosition(x: Int, y: Int) {
        this.x.set(x)
        this.y.set(y)
        updateElements()
    }

    override fun getX(): Int {
        return x.get()
    }

    override fun getY(): Int {
        return y.get()
    }

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun setWidth(width: Int) {
        manualWidth = width
        compute()
    }

    override fun setHeight(height: Int) {
        manualHeight = height
        compute()
    }

    fun clampWidth(width: Int): LayoutWidget {
        manualWidth = width
        if (elements.isNotEmpty())
            compute()
        return this
    }

    fun clampHeight(height: Int): LayoutWidget {
        manualHeight = height
        if (elements.isNotEmpty())
            compute()
        return this
    }

    override fun forEachChild(consumer: Consumer<ClickableWidget>?) {
        for ((_, e) in elements) {
            e.element.forEachChild(consumer)
        }
    }

    fun categorize(children: MutableList<Element>, drawables: MutableList<Drawable>, selectables: MutableList<Selectable>, other: Consumer<Widget>) {
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

    fun pushSpacing(w: UnaryOperator<Int>, h: UnaryOperator<Int>): LayoutWidget {
        val prev = sets.peek()
        sets.push(prev.copy(spacingW = w.apply(prev.spacingW), spacingH = h.apply(prev.spacingH)))
        return this
    }

    fun popSpacing(): LayoutWidget {
        if (sets.size > 0) {
            sets.pop()
        }
        return this
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

    private fun attemptRecomputeDims() {
        if (manualHeight > 0 && manualWidth > 0) {
            updateWidth(manualWidth)
            updateHeight(manualHeight)
            return
        }
        var maxW = 0
        var maxH = 0
        for ((_, posEl) in elements) {
            maxW = (posEl.getRight() + paddingW - ((posEl.getLeft() - paddingW).takeIf { it < 0 } ?: 0)).takeIf { it > maxW } ?: maxW
            maxH = (posEl.getBottom() + paddingH).takeIf { it > maxH } ?: maxH
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

    fun compute(): LayoutWidget {
        for (posEl in elements.values) {
            if (posEl.element is LayoutWidget) {
                posEl.element.compute()
            }
        }
        attemptRecomputeDims()
        attemptRecomputeDims()
        for (posEl in elements.values) {
            if (posEl.alignment == Position.ALIGN_JUSTIFY) {
                if (posEl.element is ClickableWidget) {
                    posEl.element.width = width - (2 * paddingW)
                } else if (posEl.element is Scalable) {
                    posEl.element.setWidth(width - (2 * paddingW))
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
                        posEl.x.dec(posEl.getLeft() - closestLeftEl.getRight())
                        posEl.x.inc(posEl.set.spacingW)
                        posEl.element.setWidth(prevRight - posEl.getLeft())
                    }
                } else {
                    if (posEl.element is ClickableWidget) {
                        val prevRight = posEl.getRight()
                        posEl.x.dec(posEl.getLeft() - posEl.set.x.get())
                        posEl.element.width = prevRight - posEl.getLeft()
                    } else if (posEl.element is Scalable) {
                        val prevRight = posEl.getRight()
                        posEl.x.dec(posEl.getLeft() - posEl.set.x.get())
                        posEl.element.setWidth(prevRight - posEl.getLeft())
                    }
                }
            }
        }
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
        fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos>

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
             * Aligns an element to the right side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the left of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_RIGHT_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY
        }
    }

    //client
    enum class PositionRelativePos: PopupWidget.Builder.Position, Position {
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
    enum class PositionRelativeAlignment: Builder.Position, Position {
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
    sealed interface PositionAlignment: Builder.Position, Position {
        fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos>
    }

    //client
    enum class PositionGlobalAlignment: PositionAlignment {
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT {
            override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT {
            override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_CENTER {
            override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_JUSTIFY {
            override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.x, 0) { (globalSet.w.get() - globalSet.x.get()) / 2 - el.width / 2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_AND_JUSTIFY {
            override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.x), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT_AND_JUSTIFY {
            override fun position(parent: PositionedElement<*>, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.w, 0) {-el.width}, prevY)
            }
        }
    }
}