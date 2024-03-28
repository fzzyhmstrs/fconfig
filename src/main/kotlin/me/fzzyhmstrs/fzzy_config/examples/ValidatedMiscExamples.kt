package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.api.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.math.Expression.Impl.evalSafe
import me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor.Companion.validatedColor
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object ValidatedMiscExamples{

    //example validated boolean. It's pretty straightforward, and in general it's recommended to use the shorthand
    val validatedBool = ValidatedBoolean(true)

    //example validated color. defined with standard integer RGBA color components [0-225]
    //this example has transparency enabled. To allow only opaque colors, use the RGB overload or input Int.MIN_VALUE
    val validatedColor = ValidatedColor(255, 128, 0, 255)

    //this validated color allows opaque colors only
    val validatedColorOpaque = ValidatedColor(0, 128, 255)

    //validated color built from a hex string, with transparency enabled.
    val validatedColorString = "D6FF00AA".validatedColor(true)

    //example enum class used in the validated enum below
    //Note the implementation of EnumTranslatable, not required, but strongly recommended
    enum class TestEnum: EnumTranslatable {
        VERY,
        COOL,
        ENUM;
        override fun prefix(): String{
            return "my.config.test_enum"
        }
    }

    // example validated Enum. COOL is the default value. This enum is going to use a Cycling style of widget for the GUI, much like vanilla. This is optional.
    val validatedEnum = ValidatedEnum(TestEnum.COOL, ValidatedEnum.WidgetType.CYCLING)

    // example validated Expression; automatically parses and caches the Math Expression input in string form.
    // The user can input any equation they like as long as it uses x, y, both, or neither expected variables passed in the set
    val validatedExpression = ValidatedExpression("2.5 * x ^ 2 - 45 * y", setOf('x', 'y'))

    fun evalExpression() {
        fun evalExpressionExample() {
            val vars = mapOf('x' to 2.0, 'y' to 10.0) //prepared variable map with the current values of the expected vars
            val result = validatedExpression.eval(vars) //straight eval() call. This can throw exceptions, so use with caution
            val resultSafe = validatedExpression.evalSafe(vars, 25.0) //when possible, use evalSafe with a fallback
        }
    }

    //Example validated identifier. Note that this "raw" usage of the constructor is not recommended in most cases.
    //For instance, in this case, an implementation of ofRegistry(Registry, BiPredicate) would be advisable
    val validatedIdentifier = ValidatedIdentifier(Identifier("oak_planks"), AllowableIdentifiers({ id -> id.toString().contains("planks") }, { Registries.BLOCK.ids.filter { it.toString().contains("planks") } }))

    //Unbounded validated Identifier. Any valid Identifier will be allowed
    val unboundedIdentifier = ValidatedIdentifier(Identifier("nether_star"))

    //Unbounded validated Identifier directly from string. Any valid Identifier will be allowed
    val stringIdentifier = ValidatedIdentifier("nether_star")

    //Unbounded validated Identifier directly from string nbamespace and path. Any valid Identifier will be allowed
    val stringStringIdentifier = ValidatedIdentifier("minecraft","nether_star")

    //example validated string. This is built using the Builder, which is typically recommended except in special circumstances
    //this string requires that lowercase chicken be included in the string
    val validatedString = ValidatedString.Builder("chickenfrog")
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

    //Unbounded validated string. Any valid string will be allowed
    val unboundedString = ValidatedString("hamsters")

    //Empty validated string. Any valid string will be allowed, and the default value is ""
    val emptyString = ValidatedString()
}