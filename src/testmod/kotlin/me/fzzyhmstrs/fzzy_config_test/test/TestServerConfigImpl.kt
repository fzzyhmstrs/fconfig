/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.config.Config

import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import net.minecraft.util.Identifier

class TestServerConfigImpl: Config(Identifier.of("fzzy_config_test","server_config")) {

    var bl1 = true
    var bl2 = ValidatedBoolean()

    var int1 = 6
    var int2 = ValidatedInt(6, 10, 1, ValidatedNumber.WidgetType.TEXTBOX_WITH_BUTTONS)

    override fun saveType(): SaveType {
        return SaveType.SEPARATE
    }
}