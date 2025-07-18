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
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedChoiceList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import me.fzzyhmstrs.fzzy_config_test.FC.TEST_PERMISSION_GOOD
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.MutableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlComment
import java.awt.Color

@Version(1)
@IgnoreVisibility
class TestConfigImpl2: Config(Identifier.of("fzzy_config_test","test_config2")) {

    @ClientModifiable
    @WithPerms(5)
    var bl1 = true
    @RequiresAction(Action.RELOAD_DATA)
    var bl2 = ValidatedBoolean().toCondition({ bl3.get() }, "bl3 needs to be true".lit().formatted(Formatting.RED), { false }).withCondition({ set2.isNotEmpty() }, "Set2 can't be empty".lit().formatted(Formatting.RED)).withFailTitle("Disabled".lit(), "Super disabled".lit())
    @RequiresAction(Action.RESTART)
    var bl3 = ValidatedBoolean(false)

    var bl2Button = ConfigAction.Builder().title("bl2 Conditionally".lit()).build(Runnable { println("Conditionally:${ bl2.get() }, base: ${ bl2.getUnconditional() }") })

    @WithCustomPerms(["my_perm.custom"])
    var int1 = 6
    @WithCustomPerms([TEST_PERMISSION_GOOD])
    @RequiresAction(Action.RELOAD_BOTH)
    var int2 = ValidatedInt(6, 10, 1)

    @WithPerms(5)
    var enum1 = TestEnum.GAMMA
    @WithPerms(5)
    var enum2 = TestEnum.BETA.validated()

    var section1 = TestSectionImpl()
    class TestSectionImpl: ConfigSection() {
        var float1 = 1f
        @RequiresAction(Action.RELOG)
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
                    if(s.contains("chicken", true)){
                        val s2 = s.replace(Regex("(?i)chicken"), "chicken")
                        ValidationResult.error(s2, "'chicken' needs to be lowercase in the string")
                    } else {
                        ValidationResult.error(s, "String must contain the lowercase word 'chicken'")
                    }
                }
            }
            .build()

        var ingredient1 = ValidatedIngredient(Identifier.of("diamond_axe"))

        var ingredient2 = ValidatedIngredient(setOf(Identifier.of("diamond_axe"), TagKey.of(RegistryKeys.ITEM, Identifier.of("barrels"))))

        @RequiresAction(Action.RELOAD_DATA)
        var tag1 = ValidatedTagKey(ItemTags.PICKAXES)

        var object1 = ValidatedAny(TestAny())

        var object2 = TestAny2()

        class TestAny {
            @RequiresRestart
            var testInt = 1
            var testValidated = ValidatedFloat(3f, 6f, 1f)
            var testValidated2 = listOf(1, 3, 5, 7).validated()
        }

        class TestAny2: Walkable, Translatable {
            @RequiresRestart
            var testInt = 1
            var testValidated = ValidatedFloat(3f, 6f, 1f)
            var testValidated2 = listOf(1, 3, 5, 7).validated()

            override fun translationKey(): String {
                return ""
            }

            override fun descriptionKey(): String {
                return ""
            }

            override fun translation(fallback: String?): MutableText {
                return FcText.literal("Butthead")
            }

            override fun description(fallback: String?): MutableText {
                return FcText.literal("Beavis and Butthead")
            }

            override fun hasTranslation(): Boolean {
                return false
            }

            override fun hasDescription(): Boolean {
                return true
            }
        }
    }

    @TomlComment("Testing out a comment")
    var mathTest = ValidatedExpression("x + 5", setOf('x'))

    @RequiresAction(Action.RELOAD_RESOURCES)
    @NonSync
    @WithPerms(5)
    var list1 = listOf(1, 3, 5, 7)
    var list2 = listOf(1, 3, 5, 7).validated()

    var list2Button = ConfigAction.Builder().title("List2 Contains 9".lit()).build(Runnable { println(TestConfig.listProvider.getArgResult("fzzy_config_test.test_config2.list2?contains=9", ContainsArg)) })
    var list2Index = ConfigAction.Builder().title("List2 Index 1".lit()).build(Runnable { println(TestConfig.listProvider.getArgResult("fzzy_config_test.test_config2.list2?index=1", IndexArg(-666))) })
    var list2Process = ConfigAction.Builder().title("List2 Process".lit()).build(Runnable { TestConfig.listProvider.processArgResults("fzzy_config_test.test_config2.list2?contains=9?random?index=1", ContainsArg.to { println(it) }, IndexArg(-666).to { println(it) }, RandomArg(-777).to { println(it) }) })

    @Comment("Testing out a comment")
    var color1 = Color(255, 128, 0).validated(true)

    @WithPerms(5)
    var set1 = setOf(0.2, 0.4, 0.6)
    var set2 = setOf(0.2, 0.4, 0.6).validated()

    var map1 = mapOf(1 to "a", 2 to "c")

    var id1 = ValidatedIdentifier.ofList(Identifier.of("stick"), listOf(Identifier.of("stick"), Identifier.of("blaze_rod"), Identifier.of("coal"), Identifier.of("charcoal")))

    var choice1 = ValidatedList.ofInt(1, 2, 5, 10).toChoices(translationProvider = { t, u -> ("$u.$t").translate() }, descriptionProvider = { t, u -> ("$u.$t").translate() })
    var choice2 = ValidatedList.ofInt(1, 2, 5, 10).toChoices(widgetType = ValidatedChoice.WidgetType.INLINE, translationProvider = { t, u -> ("$u.$t").translate() }, descriptionProvider = { t, u -> ("$u.$t").translate() })
    var choice3 = ValidatedList.ofInt(1, 2, 5, 10, 20, 40, 80, 160).toChoices(widgetType = ValidatedChoice.WidgetType.SCROLLABLE)
    var choice3a = ValidatedList.ofInt(1, 2, 5, 10, 20, 40).toChoices(widgetType = ValidatedChoice.WidgetType.SCROLLABLE)

    var choiceList1 = ValidatedList.ofInt(1, 2, 5, 10).toChoiceList(listOf(1, 5))
    var choiceList2 = choice1.toChoiceList(listOf(1, 5), ValidatedChoiceList.WidgetType.INLINE)
    var choiceList3 = ValidatedList.ofString("A", "B", "AB", "C", "ACDC", "1", "4").toChoiceList(listOf("A", "1"), widgetType = ValidatedChoiceList.WidgetType.SCROLLABLE)
    var choiceList3a = ValidatedList.ofString("A", "B", "AB", "C", "ACDC", "1").toChoiceList(listOf("A", "1"), widgetType = ValidatedChoiceList.WidgetType.SCROLLABLE)


    var longObject = TestAny3()

    class TestAny3: Walkable {
        @RequiresAction(Action.RELOG)
        var testInt1 = 1
        var testInt2 = 1
        var testInt3 = 1
        var testInt4 = 1
        var testInt5 = 1
        var testInt6 = 1
        var testInt7 = 1
        var testInt8 = 1
        var testInt9 = 1
        var testInt10 = 1
        var testInt11 = 1
        var testInt12 = 1
        var testInt13 = 1
        var testValidated = ValidatedFloat(3f, 6f, 1f)
        var testValidated2 = listOf(1, 3, 5, 7).validated()
    }
}