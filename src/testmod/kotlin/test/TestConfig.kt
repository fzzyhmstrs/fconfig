package test

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType

object TestConfig {

    var testConfig = ConfigApi.registerAndLoadConfig({ TestConfigImpl() }, RegisterType.CLIENT)

}