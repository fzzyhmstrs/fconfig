package me.fzzyhmstrs.fzzy_config.validated_field.misc

import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedList
import me.fzzyhmstrs.fzzy_config.validated_field.number.*
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Internal
import java.awt.Color
import kotlin.reflect.KClass

/**
 * Shorthand extension functions for simple field types
 * @author fzzyhmstrs
 * @since 0.2.0
 */
object Shorthand {

    /**
     * Shorthand validated int
     *
     * Will be unbounded, and the number used will be the default value
     * @return [ValidatedInt] wrapping the extended plain Int
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandInt]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Int.validated(): ValidatedInt {
        return ValidatedInt(this)
    }

    /**
     * Shorthand validated byte
     *
     * Will be unbounded, and the number used will be the default value
     * @return [ValidatedByte] wrapping the extended plain Byte
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandByte]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Byte.validated(): ValidatedByte {
        return ValidatedByte(this)
    }

    /**
     * Shorthand validated double
     *
     * Will be unbounded, and the number used will be the default value
     * @return [ValidatedDouble] wrapping the extended plain Double
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandDouble]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Double.validated(): ValidatedDouble {
        return ValidatedDouble(this)
    }

    /**
     * Shorthand validated short
     *
     * Will be unbounded, and the number used will be the default value
     * @return [ValidatedShort] wrapping the extended plain Short
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandShort]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Short.validated(): ValidatedShort {
        return ValidatedShort(this)
    }

    /**
     * Shorthand validated long
     *
     * Will be unbounded, and the number used will be the default value
     * @return [ValidatedLong] wrapping the extended plain Long
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandLong]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Long.validated(): ValidatedLong {
        return ValidatedLong(this)
    }

    /**
     * Shorthand validated float
     *
     * Will be unbounded, and the number used will be the default value
     * @return ValidatedFloat wrapping the extended plain Float
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandFloat]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Float.validated(): ValidatedFloat {
        return ValidatedFloat(this)
    }

    /**
     * Shorthand validated Enum
     *
     * Enum constant used will be the default
     * @param E the enum type. instanceof [Enum] and [Translatable] ([EnumTranslatable][me.fzzyhmstrs.fzzy_config.api.EnumTranslatable] is recommended)
     * @return ValidatedEnum wrapping the extended Enum constant
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.TestEnum]
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandEnum]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <E> E.validated(): ValidatedEnum<E> where E: Enum<E>, E: Translatable {
        return ValidatedEnum(this)
    }

    /**
     * Shorthand validated Boolean
     *
     * Boolean used will be the default
     * @return ValidatedBoolean wrapping the boolean passed
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandBool]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Boolean.validated(): ValidatedBoolean {
        return ValidatedBoolean(this)
    }

    /**
     * Shorthand validated [Color]
     *
     * color values in the Color will be the defaults
     * @param transparent Boolean if this ValidatedColor accepts transparency values or not. Default false
     * @return [ValidatedColor] wrapping the Color values passed. Note that the get() of the ValidatedColor does *not* return [Color]
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandColor]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Color.validated(transparent: Boolean = false): ValidatedColor {
        return ValidatedColor(this.red, this.green, this.blue, if(transparent)this.alpha else Int.MIN_VALUE)
    }

    @JvmStatic
    fun <T: Any> List<T>.validated(handler: Entry<T>): ValidatedList<T> {
        return ValidatedList(this, handler)
    }

    @Internal
    val shorthandValidationMap = mapOf(
        java.lang.Integer::class.java to ValidatedInt(0),
        java.lang.Short::class.java to ValidatedShort(0.toShort()),
        java.lang.Byte::class.java to ValidatedByte(0.toByte()),
        java.lang.Long::class.java to ValidatedLong(0L),
        java.lang.Double::class.java to ValidatedDouble(0.0),
        java.lang.Float::class.java to ValidatedFloat(0f)
    )

    @JvmStatic
    inline fun <reified T: Number> List<T>.validated(): ValidatedList<T> {
        val entry = shorthandValidationMap[T::class.java] as? Entry<T> ?: throw IllegalStateException("Incompatible shorthand type [${T::class.java}] in List")
        return ValidatedList(this, entry)
    }
}