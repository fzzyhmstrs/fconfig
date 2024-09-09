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

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config_test.JavaTestConfig

object TestConfig {

    fun init() {
        println("I registered my config")
    }

    var testConfig2 = ConfigApi.registerAndLoadConfig({ TestConfigImpl2() }, RegisterType.BOTH)
    var testConfig4 = ConfigApi.registerAndLoadConfig({ TestConfigImpl4() }, RegisterType.BOTH)
}