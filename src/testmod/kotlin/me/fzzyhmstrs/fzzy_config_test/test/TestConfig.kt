package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config_test.JavaTestConfig

object TestConfig {

    fun init(){
        println("I registered my config")
    }

    var testConfig = ConfigApi.registerAndLoadConfig({ TestConfigImpl() }, RegisterType.CLIENT)
    var testConfig2 = ConfigApi.registerAndLoadConfig({ TestConfigImpl2() }, RegisterType.CLIENT)
    var testConfig3 = ConfigApi.registerAndLoadConfig({ TestConfigImpl3() }, RegisterType.CLIENT)

    var testConfig4 = ConfigApi.registerAndLoadConfig({ TestConfigImpl4() }, RegisterType.BOTH)

    var javaConfig = ConfigApi.registerAndLoadConfig({ JavaTestConfig() }, RegisterType.CLIENT)
}