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
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil

/**
 * Defines standard widget sprites used throughout Fzzy Config. May be useful for custom widgets, popups, etc.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
object TextureIds {

    /** a green checkmark */
    val ENTRY_OK = "widget/entry_ok".fcId()

    /** a greyscale checkmark */
    val ENTRY_OK_DISABLED = "widget/entry_ok_disabled".fcId()

    /** a red 'X' */
    val ENTRY_NO = "widget/entry_no".fcId()

    /** a greyscale 'X' */
    val ENTRY_NO_DISABLED = "widget/entry_no_disabled".fcId()

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

    /** a gray button with an "open screen" wireframe texture in the middle */
    val GOTO = "widget/action/goto".fcId()

    /** same as above but with the "inactive" button texture */
    val GOTO_INACTIVE = "widget/action/goto_inactive".fcId()

    /** same as above but with a white highlighted border */
    val GOTO_HIGHLIGHTED = "widget/action/goto_highlighted".fcId()

    val GOTO_SET = TextureSet(GOTO, GOTO_INACTIVE, GOTO_HIGHLIGHTED)
    val GOTO_LANG = "fc.button.goto".translate()

    /////////////////////////

    /** a gray button with a blue "i" bubble in the middle */
    val INFO = "widget/action/info".fcId()

    /** same as above but with the "inactive" button texture */
    val INFO_INACTIVE = "widget/action/info_inactive".fcId()

    /** same as above but with a white highlighted border */
    val INFO_HIGHLIGHTED = "widget/action/info_highlighted".fcId()

    val INFO_SET = TextureSet(INFO, INFO_INACTIVE, INFO_HIGHLIGHTED)
    val INFO_LANG = "fc.button.info".translate()

    /////////////////////////

    /** a gray button texture with the fzzy config logo in the middle */
    val CONFIG = "widget/action/config".fcId()

    /** same as above but with the "inactive" button texture */
    val CONFIG_INACTIVE = "widget/action/config_inactive".fcId()

    /** same as above but with a white highlighted border */
    val CONFIG_HIGHLIGHTED = "widget/action/config_highlighted".fcId()
    val CONFIG_LANG = "fc.button.config".translate()
    val CONFIG_INACTIVE_LANG = "fc.button.config_inactive".translate()

    ////////////////////////

    /** white wireframe box with a plus sign in the middle and cutouts in the top and bottom of the box */
    val GROUP_EXPAND = "widget/action/group_expand".fcId()
    val GROUP_EXPAND_HIGHLIGHTED = "widget/action/group_expand_highlighted".fcId()

    /** white wireframe box with a minus sign in the middle */
    val GROUP_COLLAPSE = "widget/action/group_collapse".fcId()
    val GROUP_COLLAPSE_HIGHLIGHTED = "widget/action/group_collapse_highlighted".fcId()

    /////////////////////////

    /** a list of map entries in square brackets */
    val DECO_MAP = "widget/decoration/map".fcId()

    /** a list of list entries. no brackets */
    val DECO_LIST = "widget/decoration/list".fcId()

    /** a list of entries with checkboxes on the left. Entry one is checked, 2 and 3 are not. no brackets */
    val DECO_CHOICE_LIST = "widget/decoration/choice_list".fcId()

    /** a wireframe depiction of an NbtObject */
    val DECO_OBJECT = "widget/decoration/object".fcId()

    /** a simple square frame */
    val DECO_FRAME = "widget/decoration/frame".fcId()

    /** a wireframe hammer and anvil */
    val DECO_INGREDIENT = "widget/decoration/ingredient".fcId()

    /** an arrow pointing to a wireframe config screen depiction */
    val DECO_OPEN_SCREEN = "widget/decoration/open_screen".fcId()

    /** curly braces with a sword in between them */
    val DECO_TAG = "widget/decoration/tag".fcId()

    /** a wireframe locked padlock */
    val DECO_LOCKED = "widget/decoration/locked".fcId()

    /** a classic external link arrow */
    val DECO_LINK = "widget/decoration/link".fcId()

    /** an exclamation mark */
    val DECO_ALERT = "widget/decoration/alert".fcId()

    /** a question mark */
    val DECO_QUESTION = "widget/decoration/question".fcId()

    /** a wireframe open book */
    val DECO_BOOK = "widget/decoration/book".fcId()

    /** a wireframe computer folder */
    val DECO_FOLDER = "widget/decoration/folder".fcId()

    /** a wireframe command line icon */
    val DECO_COMMAND = "widget/decoration/command".fcId()

    /** a button icon with a mouse pointer hovering over it */
    val DECO_BUTTON_CLICK = "widget/decoration/mouse".fcId()

    val LIST_LANG = "fc.validated_field.list".translate()

    val SET_LANG = "fc.validated_field.set".translate()

    val MAP_LANG = "fc.validated_field.map".translate()
    val MAP_ARROW = ">".lit()

    //////////// SMALL (10/11x10) /////////////

    /** wireframe "copy" symbol of two pieces of paper overlapping. 10x10 */
    val CONTEXT_COPY: TextureProvider = TextureSet("widget/context/copy".fcId(), "widget/context/copy_disabled".fcId(), "widget/context/copy".fcId())

    /** wireframe "paste" symbol of a clipboard. 10x10 */
    val CONTEXT_PASTE: TextureProvider = TextureSet("widget/context/paste".fcId(), "widget/context/paste_disabled".fcId(), "widget/context/paste".fcId())

    /** wireframe arrow pointing right. 10x10 */
    val CONTEXT_FORWARD: TextureProvider = TextureSet("widget/context/forward".fcId(), "widget/context/forward_disabled".fcId(), "widget/context/forward".fcId())

    /** wireframe "undo" symbol of an arrow lopping left to right and pointing back left. 10x10 */
    val CONTEXT_REVERT: TextureProvider = TextureSet("widget/context/revert".fcId(), "widget/context/revert_disabled".fcId(), "widget/context/revert".fcId())

    /** wireframe of two arrows pointing in a circular motion. 10x10 */
    val CONTEXT_RESTORE: TextureProvider = TextureSet("widget/context/restore".fcId(), "widget/context/restore_disabled".fcId(), "widget/context/restore".fcId())

    /** wireframe of a floppy disk. 10x10 */
    val CONTEXT_SAVE: TextureProvider = TextureSet("widget/context/save".fcId(), "widget/context/save_disabled".fcId(), "widget/context/save".fcId())

    /** wireframe of a magnifying glass. 10x10 */
    val CONTEXT_FIND: TextureProvider = TextureSet("widget/context/find".fcId(), "widget/context/find_disabled".fcId(), "widget/context/find".fcId())

    /** up arrow inside standard MC button texturing. 11x10 */
    val INCREMENT_UP = "widget/scroll/increment_up".fcId()

    /** highlighted up arrow inside standard MC button texturing. 11x10 */
    val INCREMENT_UP_HIGHLIGHTED = "widget/scroll/increment_up_highlighted".fcId()

    /** greyed-out up arrow inside standard MC button texturing. 11x10 */
    val INCREMENT_UP_DISABLED = "widget/scroll/increment_up_disabled".fcId()

    /** down arrow inside standard MC button texturing. 11x10 */
    val INCREMENT_DOWN = "widget/scroll/increment_down".fcId()

    /** highlighted down arrow inside standard MC button texturing. 11x10 */
    val INCREMENT_DOWN_HIGHLIGHTED = "widget/scroll/increment_down_highlighted".fcId()

    /** greyed-out down arrow inside standard MC button texturing. 11x10 */
    val INCREMENT_DOWN_DISABLED = "widget/scroll/increment_down_disabled".fcId()

    init {
        val smolBg = RenderUtil.Background(0, 0, 11, 10)
        RenderUtil.addBackground("widget/scroll/increment_up".fcId(), smolBg)
        RenderUtil.addBackground("widget/scroll/increment_up_highlighted".fcId(), smolBg)
        RenderUtil.addBackground("widget/scroll/increment_up_disabled".fcId(), smolBg)
        RenderUtil.addBackground("widget/scroll/increment_down".fcId(), smolBg)
        RenderUtil.addBackground("widget/scroll/increment_down_highlighted".fcId(), smolBg)
        RenderUtil.addBackground("widget/scroll/increment_down_disabled".fcId(), smolBg)
    }
}