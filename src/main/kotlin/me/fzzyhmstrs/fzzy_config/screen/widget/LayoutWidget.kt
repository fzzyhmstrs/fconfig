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

import me.fzzyhmstrs.fzzy_config.nullCast
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

/**
 * A powerful widget used to heuristically layout out multiple element widgets in a manner akin to web DOM layout. Widgets stored in a layout can be automatically repositioned, scaled, queried, and categorized. **NOTE:** LayoutWidget is not a [ParentElement][net.minecraft.client.gui.ParentElement], it is expected that you extract the elements from the layout and add them as children to a parent that contains this layout. The layout is only for positioning.
 * @param x [Pos], optional. The x position of this widget. Default is [AbsPos] of 0. This initial position can be overwritten later with [setPos]
 * @param y [Pos], optional. The y position of this widget. Default is [AbsPos] of 0. This initial position can be overwritten later with [setPos]
 * @param paddingW Int, optional. Default 8px. The horizontal space given around the left/right edge of the layout (where the border would go visually). Unlike the DOM, there is no margin/padding duo, just padding.
 * @param paddingH Int, optional. Default whatever `paddingW`` is. The vertical space given around the top/bottom edge of the layout (where the border would go visually). Unlike the DOM, there is no margin/padding duo, just padding.
 * @param spacingW Int, optional. Default 4. The horizontal space between elements. This can be modified per element as needed with [pushSpacing] and [popSpacing]
 * @param spacingH Int, optional. Default whatever `spacingW` is. The vertical space between elements. This can be modified per element as needed with [pushSpacing] and [popSpacing]
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class LayoutWidget @JvmOverloads constructor(
    private var x: Pos = AbsPos(0), private var y: Pos = AbsPos(0),
    private val paddingW: Int = 8, private val paddingH: Int = paddingW,
    spacingW: Int = 4, spacingH: Int = spacingW): Widget, Scalable {

    private var width: Int = 0
    private var height: Int = 0
    private var manualWidth: Int = -1
    private var manualHeight: Int = -1

    private val xPos = MutableReferencePos(x, paddingW)
    private val yPos = MutableReferencePos(y, paddingH)
    private val wPos = RelPos(xPos, width - (2 * paddingW))
    private val hPos = RelPos(yPos, height - (2 * paddingH))
    private val sets: Deque<PosSet> = ArrayDeque<PosSet>(1).also { it.add(PosSet(xPos, yPos, wPos, hPos, spacingW, spacingH)) }

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

    /**
     * Returns whether this layout has no elements yet.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun isEmpty(): Boolean {
        return elements.isEmpty()
    }

    /**
     * The vertical padding of this layout.
     * @return [paddingH]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @Suppress("unused")
    fun getGeneralVerticalPadding(): Int {
        return paddingH
    }
    /**
     * The horizontal padding of this layout.
     * @return [paddingW]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @Suppress("unused")
    fun getGeneralHorizontalPadding(): Int {
        return paddingW
    }
    /**
     * The general vertical spacing of this layout. Does not take into account the current state of the spacing stack.
     * @return the base of the position stacks vertical spacing (aka the default spacing)
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @Suppress("unused")
    fun getGeneralVerticalSpacing(): Int {
        return sets.peekLast().spacingH
    }
    /**
     * The general horizontal spacing of this layout. Does not take into account the current state of the spacing stack.
     * @return the base of the position stacks horizontal spacing (aka the default spacing)
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun getGeneralHorizontalSpacing(): Int {
        return sets.peekLast().spacingW
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

    /**
     * Sets the X and Y anchor positions this layout is positioned against. The widget will wrap the positions in a [RelPos] to avoid mutating external position state.
     * @param x [Pos] new X/Left position anchor
     * @param y [Pos] new Y/Top position anchor
     * @return this widget
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun setPos(x: Pos, y: Pos): LayoutWidget {
        this.x = RelPos(x)
        this.y = RelPos(y)
        xPos.parent = this.x
        yPos.parent = this.y
        updateElements()
        return this
    }

    /**
     * Sets the X coordinate of this layout. This updates the [x] [Pos], so will overwrite and reference made with [setPos] in the X dimension.
     *
     * If you don't want this to happen, update the referenced [Pos] instead of using this method. The whole point of [Pos] in the first place! This still may be used, especially by parent elements (including parent layouts) that automatically manipulate this widgets position.
     * @param x horizontal screen position in pixels
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun setX(x: Int) {
        this.x = AbsPos(x)
        xPos.parent = this.x
        updateElements()
    }

    /**
     * Sets the Y coordinate of this layout. This updates the [y] [Pos], so will overwrite and reference made with [setPos] in the Y dimension.
     *
     * If you don't want this to happen, update the referenced [Pos] instead of using this method. The whole point of [Pos] in the first place! This still may be used, especially by parent elements (including parent layouts) that automatically manipulate this widgets position.
     * @param y vertical screen position in pixels
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun setY(y: Int) {
        this.y = AbsPos(y)
        yPos.parent = this.y
        updateElements()
    }

    /**
     * Sets the X and Y coordinate of this layout. This updates the [x] and [y] [Pos], so will overwrite and reference made with [setPos].
     *
     * If you don't want this to happen, update the referenced [Pos] instead of using this method. The whole point of [Pos] in the first place! This still may be used, especially by parent elements (including parent layouts) that automatically manipulate this widgets position.
     * @param x horizontal screen position in pixels
     * @param y vertical screen position in pixels
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun setPosition(x: Int, y: Int) {
        this.x = AbsPos(x)
        this.y = AbsPos(y)
        xPos.parent = this.x
        yPos.parent = this.y
        updateElements()
    }

    /**
     * Returns the current X screen position of this widget. Uses the position from the [x] [Pos]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun getX(): Int {
        return x.get()
    }

    /**
     * Returns the current Y screen position of this widget. Uses the position from the [y] [Pos]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun getY(): Int {
        return y.get()
    }

    /**
     * Returns the width of this widget. If width has been clamped, will return that manual width, otherwise the automatically computed width.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun getWidth(): Int {
        return if(manualWidth != -1) manualWidth else width
    }

    /**
     * Returns the height of this widget. If height has been clamped, will return that manual height, otherwise the automatically computed height.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun getHeight(): Int {
        return if (manualHeight != -1) manualHeight else height
    }

    /**
     * Sets a manual width for this layout. Will override any automatically computed widths.
     *
     * If the width is different from the previous/automatic width, the layout will recompute
     * @param width Int width in pixels
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun setW(width: Int) {
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

    /**
     * Sets a manual height for this layout. Will override any automatically computed height
     *
     * If the height is different from the previous/automatic height, the layout will recompute.
     * @param height Int height in pixels
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun setH(height: Int) {
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

    /**
     * Sets a manual width and height for this layout. Will override any automatically computed dimensions.
     *
     * If either dimension is different from the previous/automatic version, the layout will recompute.
     * @param width Int height in pixels
     * @param height Int height in pixels
     * @author fzzyhmstrs
     * @since 0.6.0
     */
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

    /**
     * Same as [setW] but returns itself, and won't recompute if the layout is empty. This is often used up front to, as the name implies, clamp the allowable width of a new layout before adding elements.
     * @param width Int width in pixels
     * @return this layout
     * @author fzzyhmstrs
     * @since 0.6.0
     */
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

    /**
     * Same as [setH] but returns itself, and won't recompute if the layout is empty. This is often used up front to, as the name implies, clamp the allowable height of a new layout before adding elements.
     * @param height Int height in pixels
     * @return this layout
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @Suppress("unused")
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

    /**
     * Categorizes the elements in this layout into the three constituent components many screens/parents care about, as well as providing a method for arbitrary categorization
     *
     * List of widgets -> List of elements, drawables, and selectables as applicable to each.
     *
     * This method will recursively categorize elements from contained layouts.
     * @param children MutableList&lt;[Element]&gt; list to populate with each widget entry that is also an Element (most are)
     * @param drawables MutableList&lt;[Drawable]&gt; list to populate with each widget entry that is also a Drawable (again, most are)
     * @param selectables MutableList&lt;[Selectable]&gt; list to populate with each widget entry that is also a Selectable (surprise! most are)
     * @param other [Consumer]&lt;[Widget]&gt; consumes each widget entry and can do whatever you want with them. In Fzzy Config this is usually used to categorize elements into more niche interfaces like [TooltipChild]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
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
            var newX: Pos = RelPos(set.xPos)
            var newY: Pos = RelPos(set.yPos)
            @Suppress("DEPRECATION")
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
        var newX: Pos = RelPos(set.xPos, set.spacingW)
        var newY: Pos = RelPos(set.yPos, set.spacingH)
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
     * @param h [UnaryOperator] that passes the current vertical spacing (top of the stack) and returns what the new spacing should be
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
     * Adds an element, keyed off a manually defined parent element.
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
     * Adds an element, automatically keyed off the last added element (or "" if this is the first added element).
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

    fun update() {
        updateElements()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun attemptRecomputeDims(debug: Boolean = false) {
        if (manualHeight > 0 && manualWidth > 0) {
            updateWidth(manualWidth)
            updateHeight(manualHeight)
            return
        }

        if (manualWidth <= 0) {
            var minW = Int.MAX_VALUE
            var maxW = Int.MIN_VALUE
            for ((_, posEl) in elements) {
                minW = posEl.provideMinW(minW, paddingW)
            }
            //if (debug) FC.DEVLOG.error("MinW: $minW\n")
            for ((_, posEl) in elements) {
                //if (debug) FC.DEVLOG.error("E1: $posEl")
                //if (debug) FC.DEVLOG.warn("  Before: $maxW")
                maxW = posEl.provideMaxW(maxW, minW, paddingW)
                //if (debug) FC.DEVLOG.warn("  After: $maxW")
            }
            for ((_, posEl) in elements) { //re-iterate to make sure the width at least captures the content width of the elements.
                //if (debug) FC.DEVLOG.warn("E2: $posEl")
                //if (debug) FC.DEVLOG.info("  Before: $maxW")
                maxW = max(maxW, posEl.provideContentW())
                //if (debug) FC.DEVLOG.info("  After: $maxW")
            }
            maxW += (paddingW * 2)
            updateWidth(maxW)
        } else {
            updateWidth(manualWidth)
        }

        if (manualHeight <= 0) {
            var maxH = Int.MIN_VALUE
            for ((_, posEl) in elements) {
                maxH = max(posEl.getBottom(), maxH)
            }
            maxH += paddingH
            maxH -= this.y.get()
            updateHeight(maxH)
        } else {
            updateHeight(manualHeight)
        }
    }

    fun compute(debug: Boolean = false): LayoutWidget {
        for (posEl in elements.values) {
            //if (debug) FC.DEVLOG.info("E $posEl")
            if (posEl.element is LayoutWidget) {
                posEl.element.compute(debug)
            }
        }
        attemptRecomputeDims(debug)
        val w = this.getWidth()
        var redo = 2
        do {
            for (posEl in elements.values) {
                if (posEl.alignment == Position.ALIGN_JUSTIFY || posEl.alignment == Position.ALIGN_JUSTIFY_WEAK) {
                    if (posEl.element is ClickableWidget) {
                        posEl.element.width = width - (2 * paddingW)
                    } else if (posEl.element is Scalable) {
                        posEl.element.setW(width - (2 * paddingW))
                    }
                } else if (posEl.alignment == Position.ALIGN_LEFT_AND_JUSTIFY || posEl.alignment == Position.POSITION_RIGHT_OF_AND_JUSTIFY) {
                    var closestRightEl: PositionedElement<*>? = null
                    var rightPos = 1000000000
                    for (posElRight in elements.values) {
                        if (posElRight == posEl) continue
                        if (posEl.otherIsRightwards(posElRight)) {
                            if (posElRight.getLeft() < rightPos) {
                                closestRightEl = posElRight
                                rightPos = posElRight.getLeft()
                            }
                        }
                    }
                    if (closestRightEl != null) {
                        if (posEl.element is ClickableWidget) {
                            posEl.element.width = closestRightEl.getLeft() - posEl.getLeft() - posEl.set.spacingW
                        } else if (posEl.element is Scalable) {
                            posEl.element.setW(closestRightEl.getLeft() - posEl.getLeft() - posEl.set.spacingW)
                        }
                    } else {
                        if (posEl.element is ClickableWidget) {
                            posEl.element.width = posEl.set.wPos.get() - posEl.getLeft()
                        } else if (posEl.element is Scalable) {
                            posEl.element.setW(posEl.set.wPos.get() - posEl.getLeft())
                        }
                    }

                } else if (posEl.alignment == Position.ALIGN_RIGHT_AND_JUSTIFY || posEl.alignment == Position.POSITION_LEFT_OF_AND_JUSTIFY) {
                    //if (debug) FC.DEVLOG.info("Element Before: $posEl")
                    var closestLeftEl: PositionedElement<*>? = null
                    var leftPos = -1000000000
                    for (posElLeft in elements.values) {
                        if (posElLeft == posEl) continue
                        if (posEl.otherIsLeftwards(posElLeft)) {
                            if (posElLeft.getRight() > leftPos) {
                                closestLeftEl = posElLeft
                                leftPos = posElLeft.getLeft()
                            }
                        }
                    }
                    //if (debug) FC.DEVLOG.info("Closest Right Element: $closestLeftEl")
                    if (closestLeftEl != null) {
                        if (posEl.element is ClickableWidget) {
                            posEl.getX().dec(posEl.getLeft() - closestLeftEl.getRight())
                            posEl.getX().inc(posEl.set.spacingW)
                            posEl.element.width = posEl.getRight() - closestLeftEl.getRight() - posEl.set.spacingW
                        } else if (posEl.element is Scalable) {
                            val prevRight = posEl.getRight()
                            posEl.getX().dec(posEl.getLeft() - closestLeftEl.getRight())
                            posEl.getX().inc(posEl.set.spacingW)
                            posEl.element.setW(prevRight - posEl.getLeft())
                        }
                    } else {
                        if (posEl.element is ClickableWidget) {
                            val dec = posEl.getLeft() - posEl.set.xPos.get()
                            posEl.getX().dec(dec)
                            posEl.element.width += dec
                        } else if (posEl.element is Scalable) {
                            val dec = posEl.getLeft() - posEl.set.xPos.get()
                            posEl.getX().dec(dec)
                            posEl.element.setW(posEl.element.width + dec)
                        }
                    }
                    //if (debug) FC.DEVLOG.info("Element After: $posEl")
                } else if (posEl.alignment == Position.ALIGN_LEFT_AND_STRETCH
                    || posEl.alignment == Position.ALIGN_RIGHT_AND_STRETCH
                    || posEl.alignment == Position.ALIGN_LEFT_OF_AND_STRETCH
                ) {
                    var closestDownEl: PositionedElement<*>? = null
                    var downPos = 1000000000
                    for (posElDown in elements.values) {
                        if (posElDown == posEl) continue
                        if (posEl.otherIsBelow(posElDown)) {
                            if (posElDown.getTop() < downPos) {
                                closestDownEl = posElDown
                                downPos = posElDown.getTop()
                            }
                        }
                    }
                    if (closestDownEl != null) {
                        if (posEl.element is Scalable) {
                            posEl.element.setH(closestDownEl.getTop() - posEl.getTop() - posEl.set.spacingH)
                        }
                    } else {
                        if (posEl.element is Scalable) {
                            posEl.element.setH(posEl.set.hPos.get() - posEl.getTop())
                        }
                    }
                }
            }
            redo--
            attemptRecomputeDims(debug)
            if (getWidth() == w) break
        } while (redo > 0)
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
             * Aligns an element to the left side of the previous element. Does not define any other position or alignment.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
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
             * Justification of this element won't take any overlapping elements into consideration, it will justify to the global left and right edges of the Popup regardless.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_JUSTIFY: Position = PositionGlobalAlignment.ALIGN_JUSTIFY
            /**
             * Centers an element relative to the width of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element won't take any overlapping elements into consideration, it will justify to the global left and right edges of the Popup regardless.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             *
             * This alignment *won't* contribute to computing layout width. This should be used for elements that strictly want to map to the width of the other elements, not contribute any dimensional information.
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_JUSTIFY_WEAK: Position = PositionGlobalAlignment.ALIGN_JUSTIFY_WEAK
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
             * Aligns an element to the left side of previous element and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the right of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val POSITION_RIGHT_OF_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.POSITION_RIGHT_OF_AND_JUSTIFY
            /**
             * Positions an element to the right side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the left of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_RIGHT_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY
            /**
             * Aligns an element to the left side of the Popup widget and stretches it (fits to height). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements below this one into account; it will stretch to fit up to the next element or bottom of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val POSITION_LEFT_OF_AND_JUSTIFY: Position = LayoutWidget.PositionGlobalAlignment.POSITION_LEFT_OF_AND_JUSTIFY
            /**
             * Aligns an element to the left side of the Popup widget and justifies it (fits to width). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements to the left of this one into account; it will stretch to fit up to the next element or other side of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_LEFT_AND_STRETCH: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_AND_STRETCH
            /**
             * Aligns an element to the left side of the previous element and stretches it (fits to height). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements below this one into account; it will stretch to fit up to the next element or bottom of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_LEFT_OF_AND_STRETCH: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_LEFT_OF_AND_STRETCH
            /**
             * Aligns an element to the right side of the Popup widget and stretches it (fits to height). Does not define any other position or alignment.
             *
             * Justification of this element WILL take elements below this one into account; it will stretch to fit up to the next element or bottom of the widget, allowing for the default padding in between elements.
             *
             * Requires a [ClickableWidget] or instance of [Scalable] to enable resizing
             * @author fzzyhmstrs
             * @since 0.6.0
             */
            val ALIGN_RIGHT_AND_STRETCH: Position = LayoutWidget.PositionGlobalAlignment.ALIGN_RIGHT_AND_STRETCH
        }
    }

    //client
    @Suppress("DEPRECATION")
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
    @Suppress("DEPRECATION")
    internal enum class PositionRelativeAlignment: PopupWidget.Builder.Position, Position {
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
                return Pair(SuppliedPos(parent.getX(), 0) { parent.elWidth() / 2 - el.width / 2 }, prevY)
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
    @Suppress("DEPRECATION")
    sealed interface PositionAlignment: PopupWidget.Builder.Position, Position {
        fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos>
    }

    //client
    internal enum class PositionGlobalAlignment: PositionAlignment {
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_OF {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), globalSet.spacingW) { parent.elWidth() }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.wPos, 0) { -el.width }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.wPos, 0) { -el.width }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_CENTER {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.xPos, 0) { (globalSet.wPos.get() - globalSet.xPos.get()) / 2 - el.width / 2 }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.xPos, 0) { (globalSet.wPos.get() - globalSet.xPos.get()) / 2 - el.width / 2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.xPos, 0) { (globalSet.wPos.get() - globalSet.xPos.get()) / 2 - el.width / 2 }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.xPos, 0) { (globalSet.wPos.get() - globalSet.xPos.get()) / 2 - el.width / 2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_JUSTIFY_WEAK {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.xPos, 0) { (globalSet.wPos.get() - globalSet.xPos.get()) / 2 - el.width / 2 }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.xPos, 0) { (globalSet.wPos.get() - globalSet.xPos.get()) / 2 - el.width / 2 }, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_AND_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        POSITION_RIGHT_OF_AND_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), globalSet.spacingW) { parent.elWidth() }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT_AND_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.wPos, 0) {-el.width}, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.wPos, 0) {-el.width}, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        POSITION_LEFT_OF_AND_JUSTIFY {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(ImmutableSuppliedPos(parent.getX()) { -el.width -globalSet.spacingW }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.wPos, 0) {-el.width}, prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_AND_STRETCH {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_LEFT_OF_AND_STRETCH {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(parent.getX(), globalSet.spacingW) { parent.elWidth() }, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(RelPos(globalSet.xPos), prevY)
            }
        },
        @Deprecated("Use Positions Impl values")
        ALIGN_RIGHT_AND_STRETCH {
            override fun position(parent: LayoutElement, el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.wPos, 0) {-el.width}, prevY)
            }

            override fun positionInitial(el: Widget, globalSet: PosSet, prevX: Pos, prevY: Pos): Pair<Pos, Pos> {
                return Pair(SuppliedPos(globalSet.wPos, 0) {-el.width}, prevY)
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
            if (alignment == Position.ALIGN_JUSTIFY_WEAK) return 0
            return element.width
        }
        override fun elHeight(): Int {
            return element.height
        }
        fun resetWidth() {
            if (elWidth() < 0) {
                if (element is ClickableWidget) {
                    element.width = 0
                } else if (element is Scalable) {
                    element.setW(0)
                }
            }
        }

        fun update() {
            element.x = x.get()
            element.y = y.get()
        }
        fun updateWidth(delta: Int) {
            if (alignment == Position.ALIGN_JUSTIFY
                || alignment == Position.ALIGN_LEFT_AND_JUSTIFY
                || alignment == Position.POSITION_RIGHT_OF_AND_JUSTIFY
                || alignment == Position.ALIGN_RIGHT_AND_JUSTIFY)
            {
                if (element is ClickableWidget) {
                    element.width += delta
                } else if (element is Scalable) {
                    element.setW(elWidth() + delta)
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

        @Suppress("DEPRECATION")
        fun provideMinW(minW: Int, paddingW: Int): Int {
            return when (alignment) {
                PositionGlobalAlignment.ALIGN_LEFT -> min( getLeft(), minW)
                PositionGlobalAlignment.ALIGN_LEFT_OF -> min( getLeft(), minW)
                PositionGlobalAlignment.ALIGN_RIGHT -> min( getLeft(), minW)
                PositionGlobalAlignment.ALIGN_CENTER -> min(max(paddingW, getLeft()), minW)
                PositionGlobalAlignment.ALIGN_JUSTIFY -> min(max(paddingW, getLeft()), minW)
                PositionGlobalAlignment.ALIGN_JUSTIFY_WEAK -> min(max(paddingW, getLeft()), minW)
                PositionGlobalAlignment.ALIGN_LEFT_AND_JUSTIFY -> min( getLeft(), minW)
                PositionGlobalAlignment.POSITION_RIGHT_OF_AND_JUSTIFY -> min( getLeft(), minW)
                PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY -> min( getLeft(), minW)
                PositionGlobalAlignment.POSITION_LEFT_OF_AND_JUSTIFY -> min( getLeft(), minW)
                PositionGlobalAlignment.ALIGN_LEFT_AND_STRETCH -> min( getLeft(), minW)
                PositionGlobalAlignment.ALIGN_LEFT_OF_AND_STRETCH -> min( getLeft(), minW)
                PositionGlobalAlignment.ALIGN_RIGHT_AND_STRETCH -> min( getLeft(), minW)
            }
        }

        @Suppress("DEPRECATION")
        fun provideMaxW(maxW: Int, minW: Int, paddingW: Int): Int {
            return when (alignment) {
                PositionGlobalAlignment.ALIGN_LEFT -> max(max(getRight() - minW, elWidth()), maxW)
                PositionGlobalAlignment.ALIGN_LEFT_OF -> maxW
                PositionGlobalAlignment.ALIGN_RIGHT -> max(max(getRight() - minW, elWidth()), maxW)
                PositionGlobalAlignment.ALIGN_CENTER -> max(elWidth(), maxW)
                PositionGlobalAlignment.ALIGN_JUSTIFY -> max(elWidth() - paddingW, maxW)
                PositionGlobalAlignment.ALIGN_JUSTIFY_WEAK -> maxW
                PositionGlobalAlignment.ALIGN_LEFT_AND_JUSTIFY -> max(max(getRight() - minW, elWidth()) - paddingW, maxW)
                PositionGlobalAlignment.POSITION_RIGHT_OF_AND_JUSTIFY -> max(max(getRight() - minW, elWidth()) - paddingW, maxW)
                PositionGlobalAlignment.ALIGN_RIGHT_AND_JUSTIFY -> max(max(getRight() - minW, elWidth()) - paddingW, maxW)
                PositionGlobalAlignment.POSITION_LEFT_OF_AND_JUSTIFY -> maxW
                PositionGlobalAlignment.ALIGN_LEFT_AND_STRETCH -> max(max(getRight() - minW, elWidth()), maxW)
                PositionGlobalAlignment.ALIGN_LEFT_OF_AND_STRETCH -> maxW
                PositionGlobalAlignment.ALIGN_RIGHT_AND_STRETCH -> max(max(getRight() - minW, elWidth()), maxW)
            }
        }

        @Suppress("DEPRECATION")
        fun provideContentW(): Int {
            return when (alignment) {
                PositionGlobalAlignment.ALIGN_JUSTIFY_WEAK -> 0
                else -> elWidth()
            }
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

    @Internal
    //client
    data class PosSet(val xPos: Pos, val yPos: Pos, val wPos: Pos, val hPos: Pos, val spacingW: Int, val spacingH: Int)
}