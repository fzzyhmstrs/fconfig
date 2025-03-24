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

import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import me.fzzyhmstrs.fzzy_config_test.FC
import net.minecraft.util.Identifier
import java.awt.Color

@RootConfig
class TestRootConfigImpl: Config(Identifier.of("fzzy_config_test","root_config"), subfolder =  "test") {

    override fun fileType(): FileType {
        return FileType.TOML
    }

    var bl1 = true

    var int1 = 6
    var int2 = ValidatedInt(6, 10, 1, ValidatedNumber.WidgetType.TEXTBOX_WITH_BUTTONS)

    @RequiresAction(Action.RELOAD_DATA)
    var enum1 = TestEnum.ALPHA

    var section1Button = ConfigAction.Builder().title("Open Section Object".lit()).build(Runnable { ConfigApi.openScreen("fzzy_config_test.test_config.section1.object1"); FC.LOGGER.info("Tried opening object") })

    var section1 = TestSectionImpl()
    class TestSectionImpl: ConfigSection() {
        @ValidatedFloat.Restrict(-500f, 500f)
        var float1 = 1f
        @RequiresRestart
        var float2 = ValidatedFloat(3f, 6f, 1f, ValidatedNumber.WidgetType.TEXTBOX)
        var float3 = ValidatedFloat(3f, 6f, 1f)
        var string1 = "hello"
    }

    var mathTest = ValidatedExpression("x + 5", setOf('x'))

    var einsteinMap = ValidatedMap.Builder<ValidatedIngredient.IngredientProvider, ValidatedColor.ColorHolder>().keyHandler(ValidatedIngredient(Identifier.of("stick"))).valueHandler(ValidatedColor()).build()

    var group = ConfigGroup("test_group", collapsedByDefault = true)

    var list1 = listOf(1, 3, 5, 7)
    var list2 = listOf(1, 3, 5, 7).validated()

    var color1 = Color(255, 128, 0).validated(false)

    var group2 = ConfigGroup("test_group_2")

    var set1 = setOf(0.2, 0.4, 0.6)
    @ConfigGroup.Pop
    @ConfigGroup.Pop
    var set2 = setOf(0.2, 0.4, 0.6).validated()
}