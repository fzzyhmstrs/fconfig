package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber

class TestConfigImpl: Config("test_config", "fzzy_config", "test") {

    var bl1 = true
    var bl2 = ValidatedBoolean()

    var int1 = 6
    var int2 = ValidatedInt(6,10,1)

    var enum1 = TestEnum.ALPHA
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
    }

}