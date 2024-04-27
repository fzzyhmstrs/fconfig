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

import me.fzzyhmstrs.fzzy_config.annotations.ConvertFrom
import me.fzzyhmstrs.fzzy_config.annotations.IgnoreVisibility
import me.fzzyhmstrs.fzzy_config.annotations.RequiresRestart
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.util.Identifier

@IgnoreVisibility
@ConvertFrom("test_config3.json","fzzy_config_test")
class TestConfigImpl3: Config(Identifier("fzzy_config_test","test_config3")) {

    fun getBl1(): Boolean {
        return bl1
    }
    @RequiresRestart
    private var bl1 = true

    fun getBl2(): Boolean {
        return bl2.get()
    }
    private var bl2 = ValidatedBoolean()

    fun getInt1(): Int {
        return int1
    }
    @ValidatedInt.Restrict(0,20)
    private var int1 = 6

    fun getInt2(): Int {
        return int2.get()
    }
    @RequiresRestart
    private var int2 = ValidatedInt(6,10,1)

    /*
    {
      "bl1": false,
      "bl2": false,
      "int1": 12345,
      "int2": 1
    }
    */
}