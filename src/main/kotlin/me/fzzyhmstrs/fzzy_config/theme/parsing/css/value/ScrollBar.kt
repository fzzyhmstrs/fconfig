/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.value

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.Creator
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.Creator.Companion.applyValue
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.SequencedValueBuilder
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilders
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.Declaration
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.minecraft.util.Identifier
import java.util.*

data class ScrollBar(
    val scrollWidth: Int = 6,
    val scrollButtonHeight: Int = 6,
    val scrollType: ScrollBarType = ScrollBarType.DYNAMIC,
    val scrollFixedHeight: Int = 8,
    val scrollButtonType: ScrollBarButtons = ScrollBarButtons.SPLIT,
    val scrollShowHoverOnly: Boolean = false,
    val scrollBarBackground: Identifier = "widget/scroll/vanilla/scroller_background".fcId(),
    val scrollBarScroller: Identifier = "widget/scroll/vanilla/scroller".fcId(),
    val scrollBarHighlighted: Identifier = "widget/scroll/vanilla/scroller_highlighted".fcId(),
    val scrollBarDown: Identifier = "widget/scroll/vanilla/scroll_down".fcId(),
    val scrollBarDownHighlighted: Identifier =  "widget/scroll/vanilla/scroll_down_highlighted".fcId(),
    val scrollBarUp: Identifier = "widget/scroll/vanilla/scroll_up".fcId(),
    val scrollBarUpHighlighted: Identifier = "widget/scroll/vanilla/scroll_up_highlighted".fcId()) {

    /**
     * The visual style of the scroll bar itself
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    enum class ScrollBarType {
        /**
         * The scroll bar changes height based on the amount of scroll available.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        DYNAMIC,
        /**
         * The scroll bar is a set height no matter the scroll amount.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        FIXED;

        companion object {
            val MAP = mapOf("dynamic" to DYNAMIC, "fixed" to FIXED)
        }
    }

    enum class ScrollBarButtons {
        NONE {
            override fun renderButtons(): Boolean {
                return false
            }
            override fun upY(top: Int, bottom: Int): Int {
                return 0
            }

            override fun downY(top: Int, bottom: Int): Int {
                return 0
            }

            override fun scrollTop(top: Int): Int {
                return top
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return false
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return false
            }
        },
        TOP {
            override fun renderButtons(): Boolean {
                return true
            }
            override fun upY(top: Int, bottom: Int): Int {
                return top
            }

            override fun downY(top: Int, bottom: Int): Int {
                return top + 6
            }

            override fun scrollTop(top: Int): Int {
                return top + 12
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < (top + 6) && mouseY >= top
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < scrollTop(top) && mouseY >= (top + 6)
            }
        },
        BOTTOM {
            override fun renderButtons(): Boolean {
                return true
            }
            override fun upY(top: Int, bottom: Int): Int {
                return bottom - 12
            }

            override fun downY(top: Int, bottom: Int): Int {
                return bottom - 6
            }

            override fun scrollTop(top: Int): Int {
                return top
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom - 12
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < (bottom - 6) && mouseY >= scrollBottom(bottom)
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < bottom && mouseY >= (bottom - 6)
            }
        },
        SPLIT {
            override fun renderButtons(): Boolean {
                return true
            }

            override fun upY(top: Int, bottom: Int): Int {
                return top
            }

            override fun downY(top: Int, bottom: Int): Int {
                return bottom - 6
            }

            override fun scrollTop(top: Int): Int {
                return top + 6
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom - 6
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < scrollTop(top) && mouseY >= top
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < bottom && mouseY >= scrollBottom(bottom)
            }
        };

        abstract fun renderButtons(): Boolean
        abstract fun upY(top: Int, bottom: Int): Int
        abstract fun downY(top: Int, bottom: Int): Int
        abstract fun scrollTop(top: Int): Int
        abstract fun scrollBottom(bottom: Int): Int
        abstract fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean
        abstract fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean

        companion object {
            val MAP = ValueBuilders.createMap(ScrollBarButtons::class.java)
        }
    }
}

class ScrollBarDeclaration(private val scrollWidth: LengthValue,
                           private val scrollWidthDefault: Boolean,
                           private val scrollButtonHeight: LengthValue,
                           private val scrollButtonHeightDefault: Boolean,
                           private val scrollType: ScrollBar.ScrollBarType,
                           private val scrollTypeDefault: Boolean,
                           private val scrollFixedHeight: LengthValue,
                           private val scrollFixedHeightDefault: Boolean,
                           private val scrollButtonType: ScrollBar.ScrollBarButtons,
                           private val scrollButtonTypeDefault: Boolean,
                           private val scrollShowHoverOnly: Boolean,
                           private val scrollShowHoverOnlyDefault: Boolean,
                           private val scrollBarBackground: Identifier,
                           private val scrollBarBackgroundDefault: Boolean,
                           private val scrollBarScroller: Identifier,
                           private val scrollBarScrollerDefault: Boolean,
                           private val scrollBarHighlighted: Identifier,
                           private val scrollBarHighlightedDefault: Boolean,
                           private val scrollBarDown: Identifier,
                           private val scrollBarDownDefault: Boolean,
                           private val scrollBarDownHighlighted: Identifier,
                           private val scrollBarDownHighlightedDefault: Boolean,
                           private val scrollBarUp: Identifier,
                           private val scrollBarUpDefault: Boolean,
                           private val scrollBarUpHighlighted: Identifier,
                           private val scrollBarUpHighlightedDefault: Boolean): Declaration<ScrollBar> {
    override fun ruleValue(): ScrollBar {
        return ScrollBar(
            scrollWidth.getValue(),
            scrollButtonHeight.getValue(),
            scrollType,
            scrollFixedHeight.getValue(),
            scrollButtonType,
            scrollShowHoverOnly,
            scrollBarBackground,
            scrollBarScroller,
            scrollBarHighlighted,
            scrollBarDown,
            scrollBarDownHighlighted,
            scrollBarUp,
            scrollBarUpHighlighted
        )
    }

    override fun layer(lower: Declaration<ScrollBar>): Declaration<ScrollBar> {
        lower as ScrollBarDeclaration
        val scrollWidth = if (scrollWidthDefault) lower.scrollWidth else scrollWidth
        val scrollButtonHeight = if (scrollButtonHeightDefault) lower.scrollButtonHeight else scrollButtonHeight
        val scrollType = if (scrollTypeDefault) lower.scrollType else scrollType
        val scrollFixedHeight = if (scrollFixedHeightDefault) lower.scrollFixedHeight else scrollFixedHeight
        val scrollButtonType = if (scrollButtonTypeDefault) lower.scrollButtonType else scrollButtonType
        val scrollShowHoverOnly = if (scrollShowHoverOnlyDefault) lower.scrollShowHoverOnly else scrollShowHoverOnly
        val scrollBarBackground = if (scrollBarBackgroundDefault) lower.scrollBarBackground else scrollBarBackground
        val scrollBarScroller = if (scrollBarScrollerDefault) lower.scrollBarScroller else scrollBarScroller
        val scrollBarHighlighted = if (scrollBarHighlightedDefault) lower.scrollBarHighlighted else scrollBarHighlighted
        val scrollBarDown = if(scrollBarDownDefault) lower.scrollBarDown else scrollBarDown
        val scrollBarDownHighlighted = if(scrollBarDownHighlightedDefault) lower.scrollBarDownHighlighted else scrollBarDownHighlighted
        val scrollBarUp = if(scrollBarUpDefault) lower.scrollBarUp else scrollBarUp
        val scrollBarUpHighlighted = if(scrollBarUpHighlightedDefault) lower.scrollBarUpHighlighted else scrollBarUpHighlighted
        return ScrollBarDeclaration(
            scrollWidth, scrollWidthDefault && lower.scrollWidthDefault,
            scrollButtonHeight, scrollButtonHeightDefault && lower.scrollButtonHeightDefault,
            scrollType, scrollTypeDefault && lower.scrollTypeDefault,
            scrollFixedHeight, scrollFixedHeightDefault && lower.scrollFixedHeightDefault,
            scrollButtonType, scrollButtonTypeDefault && lower.scrollButtonTypeDefault,
            scrollShowHoverOnly, scrollShowHoverOnlyDefault && lower.scrollShowHoverOnlyDefault,
            scrollBarBackground, scrollBarBackgroundDefault && lower.scrollBarBackgroundDefault,
            scrollBarScroller, scrollBarScrollerDefault && lower.scrollBarScrollerDefault,
            scrollBarHighlighted, scrollBarHighlightedDefault && lower.scrollBarHighlightedDefault,
            scrollBarDown, scrollBarDownDefault && lower.scrollBarDownDefault,
            scrollBarDownHighlighted, scrollBarDownHighlightedDefault && lower.scrollBarDownHighlightedDefault,
            scrollBarUp, scrollBarUpDefault && lower.scrollBarUpDefault,
            scrollBarUpHighlighted, scrollBarUpHighlightedDefault && lower.scrollBarUpHighlightedDefault
        )
    }
}

class ScrollBarDeclarationCreator: Creator<ScrollBarDeclaration> {
    var scrollWidth: LengthValue = LengthValue(6)
        set(value) {
            scrollWidthDefault = false
            field = value
        }
    private var scrollWidthDefault: Boolean = true
    var scrollButtonHeight: LengthValue = LengthValue(6)
        set(value) {
            scrollButtonHeightDefault = false
            field = value
        }
    private var scrollButtonHeightDefault: Boolean = true
    var scrollType: ScrollBar.ScrollBarType = ScrollBar.ScrollBarType.DYNAMIC
        set(value) {
            scrollTypeDefault = false
            field = value
        }
    private var scrollTypeDefault: Boolean = true
    var scrollFixedHeight: LengthValue = LengthValue(8)
        set(value) {
            scrollFixedHeightDefault = false
            field = value
        }
    private var scrollFixedHeightDefault: Boolean = true
    var scrollButtonType: ScrollBar.ScrollBarButtons = ScrollBar.ScrollBarButtons.SPLIT
        set(value) {
            scrollButtonTypeDefault = false
            field = value
        }
    private var scrollButtonTypeDefault: Boolean = true
    var scrollShowHoverOnly: Boolean = false
        set(value) {
            scrollShowHoverOnlyDefault = false
            field = value
        }
    private var scrollShowHoverOnlyDefault: Boolean = true
    var scrollBarBackground: Identifier = "widget/scroll/vanilla/scroller_background".fcId()
        set(value) {
            scrollBarBackgroundDefault = false
            field = value
        }
    private var scrollBarBackgroundDefault: Boolean = true
    var scrollBarScroller: Identifier = "widget/scroll/vanilla/scroller".fcId()
        set(value) {
            scrollBarScrollerDefault = false
            field = value
        }
    private var scrollBarScrollerDefault: Boolean = true
    var scrollBarHighlighted: Identifier = "widget/scroll/vanilla/scroller_highlighted".fcId()
        set(value) {
            scrollBarHighlightedDefault = false
            field = value
        }
    private var scrollBarHighlightedDefault: Boolean = true
    var scrollBarDown: Identifier = "widget/scroll/vanilla/scroll_down".fcId()
        set(value) {
            scrollBarDownDefault = false
            field = value
        }
    private var scrollBarDownDefault: Boolean = true
    var scrollBarDownHighlighted: Identifier =  "widget/scroll/vanilla/scroll_down_highlighted".fcId()
        set(value) {
            scrollBarDownHighlightedDefault = false
            field = value
        }
    private var scrollBarDownHighlightedDefault: Boolean = true
    var scrollBarUp: Identifier = "widget/scroll/vanilla/scroll_up".fcId()
        set(value) {
            scrollBarUpDefault = false
            field = value
        }
    private var scrollBarUpDefault: Boolean = true
    var scrollBarUpHighlighted: Identifier = "widget/scroll/vanilla/scroll_up_highlighted".fcId()
        set(value) {
            scrollBarUpHighlightedDefault = false
            field = value
        }
    private var scrollBarUpHighlightedDefault: Boolean = true

    override fun create(): ScrollBarDeclaration {
        return ScrollBarDeclaration(scrollWidth, scrollWidthDefault, scrollButtonHeight, scrollButtonHeightDefault, scrollType, scrollTypeDefault, scrollFixedHeight, scrollFixedHeightDefault, scrollButtonType, scrollButtonTypeDefault, scrollShowHoverOnly, scrollShowHoverOnlyDefault, scrollBarBackground, scrollBarBackgroundDefault, scrollBarScroller, scrollBarScrollerDefault, scrollBarHighlighted, scrollBarHighlightedDefault, scrollBarDown, scrollBarDownDefault, scrollBarDownHighlighted, scrollBarDownHighlightedDefault, scrollBarUp, scrollBarUpDefault, scrollBarUpHighlighted, scrollBarUpHighlightedDefault)
    }
}

object ScrollBarDeclarationKey: DeclarationKey<ScrollBar, ScrollBarDeclarationCreator> {
    override fun createDecl(
        decl: String,
        queue: TokenQueue,
        builder: ScrollBarDeclarationCreator
    ): ValidationResult<Optional<ScrollBarDeclarationCreator>> {
        return when (decl) {
            "scroll-bar" -> {
                SequencedValueBuilder.create<ScrollBarDeclaration, ScrollBarDeclarationCreator>()
                    .sequence(ValueBuilders.lengthValueBuilder(6)) { t, c -> c.scrollWidth = t }
                    .sequence(ValueBuilders.lengthValueBuilder(6)) { t, c -> c.scrollButtonHeight = t }
                    .sequence(ValueBuilders.enumValueBuilder(ScrollBar.ScrollBarType.MAP, fallback = ScrollBar.ScrollBarType.DYNAMIC)) { t, c -> c.scrollType = t }
                    .sequence(ValueBuilders.lengthValueBuilder(8)) { t, c -> c.scrollFixedHeight = t }
                    .sequence(ValueBuilders.enumValueBuilder(ScrollBar.ScrollBarButtons.MAP, fallback = ScrollBar.ScrollBarButtons.SPLIT)) { t, c -> c.scrollButtonType = t }
                    .sequence(ValueBuilders.boolValueBuilder()) { t, c -> c.scrollShowHoverOnly = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller_background".fcId())) { t, c -> c.scrollBarBackground = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller".fcId())) { t, c -> c.scrollBarScroller = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller_highlighted".fcId())) { t, c -> c.scrollBarBackground = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_down".fcId())) { t, c -> c.scrollBarDown = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_down_highlighted".fcId())) { t, c -> c.scrollBarDownHighlighted = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_up".fcId())) { t, c -> c.scrollBarUp = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_up_highlighted".fcId())) { t, c -> c.scrollBarUpHighlighted = t }
                    .build().applyValue(queue, builder)
            }
            "scroll-bar-width" -> {
                builder.applyValue(queue, ValueBuilders.lengthValueBuilder(6)) { t, c -> c.scrollWidth = t }
            }
            "scroll-bar-button-height" -> {
                builder.applyValue(queue, ValueBuilders.lengthValueBuilder(6)) { t, c -> c.scrollButtonHeight = t }
            }
            "scroll-bar-type" -> {
                builder.applyValue(
                    queue, ValueBuilders.enumValueBuilder(ScrollBar.ScrollBarType.MAP, fallback = ScrollBar.ScrollBarType.DYNAMIC)
                ) { t, c -> c.scrollType = t }
            }
            "scroll-bar-fixed-height" -> {
                builder.applyValue(queue, ValueBuilders.lengthValueBuilder(8)) { t, c -> c.scrollFixedHeight = t }
            }
            "scroll-bar-button-type" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.enumValueBuilder(ScrollBar.ScrollBarButtons.MAP, fallback = ScrollBar.ScrollBarButtons.SPLIT)
                ) { t, c -> c.scrollButtonType = t }
            }
            "scroll-bar-show-on-hover-only" -> {
                builder.applyValue(queue, ValueBuilders.boolValueBuilder()) { t, c -> c.scrollShowHoverOnly = t }
            }
            "scroll-bar-texture" -> {
                SequencedValueBuilder.create<ScrollBarDeclaration, ScrollBarDeclarationCreator>()
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller_background".fcId())) { t, c -> c.scrollBarBackground = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller".fcId())) { t, c -> c.scrollBarScroller = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller_highlighted".fcId())) { t, c -> c.scrollBarBackground = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_down".fcId())) { t, c -> c.scrollBarDown = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_down_highlighted".fcId())) { t, c -> c.scrollBarDownHighlighted = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_up".fcId())) { t, c -> c.scrollBarUp = t }
                    .sequence(ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_up_highlighted".fcId())) { t, c -> c.scrollBarUpHighlighted = t }
                    .build().applyValue(queue, builder)
            }
            "scroll-bar-background" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller_background".fcId())
                ) { t, c -> c.scrollBarBackground = t }
            }
            "scroll-bar-scroller" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller".fcId())
                ) { t, c -> c.scrollBarScroller = t }
            }
            "scroll-bar-scroller-highlighted" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroller_highlighted".fcId())
                ) { t, c -> c.scrollBarHighlighted = t }
            }
            "scroll-bar-down" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_down".fcId())
                ) { t, c -> c.scrollBarDown = t }
            }
            "scroll-bar-down-highlighted" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_down_highlighted".fcId())
                ) { t, c -> c.scrollBarDownHighlighted = t }
            }
            "scroll-bar-up" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_up".fcId())
                ) { t, c -> c.scrollBarUp = t }
            }
            "scroll-bar-up-highlighted" -> {
                builder.applyValue(
                    queue,
                    ValueBuilders.identifierValueBuilder(fallback = "widget/scroll/vanilla/scroll_up_highlighted".fcId())
                ) { t, c -> c.scrollBarUpHighlighted = t }
            }
            else -> {
                ValidationResult.error(Optional.empty(), Errors.INVALID_DECL) { b -> b.message("Scroll Bar").content(decl) }
            }
        }
    }

    override fun builder(): ScrollBarDeclarationCreator {
        return ScrollBarDeclarationCreator()
    }

    override fun defaultValue(): ScrollBar {
        return ScrollBar()
    }

    val ALIASES = arrayOf(
        "scroll-bar-width",
        "scroll-bar-button-height",
        "scroll-bar-type",
        "scroll-bar-fixed-height",
        "scroll-bar-button-type",
        "scroll-bar-show-on-hover-only",
        "scroll-bar-texture",
        "scroll-bar-background",
        "scroll-bar-scroller",
        "scroll-bar-scroller-highlighted",
        "scroll-bar-down",
        "scroll-bar-down-highlighted",
        "scroll-bar-up",
        "scroll-bar-up-highlighted")
}