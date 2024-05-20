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
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.Walkable
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
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlComment
import java.awt.Color

@Version(1)
class TestConfigImpl2: Config(Identifier("fzzy_config_test","test_config2")) {

    @ClientModifiable
    @WithPerms(5)
    var bl1 = true
    var bl2 = ValidatedBoolean()

    var int1 = 6
    var int2 = ValidatedInt(6,10,1)

    @WithPerms(5)
    var enum1 = TestEnum.ALPHA
    @WithPerms(5)
    var enum2 = TestEnum.BETA.validated()

    var section1 = TestSectionImpl()
    class TestSectionImpl: ConfigSection(){
        var float1 = 1f
        var float2 = ValidatedFloat(3f,6f,1f,ValidatedNumber.WidgetType.TEXTBOX)
        var float3 = ValidatedFloat(3f,6f,1f)
        var string1 = "hello"
        var string2 = ValidatedString.Builder("chickenfrog")
            .both { s,_ -> ValidationResult.predicated(s, s.contains("chicken"), "String must contain the lowercase word 'chicken'.") }
            .withCorrector()
            .both { s,_ ->
                if(s.contains("chicken")){
                    ValidationResult.success(s)
                } else {
                    if(s.contains("chicken", true)){
                        val s2 = s.replace(Regex("(?i)chicken"),"chicken")
                        ValidationResult.error(s2,"'chicken' needs to be lowercase in the string")
                    } else {
                        ValidationResult.error(s,"String must contain the lowercase word 'chicken'")
                    }
                }
            }
            .build()

        var ingredient1 = ValidatedIngredient(Identifier("diamond_axe"))

        var ingredient2 = ValidatedIngredient(setOf(Identifier("diamond_axe"),TagKey.of(RegistryKeys.ITEM,Identifier("barrels"))))

        var tag1 = ValidatedTagKey(ItemTags.PICKAXES)

        var object1 = ValidatedAny(TestAny())

        var object2 = TestAny2()

        class TestAny {
            @RequiresRestart
            var testInt = 1
            var testValidated = ValidatedFloat(3f,6f,1f)
            var testValidated2 = listOf(1,3,5,7).validated()
        }

        class TestAny2: Walkable {
            @RequiresRestart
            var testInt = 1
            var testValidated = ValidatedFloat(3f,6f,1f)
            var testValidated2 = listOf(1,3,5,7).validated()
        }
    }

    @TomlComment("Testing out a comment")
    var mathTest = ValidatedExpression("x + 5", setOf('x'))

    @NonSync
    @WithPerms(5)
    var list1 = listOf(1,3,5,7)
    var list2 = listOf(1,3,5,7).validated()

    @Comment("Testing out a comment")
    var color1 = Color(255,128,0).validated(true)

    @WithPerms(5)
    var set1 = setOf(0.2,0.4,0.6)
    var set2 = setOf(0.2,0.4,0.6).validated()

    var map1 = mapOf(1 to "a", 2 to "c")

    var id1 = ValidatedIdentifier.ofList(Identifier("stick"), listOf(Identifier("stick"),Identifier("blaze_rod"),Identifier("coal"),Identifier("charcoal")))

    var choice1 = ValidatedList.ofInt(1,2,5,10).toChoices()
}