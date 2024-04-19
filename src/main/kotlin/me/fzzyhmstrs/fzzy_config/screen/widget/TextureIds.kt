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

object TextureIds {

    val ENTRY_OK = "widget/entry_ok".fcId()
    val ENTRY_ERROR = "widget/entry_error".fcId()
    val ENTRY_ONGOING = "widget/entry_ongoing".fcId()

    val DELETE = "widget/action/delete".fcId()
    val DELETE_INACTIVE = "widget/action/delete_inactive".fcId()
    val DELETE_HIGHLIGHTED = "widget/action/delete_highlighted".fcId()
    val DELETE_LANG = "fc.button.delete".translate()

    val ADD = "widget/action/add".fcId()
    val ADD_INACTIVE = "widget/action/add_inactive".fcId()
    val ADD_HIGHLIGHTED = "widget/action/add_highlighted".fcId()
    val ADD_LANG = "fc.button.add".translate()

    val DECO_MAP = "widget/decoration/map".fcId()
    val DECO_LIST = "widget/decoration/list".fcId()

    val MAP_LANG = "fc.validated_field.map".translate()
    val MAP_ARROW = ">".lit()
}