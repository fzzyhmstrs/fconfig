package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.annotations.ConvertFrom
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import java.util.function.Supplier

object TestConfig {

    fun init(){
        println("I registered my config")
    }

    var testConfig = ConfigApi.registerAndLoadConfig({ TestConfigImpl() }, RegisterType.CLIENT)
    var testConfig2 = ConfigApi.registerAndLoadConfig({ TestConfigImpl2() }, RegisterType.CLIENT)
    var testConfig3 = ConfigApi.registerAndLoadConfig({ TestConfigImpl3() }, RegisterType.CLIENT)

}