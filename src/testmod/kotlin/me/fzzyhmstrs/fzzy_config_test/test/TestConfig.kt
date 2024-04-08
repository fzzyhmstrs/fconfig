package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType

object TestConfig {

    fun init(){
        println("I registered my config")
    }

    var testConfig = ConfigApi.registerAndLoadConfig({ TestConfigImpl() }, RegisterType.CLIENT)

}