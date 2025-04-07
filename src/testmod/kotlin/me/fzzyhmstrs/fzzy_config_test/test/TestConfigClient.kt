/*
* Copyright (c) 2024-5 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test.test

import com.google.common.base.Suppliers
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.screen.widget.SuppliedTextWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config_test.JavaTestConfig
import me.fzzyhmstrs.fzzy_config_test.JavaTestConfig2
import net.minecraft.client.MinecraftClient

object TestConfigClient {

    fun init() {
        println("I registered my config")
    }

    //var testConfig = ConfigApi.registerAndLoadConfig({ TestConfigImpl() }, RegisterType.CLIENT)
    //var testConfig3 = ConfigApi.registerAndLoadConfig({ TestConfigImpl3() }, RegisterType.CLIENT)

    //var javaConfig = ConfigApi.registerAndLoadConfig({ JavaTestConfig() }, RegisterType.CLIENT)

}