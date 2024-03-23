package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.api.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.math.Expression
import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedList
import me.fzzyhmstrs.fzzy_config.validated_field.misc.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validated_field.misc.ValidatedEnum
import java.awt.Color

internal object ValidatedShorthands {

    //Shorthand unbounded int. The int is the default value
    val shorthandInt = 12.validated()


    //Shorthand unbounded byte. The byte is the default value
    val shorthandByte = 12.toByte().validated()


    //Shorthand unbounded byte. The byte is the default value
    val shorthandShort = 12.toShort().validated()


    //Shorthand unbounded long. The long is the default value
    val shorthandLong = 100L.validated()


    //Shorthand unbounded double. The double is the default value
    val shorthandDouble = 4.0.validated()


    //Shorthand unbounded float. The float is the default value
    val shorthandFloat = 4f.validated()

    enum class TestEnum: EnumTranslatable {
        TEST,
        MORE,
        EVEN,
        ODDS;

        override fun prefix(): String {
            return "my.config"
        }
    }

    val lang = """{
        "my.config.TEST": "Test",
        "my.config.TEST.desc": "A test description",
        "my.config.MORE": "More Testing",
        "my.config.MORE.desc": "Another test description",
        "my.config.EVEN": "Even Numbers",
        "my.config.EVEN.desc": "Only even numbers here!",
        "my.config.ODDS": "Odd Numbers",
        "my.config.ODDS.desc": "Odd numbers and nothing else!"
    }"""

    //shorthand validated Enum. the constant is the default value
    val shorthandEnum = TestEnum.MORE.validated()

    //Shorthand validated Color. The color values in the Color will be the default color components
    val shorthandColor = Color(255,255,128,255).validated()

    //Shorthand boolean. the bool used is the default
    val shorthandBool = true.validated()

    //Shorthand math Expression. This is directly in the Expression class itself, not in the Shorthand object
    val shorthandMath = Expression.validated("x * 0.5")
}