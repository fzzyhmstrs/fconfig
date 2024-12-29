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

import me.fzzyhmstrs.fzzy_config.annotations.RequiresRestart
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedExpression
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import net.minecraft.registry.tag.ItemTags
import net.minecraft.text.ClickEvent
import net.minecraft.util.Identifier
import java.awt.Color

@Version(1)
class TestConfigImpl: Config(Identifier("fzzy_config_test", "test_config"), subfolder =  "test") {

    var bl1 = true
    var bl2 = ValidatedBoolean()

    var int1 = 6
    var int2 = ValidatedInt(6, 10, 1)

    @RequiresRestart
    var enum1 = TestEnum.ALPHA
    @RequiresRestart
    var enum2 = TestEnum.BETA.validated()

    var section1 = TestSectionImpl()
    class TestSectionImpl: ConfigSection() {
        @ValidatedFloat.Restrict(-500f, 500f)
        var float1 = 1f
        @RequiresRestart
        var float2 = ValidatedFloat(3f, 6f, 1f, ValidatedNumber.WidgetType.TEXTBOX)
        var float3 = ValidatedFloat(3f, 6f, 1f)
        var string1 = "hello"
        var string2 = ValidatedString.Builder("chickenfrog")
            .both { s, _ -> ValidationResult.predicated(s, s.contains("chicken"), "String must contain the lowercase word 'chicken'.") }
            .withCorrector()
            .both { s, _ ->
                if(s.contains("chicken")) {
                    ValidationResult.success(s)
                } else {
                    if(s.contains("chicken", true)) {
                        val s2 = s.replace(Regex("(?i)chicken"), "chicken")
                        ValidationResult.error(s2, "'chicken' needs to be lowercase in the string")
                    } else {
                        ValidationResult.error(s, "String must contain the lowercase word 'chicken'")
                    }
                }
            }
            .build()

        var ingredient1 = ValidatedIngredient(Identifier("stick"))

        @RequiresRestart
        var tag1 = ValidatedTagKey(ItemTags.PICKAXES)

        var object1 = ValidatedAny(TestAny())

        class TestAny {
            var testAction = ConfigAction.Builder().title("Open Docs...".lit()).build(ClickEvent(ClickEvent.Action.OPEN_URL, "https://fzzyhmstrs.github.io/fconfig/"))
            var testAction2 = ConfigAction.Builder().title("Open Wiki...".lit()).decoration(TextureIds.DECO_BOOK).build(ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/fzzyhmstrs/fconfig/wiki/"))
            var testInt = 1
            var testValidated = ValidatedFloat(3f, 6f, 1f)
            var testValidated2 = listOf(1, 3, 5, 7).validated()
        }
    }

    var mathTest = ValidatedExpression("x + 5", setOf('x'))

    var group = ConfigGroup("test_group")

    var list1 = listOf(1, 3, 5, 7)
    var list2 = listOf(1, 3, 5, 7).validated()

    var color1 = Color(255, 128, 0).validated(true)

    var group2 = ConfigGroup("test_group_2")

    var set1 = setOf(0.2, 0.4, 0.6)
    @ConfigGroup.Pop
    var set2 = setOf(0.2, 0.4, 0.6).validated()

    val pair1 = ValidatedInt(1, 10, 0).pairWith(ValidatedInt(1, 10, 0))

    @ConfigGroup.Pop
    var map1 = mapOf(1 to "a", 2 to "c")

    var id1 = ValidatedIdentifier.ofList(Identifier.of("stick"), listOf(Identifier.of("stick"), Identifier.of("blaze_rod"), Identifier.of("coal"), Identifier.of("charcoal")))

    var choice1 = ValidatedList.ofInt(1, 2, 5, 10).toChoices()

    override fun update(deserializedVersion: Int) {
        if (deserializedVersion == 0) {
            println("I updated from version 0")
            println(int1)
            int1 /= 10
            println(int1)
        }
    }
}