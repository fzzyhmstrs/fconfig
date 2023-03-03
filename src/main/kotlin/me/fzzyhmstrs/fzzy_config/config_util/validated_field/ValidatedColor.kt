package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.ConfigSection
import me.fzzyhmstrs.fzzy_config.config_util.ReadMeText
import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.awt.Color
import java.util.function.Predicate

open class ValidatedColor(
    defaultR: Int,
    defaultG: Int,
    defaultB: Int,
    defaultA: Int = Int.MIN_VALUE,
    headerText: Header = Header(),
    decorator: LineDecorator = LineDecorator.DEFAULT)
    :
    ConfigSection(headerText,decorator)
{
    
    init{
        if(defaultR<0 || defaultR>255) throw IllegalArgumentException("Red portion of validated color not provided a default value between 0 and 255")
        if(defaultG<0 || defaultG>255) throw IllegalArgumentException("Green portion of validated color not provided a default value between 0 and 255")
        if(defaultB<0 || defaultB>255) throw IllegalArgumentException("Blue portion of validated color not provided a default value between 0 and 255")
        if((defaultA<0 && defaultA!=Int.MIN_VALUE) || defaultA>255) throw IllegalArgumentException("Transparency portion of validated color not provided a default value between 0 and 255")
    }
    
    @ReadMeText("fc.config.validated_color_r")
    var r = ValidatedInt(defaultR,255,0)
    @ReadMeText("fc.config.validated_color_g")
    var g = ValidatedInt(defaultG,255,0)
    @ReadMeText("fc.config.validated_color_b")
    var b = ValidatedInt(defaultB,255,0)
    @ReadMeText("fc.config.validated_color_a")
    var a = if(defaultA != Int.MIN_VALUE){ 
        ValidatedInt(defaultA,255,0)
    } else {
        ValidatedInt(255,255,255)
    }
    
    fun getAsInt():Int{
        return (r.get() shl 16) + (g.get() shl 8) + b.get()
    }
    
    fun getAsColor(): Color {
        return Color(r.get(),g.get(),b.get(),a.get())
    }
}
