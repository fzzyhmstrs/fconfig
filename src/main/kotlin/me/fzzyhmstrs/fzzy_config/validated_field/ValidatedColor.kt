/*
package me.fzzyhmstrs.fzzy_config.validated_field

import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import net.minecraft.util.math.ColorHelper
import java.awt.Color

*/
/**
 * A pre-built [ConfigSection] that stores a color value in RGBA format
 *
 * Each component of the color is individually validated via a [ValidatedInt]. Provides helper functions to get the color as a single int (0xFFFFFFFF) or as a Java [Color]. Individual color components can also be queried of course, via `myColor.r.get()` (for example with red).
 *
 * @param defaultR Int. The default red component of the color, 0-255.
 * @param defaultG Int. The default green component of the color, 0-255.
 * @param defaultB Int. The default blue component of the color, 0-255.
 * @param defaultA Int, optional. The default alpha/transparency component of the color, 0-255. If not provided, the color will be completely opaque, and the only valid value for `a` will be 255. If provided, 255 is opaque, 0 is transparent.
 * @param headerText Header, optional. use to provide a descriptive header to this color section. If left out, will be blank.
 * @param decorator LineDecorating, optional. If not provided, [LineDecorator.DEFAULT](me.fzzyhmstrs.fzzy_config.config_util.ReadMeBuilder.LineDecorator.DEFAULT) will be used.
 *
 * @exception IllegalArgumentException if the
 * *//*

open class ValidatedColor(
    defaultR: Int,
    defaultG: Int,
    defaultB: Int,
    defaultA: Int = Int.MIN_VALUE)
    :
    ConfigSection()
{

    init{
        if(defaultR<0 || defaultR>255) throw IllegalArgumentException("Red portion of validated color not provided a default value between 0 and 255")
        if(defaultG<0 || defaultG>255) throw IllegalArgumentException("Green portion of validated color not provided a default value between 0 and 255")
        if(defaultB<0 || defaultB>255) throw IllegalArgumentException("Blue portion of validated color not provided a default value between 0 and 255")
        if((defaultA<0 && defaultA!=Int.MIN_VALUE) || defaultA>255) throw IllegalArgumentException("Transparency portion of validated color not provided a default value between 0 and 255")
    }

    var r = ValidatedInt(defaultR,255,0)
    var g = ValidatedInt(defaultG,255,0)
    var b = ValidatedInt(defaultB,255,0)
    var a = if(defaultA != Int.MIN_VALUE){
        ValidatedInt(defaultA,255,0)
    } else {
        ValidatedInt(255,255,255)
    }

    */
/**
     * Helper method to get this ValidatedColor as a color int in ARGB format
     *
     * @return Int in ARGB format (0xFFAA5500)
     *//*

    fun getAsInt(): Int{
        return ColorHelper.Argb.getArgb(a.get(),r.get(),g.get(),b.get())
    }

    */
/**
     * Helper method to get this ValidatedColor as a Java [Color] instance
     *
     * @return Color, a [Color] with the ARGB data of this ValidatedColor.
     *//*

    fun getAsColor(): Color {
        return Color(r.get(),g.get(),b.get(),a.get())
    }
}*/