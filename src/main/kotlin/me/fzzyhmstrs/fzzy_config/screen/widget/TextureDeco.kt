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

import me.fzzyhmstrs.fzzy_config.screen.decoration.SmallSpriteDecoration
import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecoration

/**
 * Defines standard widget sprites used throughout Fzzy Config. May be useful for custom widgets, popups, etc.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
object TextureDeco {

    /** a list of map entries in square brackets */
    val DECO_MAP = SpriteDecoration(TextureIds.DECO_MAP)

    /** a list of list entries. no brackets */
    val DECO_LIST = SpriteDecoration(TextureIds.DECO_LIST)

    /** a list of entries with checkboxes on the left. Entry one is checked, 2 and 3 are not. no brackets */
    val DECO_CHOICE_LIST = SpriteDecoration(TextureIds.DECO_CHOICE_LIST)

    /** a wireframe depiction of an NbtObject */
    val DECO_OBJECT = SpriteDecoration(TextureIds.DECO_OBJECT)

    /** a simple square frame */
    val DECO_FRAME = SpriteDecoration(TextureIds.DECO_FRAME)

    /** a wireframe hammer and anvil */
    val DECO_INGREDIENT = SpriteDecoration(TextureIds.DECO_INGREDIENT)

    /** an arrow pointing to a wireframe config screen depiction */
    val DECO_OPEN_SCREEN = SpriteDecoration(TextureIds.DECO_OPEN_SCREEN)

    /** curly braces with a sword in between them */
    val DECO_TAG = SpriteDecoration(TextureIds.DECO_TAG)

    /** a wireframe locked padlock */
    val DECO_LOCKED = SpriteDecoration(TextureIds.DECO_LOCKED)

    /** a classic external link arrow */
    val DECO_LINK = SpriteDecoration(TextureIds.DECO_LINK)

    /** an exclamation mark */
    val DECO_ALERT = SpriteDecoration(TextureIds.DECO_ALERT)

    /** a question mark */
    val DECO_QUESTION = SpriteDecoration(TextureIds.DECO_QUESTION)

    /** a wireframe open book */
    val DECO_BOOK = SpriteDecoration(TextureIds.DECO_BOOK)

    /** a wireframe computer folder */
    val DECO_FOLDER = SpriteDecoration(TextureIds.DECO_FOLDER)

    /** a wireframe command line icon */
    val DECO_COMMAND = SpriteDecoration(TextureIds.DECO_COMMAND)

    /** a button icon with a mouse pointer hovering over it */
    val DECO_BUTTON_CLICK = SpriteDecoration(TextureIds.DECO_BUTTON_CLICK)

    //////////// SMALL (10x10) /////////////

    /** wireframe "copy" symbol of two pieces of paper overlapping */
    val CONTEXT_COPY = SmallSpriteDecoration(TextureIds.CONTEXT_COPY)

    /** wireframe "paste" symbol of a clipboard */
    val CONTEXT_PASTE = SmallSpriteDecoration(TextureIds.CONTEXT_PASTE)

    /** wireframe arrow pointing right */
    val CONTEXT_FORWARD = SmallSpriteDecoration(TextureIds.CONTEXT_FORWARD)

    /** wireframe "undo" symbol of an arrow lopping left to right and pointing back left */
    val CONTEXT_REVERT = SmallSpriteDecoration(TextureIds.CONTEXT_REVERT)

    /** wireframe of two arrows pointing in a circular motion */
    val CONTEXT_RESTORE = SmallSpriteDecoration(TextureIds.CONTEXT_RESTORE)

    /** wireframe of a floppy disk. 10x10 */
    val CONTEXT_SAVE = SmallSpriteDecoration(TextureIds.CONTEXT_SAVE)

    /** wireframe of a magnifying glass. 10x10 */
    val CONTEXT_FIND = SmallSpriteDecoration(TextureIds.CONTEXT_FIND)
}