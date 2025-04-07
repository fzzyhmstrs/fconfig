/*
* Copyright (c) 2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.Walkable
import net.minecraft.util.Identifier

class TestConfigImplAny: Config(Identifier.of("fzzy_config_test","test_config_any")) {

    override fun saveType(): SaveType {
        return SaveType.SEPARATE
    }

    @ClientModifiable
    var any1 = MyTestAny()

    //var bl1 = true

    class MyTestAny: Walkable {
        @RequiresAction(Action.RESTART)
        var test: Int = 5
        var test2: Double = 4.5
        var test3: String = "ggg"
        var test4: Boolean = false
        var test5: TriState = TriState.DEFAULT
    }
}