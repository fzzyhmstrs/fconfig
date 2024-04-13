package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.annotations.ConvertFrom
import me.fzzyhmstrs.fzzy_config.annotations.RequiresRestart
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedByte
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier
import java.awt.Color

class TestConfigImpl4: Config("test_config4", "fzzy_config_test") {

    @RequiresRestart
    var bl1 = true
    var bl2 = ValidatedBoolean()

    @ValidatedInt.Restrict(0,20)
    var int1 = 6
    @RequiresRestart
    var int2 = ValidatedInt(6,10,1)

    /*
    {
      "bl1": false,
      "bl2": false,
      "int1": 12345,
      "int2": 1
    }
    */
}