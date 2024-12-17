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

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecorated
import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecoration
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate

/**
 * Defines standard widget sprites used throughout Fzzy Config. May be useful for custom widgets, popups, etc.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
object TextureDeco {

    /** a green checkmark */
    val ENTRY_OK = "widget/entry_ok".fcId()

    /** a red triangular sign with an exclamation mark in the middle */
    val ENTRY_ERROR = "widget/entry_error".fcId()

    /** an animated sprite depicting a pencil writing*/
    val ENTRY_ONGOING = "widget/entry_ongoing".fcId()

    /////////////////////////

    /** a gray button texture with a red minus sign in the middle */
    val DELETE = "widget/action/delete".fcId()

    /** same as above but with the "inactive" button texture */
    val DELETE_INACTIVE = "widget/action/delete_inactive".fcId()

    /** same as above but with a white highlighted border */
    val DELETE_HIGHLIGHTED = "widget/action/delete_highlighted".fcId()
    val DELETE_LANG = "fc.button.delete".translate()

    /////////////////////////

    /** a gray button with a green plus sign in the middle */
    val ADD = "widget/action/add".fcId()

    /** same as above but with the "inactive" button texture */
    val ADD_INACTIVE = "widget/action/add_inactive".fcId()

    /** same as above but with a white highlighted border */
    val ADD_HIGHLIGHTED = "widget/action/add_highlighted".fcId()
    val ADD_LANG = "fc.button.add".translate()

    /////////////////////////

    /** a gray button texture with the fzzy config logo in the middle */
    val CONFIG = "widget/action/config".fcId()

    /** same as above but with the "inactive" button texture */
    val CONFIG_INACTIVE = "widget/action/config_inactive".fcId()

    /** same as above but with a white highlighted border */
    val CONFIG_HIGHLIGHTED = "widget/action/config_highlighted".fcId()
    val CONFIG_LANG = "fc.button.config".translate()
    val CONFIG_INACTIVE_LANG = "fc.button.config_inactive".translate()

    /////////////////////////

    /** a list of map entries in square brackets */
    val DECO_MAP = SpriteDecoration(TextureIds.DECO_MAP)

    /** a list of list entries. no brackets */
    val DECO_LIST = SpriteDecoration(TextureIds.DECO_LIST)

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

    val MAP_LANG = "fc.validated_field.map".translate()
    val MAP_ARROW = ">".lit()
}