package me.fzzyhmstrs.fzzy_config.test

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt


class TestWalkable2: Config("test_walkable","fzzy_config") {

    var testInt = ValidatedInt(20,20,10)

    var testString = ValidatedString()

    var testBoolean = ValidatedBoolean(true)

}