/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.validation.minecraft

import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.util.Formatting
import java.util.function.UnaryOperator

class ValidatedText {

    /*
    * Text holder that stores a set of lines, each line holds 1 or more text objects, which store a single text representation instance
    *
    * Line isn't necessarily one "line" in the editor. More like a paragraph.
    *
    * Representations can be various kinds of text, Plain text, translations, keybinds, etc. Each has their own handler and is managed separately.
    * - For example, translation, keybind, etc. would have some indicator that denotes it as a translation
    * - They define how the cursor interacts with it, deletes it, selects it, etc.
    * - Also defines what styling does to it.
    * - Like translatable or keybind would apply style to the whole text fragment, while plain text you can select bits and style
    * - which would break the fragment apart into individually styled fragments that would later be appended together when a Text object is constructed
    *
    * Interfacing the UI with the text holding will be a trick, since lines can be >1 line
    * - Some sort of tracker class that knows positions of things in the widget. Like Array of Arrays of line# and X pos
    * - sort of suffix array of positions
    * - index -> LinePos
    * - LinePos -> index
    * - X -> LinePos
    * LinePos a small util class
    *
    * Cursor = Fragment(index?) + LinePos
    * */


    class Holder {
    }

    class Paragraph {

    }

    interface Fragment {
        fun delete(startPos: Int, endPos: Int): InteractResult
        fun type(chr: Char, position: Int): InteractResult
        fun format(formatting: Formatting, handler: InputHandler): InteractResult
        fun styled(startPos: Int, endPos: Int, operator: UnaryOperator<Style>, handler: InputHandler): InteractResult
        fun click(handler: InputHandler)
        fun release(handler: InputHandler)

        val content: OrderedText
        val style: Style
    }

    enum class InteractResult(val changed: Boolean) {
        DELETED(true),
        CHANGED(true),
        DEFAULT(false)
    }

    //the vertical line the item belongs to and the x position
    data class LinePos(val line: Int, val pos: Int)

    class InputHandler {

    }

}