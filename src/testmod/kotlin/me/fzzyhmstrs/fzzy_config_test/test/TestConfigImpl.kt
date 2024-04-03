package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated

class TestConfigImpl: Config("test_config", "fzzy_config", "test") {

    var bl1 = true
    var bl2 = ValidatedBoolean(true)

    var int1 = 6
    var int2 = ValidatedInt(6,10,1)

    var enum1 = TestEnum.ALPHA
    var enum2 = TestEnum.BETA.validated()

}
