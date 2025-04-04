/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor.Companion.validatedColor
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.item.AxeItem
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.function.Function

object ValidatedMiscExamples {

    fun booleans() {
        //example validated boolean. It's pretty straightforward, and in general it's recommended to use the shorthand
        val validatedBool = ValidatedBoolean(true)

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun choices() {
        //fully defined validated choice, defining a set of valid ints (which happen to be the enchantment weights from the old Enchantment.Rarity enum.
        val validatedChoice = ValidatedChoice(1, listOf(1, 2, 5, 10), ValidatedInt(1, 10, 1), ValidatedChoice.WidgetType.CYCLING)

        //validated choice that uses "default" as its default choice automatically, and is defaulting to using the popup widget
        val validatedChoiceDefault = ValidatedChoice(listOf("default", "rare", "abundant"), ValidatedString())

        //validated choices built from a validated list instance.
        val validatedChoiceList = ValidatedList.ofString("default", "rare", "abundant").toChoices()

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun colors() {
        //example validated color. defined with standard integer RGBA color components [0-225]
        //this example has transparency enabled. To allow only opaque colors, use the RGB overload or input Int.MIN_VALUE
        val validatedColor = ValidatedColor(255, 128, 0, 255)

        //this validated color allows opaque colors only
        val validatedColorOpaque = ValidatedColor(0, 128, 255)

        //this validated color allows opaque colors only
        val validatedColorSimple = ValidatedColor()

        //Validated color built from a java Color. This color will not allow transparency
        val validatedColorColor = ValidatedColor(Color(1f, 0.5f, 0f), false)

        //validated color built from a hex string, with transparency enabled.
        val validatedColorString = "D6FF00AA".validatedColor(true)

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun colorClasses() {
        //generate a color holder from a ValidatedColor wrapper
        val validatedColor: ValidatedColor = ValidatedColor(255, 128, 0, 255)

        // the wrapped ColorHolder
        val holder: ValidatedColor.ColorHolder = validatedColor.get()

        //we can get color values from it
        val r: Int = holder.r //255
        val g: Int = holder.g //128
        val argb: Int = holder.argb()

        //color holders are immutable, we can mutate them via a MutableColor though
        val mutable: ValidatedColor.MutableColor = holder.mutable()

        //update the RGB to something new
        mutable.updateRGB(128, 0, 128)

        //the colors HSL is automatically updated too
        val h = mutable.h
        val l = mutable.l

        // once we are done, we can update our ValidatedColor
        validatedColor.validateAndSet(mutable.createHolder())
    }

    //example enum class used in the validated enum below
    //Note the implementation of EnumTranslatable, not required, but strongly recommended
    enum class TestEnum: EnumTranslatable {
        VERY,
        COOL,
        ENUM;
        override fun prefix(): String {
            return "my.config.test_enum"
        }
    }

    fun conditions() {
        //any validation can be wrapped in conditions. All the conditions need to pass, otherwise a fallback will be returned instead of the stored value
        // conditions should supply live values. Validated fields are a convenient mechanism to do that. A plain boolean won't update in-GUI until changes are applied.
        val validatedBooleanGate = ValidatedBoolean()

        //create a conditional validation with toCondition
        val validatedConditionInt = ValidatedInt(5, 100, 0).toCondition(validatedBooleanGate, "Gate must be true".lit()) { 0 }
    }

    // example validated Enum. COOL is the default value. This enum is going to use a Cycling style of widget for the GUI, much like vanilla. This is optional.
    val validatedEnum = ValidatedEnum(TestEnum.COOL, ValidatedEnum.WidgetType.CYCLING)

    fun expressions() {

        // example validated Expression; automatically parses and caches the Math Expression input in string form.
        // The user can input any equation they like as long as it uses x, y, both, or neither expected variables passed in the set
        val validatedExpression = ValidatedExpression("2.5 * x ^ 2 - 45 * y", setOf('x', 'y'))

        fun evalExpressionExample() {
            val vars = mapOf('x' to 2.0, 'y' to 10.0) //prepared variable map with the current values of the expected vars
            val result = validatedExpression.eval(vars) // (= -440.0) straight eval() call. This can throw exceptions, so use with caution
            val resultSafe = validatedExpression.evalSafe(vars, -250.0) //when possible, use evalSafe with a fallback
        }

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun identifiers() {
        //Example validated identifier. Note that this "raw" usage of the constructor is not recommended in most cases.
        //For instance, in this case, an implementation of ofRegistry(Registry, BiPredicate) would be advisable
        val validatedIdentifier = ValidatedIdentifier(Identifier("oak_planks"), AllowableIdentifiers({ id -> id.toString().contains("planks") }, { Registries.BLOCK.ids.filter { it.toString().contains("planks") } }))

        //Unbounded validated Identifier. Any valid Identifier will be allowed
        val unboundedIdentifier = ValidatedIdentifier(Identifier("nether_star"))

        //Unbounded validated Identifier directly from string. Any valid Identifier will be allowed
        val stringIdentifier = ValidatedIdentifier("nether_star")

        //Unbounded validated Identifier directly from string nbamespace and path. Any valid Identifier will be allowed
        val stringStringIdentifier = ValidatedIdentifier("minecraft", "nether_star")

        //Unbounded validated Identifier with a dummy default. used only for validation of other things
        val emptyIdentifier = ValidatedIdentifier()

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun pairs() {

        //example validated pair. This is the long form constructor version, pairTo is convenient shorthand
        val validatedPair = ValidatedPair(ValidatedPair.Tuple(5, "Hi"), ValidatedInt(5, 10, 0), ValidatedString())

        //validated pair made with shorthand method
        val validatedPairShort = ValidatedInt(5, 10, 0).pairWith(ValidatedString())
    }

    fun strings() {
        //example validated string. This is built using the Builder, which is typically recommended except in special circumstances
        //this string requires that lowercase chicken be included in the string
        val validatedString = ValidatedString.Builder("chickenfrog")
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

        //string validated with regex. provides entry correction in the form of stripping invalid characters from the input string, leaving only the valid regex matching sections
        //the regex provided in this example matches to Uppercase characters. AbCdE would fail validation, and would correct to ACE.
        val regexString = ValidatedString("ABCDE", "\\p{Lu}")

        //Unbounded validated string. Any valid string will be allowed
        val unboundedString = ValidatedString("hamsters")

        //Empty validated string. Any valid string will be allowed, and the default value is ""
        val emptyString = ValidatedString()

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun anys() {
        //example POJO for use in validation. It follows the same rules as sections and configs (public non-final properties, validated or not)
        class ExampleAny {
            var exampleInt = 4
            var exampleDouble = 0.4
            var exampleTag = ValidatedTagKey(ItemTags.AXES) { id -> listOf(ItemTags.AXES.id, ItemTags.SWORDS.id).contains(id) }
        }

        // wraps a plain object (that implements Walkable) into validation and serialization
        var validatedExampleAny = ValidatedAny(ExampleAny())

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun registries() {
        //example simple validation of items. Any item in the Registries.ITEM registry will be valid.
        //Type: ValidatedField<Item>
        var validatedItem = ValidatedRegistryType.of(Registries.ITEM)

        //in a more complex example, we can filter down. Maybe only swords from your mod (we'll pretend your mod is minecraft for this example)
        //Type: ValidatedField<Item>
        var validatedItemComplex = ValidatedRegistryType.of(Items.WOODEN_SWORD, Registries.ITEM) { id, re -> id.namespace == "minecraft" && re.value() is AxeItem }

        //Since we know all the outputs will be swords, we could map it down to provide SwordItem outputs
        //Type: ValidatedField<SwordItem>
        var validatedItemSword = ValidatedRegistryType.of(Items.WOODEN_SWORD, Registries.ITEM) { id, re ->
            id.namespace == "minecraft" && re.value() is AxeItem
        }.map(
            { item -> item as AxeItem },
            Function.identity()
        )
    }
}