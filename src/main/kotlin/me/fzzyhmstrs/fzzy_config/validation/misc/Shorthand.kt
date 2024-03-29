package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.math.Expression.Impl.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.list.ValidatedIdentifierList
import me.fzzyhmstrs.fzzy_config.validation.list.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus.Internal
import java.awt.Color
import java.util.function.BiPredicate

/**
 * Shorthand extension functions for simple field types
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Suppress("DEPRECATION")
object Shorthand {

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
    fun <E: Enum<E>> E.validated(): ValidatedEnum<E> {
        return ValidatedEnum(this)
    }

    @JvmStatic
    fun <E: Enum<E>> Class<E>.validated(): ValidatedEnum<E> {
        return ValidatedEnum(this.enumConstants[0])
    }

    /**
     * Shorthand validated Identifier
     *
     * Boolean used will be the default
     * @return ValidatedBoolean wrapping the boolean passed
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandBool]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Identifier.validated(): ValidatedIdentifier{
        return ValidatedIdentifier(this)
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
    fun Color.validated(transparent: Boolean = true): ValidatedColor {
        return ValidatedColor(this.red, this.green, this.blue, if(transparent || this.alpha < 255) this.alpha else Int.MIN_VALUE)
    }

    /**
     * Shorthand validated color, based on a color int
     *
     * The number used will be the default color value
     * @return [ValidatedColor] wrapping the int-based color default
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandColorInt]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun Int.validatedColor(transparent: Boolean = true): ValidatedColor {
        return Color(this).validated(transparent)
    }

    /**
     * Shorthand validated List
     *
     * list used will be the default list
     * @param handler [Entry] for handling the list values.
     * @return [ValidatedList] wrapping the list and provided handler
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandList]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Any> List<T>.validated(handler: Entry<T>): ValidatedList<T> {
        return ValidatedList(this, handler)
    }

    /**
     * Shorthand Validated Identifier List
     *
     * List used will be the default
     * @param handler [ValidatedIdentifier] for handling list entries
     * @return [ValidatedIdentifierList] wrapping the list and validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandIdentifierList]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    fun List<Identifier>.validated(handler: ValidatedIdentifier = ValidatedIdentifier(Identifier("air"))): ValidatedIdentifierList {
        return ValidatedIdentifierList(this, handler)
    }
    /**
     * Shorthand Validated Identifier List, validated with a tag
     *
     * List used will be the default
     * @param tagKey [TagKey] used for validating inputs.
     * @return [ValidatedIdentifierList] wrapping the list and tag validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandTagIdList]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map.")
    fun List<Identifier>.validatedTag(tagKey: TagKey<*>): ValidatedIdentifierList{
        return ValidatedIdentifierList(this, ValidatedIdentifier.ofTag(tagKey))
    }
    /**
     * Shorthand Validated Identifier List, validated with a registry
     *
     * List used will be the default
     * @param T the registry type
     * @param registry [Registry] used to validate entries
     * @return [ValidatedIdentifierList] wrapping the list and registry validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandRegistryIdList]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map.")
    fun<T: Any> List<Identifier>.validatedRegistry(registry: Registry<T>): ValidatedIdentifierList{
        return ValidatedIdentifierList(this, ValidatedIdentifier.ofRegistry(registry))
    }
    /**
     * Shorthand Validated Identifier List, validated with a predicated registry
     *
     * List used will be the default
     * @param T the registry type
     * @param registry [Registry] used to validate entries
     * @param predicate [BiPredicate]<Identifier,[RegistryEntry]>
     * @return [ValidatedIdentifierList] wrapping the list and predicated registry validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandPredicatedRegistryIdList]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map.")
    fun<T: Any> List<Identifier>.validatedRegistry(registry: Registry<T>, predicate: BiPredicate<Identifier,RegistryEntry<T>>): ValidatedIdentifierList{
        return ValidatedIdentifierList(this, ValidatedIdentifier.ofRegistry(registry, predicate))
    }
    /**
     * Shorthand Validated Identifier List, validated with a list
     *
     * List used will be the default
     * @param list [List] providing valid inputs to the list
     * @return [ValidatedIdentifierList] wrapping the list and list validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandListIdList]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map. Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
    fun List<Identifier>.validatedList(list: List<Identifier>): ValidatedIdentifierList {
        return ValidatedIdentifierList(this, ValidatedIdentifier.ofList(list))
    }

    @Internal
    val shorthandValidationMap: Map<Class<*>,ValidatedField<*>> = mapOf(
        java.lang.Integer::class.java to ValidatedInt(0),
        java.lang.Short::class.java to ValidatedShort(0.toShort()),
        java.lang.Byte::class.java to ValidatedByte(0.toByte()),
        java.lang.Long::class.java to ValidatedLong(0L),
        java.lang.Double::class.java to ValidatedDouble(0.0),
        java.lang.Float::class.java to ValidatedFloat(0f),
        java.lang.String::class.java to ValidatedString("list"),
        Identifier::class.java to ValidatedIdentifier(Identifier("air")),
        java.lang.Boolean::class.java to ValidatedBoolean(true)
    )

    internal fun basicValidation(input: Any?): ValidatedField<*>? {
        return when (input) {
            is Int -> ValidatedInt()
            is Short -> ValidatedShort()
            is Long -> ValidatedLong()
            is Byte -> ValidatedByte()
            is Double -> ValidatedDouble()
            is Float -> ValidatedFloat()
            is Boolean -> ValidatedBoolean()
            is Enum<*> -> ValidatedEnum(input)
            is Color -> input.validated()
            is Identifier -> input.validated()
            is String -> ValidatedString(input)
            else -> null
        }
    }

    /**
     * Shorthand validated number List
     *
     * list used will be the default list. Automatically provides a default handler based on the type of Number provided
     * @return [ValidatedList] wrapping the list
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandNumberList]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    inline fun <reified T: Number> List<T>.validated(): ValidatedList<T> {
        val entry = shorthandValidationMap[T::class.java] as? Entry<T> ?: throw IllegalStateException("Incompatible shorthand type [${T::class.java}] in List")
        return ValidatedList(this, entry)
    }

    @JvmStatic
    fun List<Int>.validated(): ValidatedList<Int>{
        return ValidatedList(this, ValidatedInt())
    }

    @JvmStatic
    fun List<Byte>.validated(): ValidatedList<Byte>{
        return ValidatedList(this, ValidatedByte())
    }

    @JvmStatic
    fun List<Short>.validated(): ValidatedList<Short>{
        return ValidatedList(this, ValidatedShort())
    }

    @JvmStatic
    fun List<Long>.validated(): ValidatedList<Long>{
        return ValidatedList(this, ValidatedLong())
    }

    @JvmStatic
    fun List<Double>.validated(): ValidatedList<Double>{
        return ValidatedList(this, ValidatedDouble())
    }

    @JvmStatic
    fun List<Float>.validated(): ValidatedList<Float>{
        return ValidatedList(this, ValidatedFloat())
    }

    @JvmStatic
    fun List<Boolean>.validated(): ValidatedList<Boolean>{
        return ValidatedList(this, ValidatedBoolean())
    }

    @JvmStatic
    inline fun <reified T: Enum<*>> List<T>.validated(): ValidatedList<T>{
        return ValidatedList(this, ValidatedEnum(T::class.java.enumConstants[0]))
    }

    /**
     * Shorthand Validated Identifier using the [TagKey] for validation
     *
     * Does not have a default value, so should only be for list or map validation
     * @return [ValidatedIdentifier] using the tag for validation.
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandTagIds]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map.")
    fun TagKey<*>.validatedIds(): ValidatedIdentifier{
        return ValidatedIdentifier.ofTag(this)
    }
    /**
     * Shorthand Validated Identifier using the [Registry] for validation
     *
     * Does not have a default value, so should only be for list or map validation
     * @return [ValidatedIdentifier] using the registry for validation.
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandRegistryIds]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map.")
    fun Registry<*>.validatedIds(): ValidatedIdentifier{
        return ValidatedIdentifier.ofRegistry(this)
    }
    /**
     * Shorthand Validated Identifier using the predicated [Registry] for validation
     *
     * Does not have a default value, so should only be for list or map validation
     * @param T the registry type
     * @param predicate [BiPredicate]<Identifier,[RegistryEntry]> to filter the registry id list
     * @return [ValidatedIdentifier] using the predicated registry for validation.
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandPredicatedRegistryIds]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map.")
    fun <T: Any> Registry<T>.validatedIds(predicate: BiPredicate<Identifier,RegistryEntry<T>>): ValidatedIdentifier{
        return ValidatedIdentifier.ofRegistry(this, predicate)
    }
    /**
     * Shorthand Validated Identifier using the [List] for validation
     *
     * Does not have a default value, so should only be for list or map validation
     * @return [ValidatedIdentifier] using the list for validation.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Use only for validation of a list or map. Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
    fun List<Identifier>.validatedIds(): ValidatedIdentifier{
        return ValidatedIdentifier.ofList(this)
    }
}