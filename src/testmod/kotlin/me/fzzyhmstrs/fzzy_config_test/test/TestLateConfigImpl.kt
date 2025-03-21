/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier

class TestLateConfigImpl: Config(Identifier.of("fzzy_config_test","never_loaded_config")) {

    var int1 = 4
    var float2 = 3.4f

    override fun translation(fallback: String?): MutableText {
        return "JK I Loaded".lit()
    }

    override fun fileType(): FileType {
        return FileType.JSON
    }
}