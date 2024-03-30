package me.fzzyhmstrs.fzzy_config.validation.collection

import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray

/**
 * a validated list
 *
 * This [ValidatedField] implements [Set], so you can directly use it as if it were an immutable list
 * @param T any non-null type
 * @param defaultValue default set of values
 * @param entryHandler [Entry] used to handle individual set entries
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.KeyEnum
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.validatedSet
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ValidatedSet<T>(defaultValue: Set<T>, private val entryHandler: Entry<T,*>): ValidatedField<Set<T>>(defaultValue), Set<T> {

    init {
        for(thing in defaultValue){
            if (entryHandler.validateEntry(thing,EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default Set entry [$thing] not valid per entryHandler provided")
        }
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Set<T>> {
        return try{
            val array = toml.asTomlArray()
            val set: MutableSet<T> = mutableSetOf()
            val errors: MutableList<String> = mutableListOf()
            for ((index, el) in array.content.withIndex()){
                val result = entryHandler.deserializeEntry(el, errors, "$fieldName[$index]", true).report(errors)
                if (!result.isError()){
                    set.add(result.get())
                }
            }
            if (errors.isNotEmpty()) {
                ValidationResult.error(set, "Error(s) encountered while deserializing list, some entries were skipped: $errors")
            } else {
                ValidationResult.success(set)
            }
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Critical error enountered while deserializing list [$fieldName], using defaults.")
        }
    }

    override fun serialize(input: Set<T>): ValidationResult<TomlElement> {
        val toml = TomlArrayBuilder()
        val errors: MutableList<String> = mutableListOf()
        try {
            for (entry in input) {
                val tomlEntry = entryHandler.serializeEntry(entry, errors, true)
                val annotations = if (entry != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(entry!!::class)
                    } catch (e: Exception){
                        listOf()
                    }
                else
                    listOf()
                toml.element(tomlEntry, annotations)
            }
        } catch (e: Exception){
            return ValidationResult.error(toml.build(),"Critical error encountered while serializing list: ${e.localizedMessage}")
        }
        return ValidationResult.predicated(toml.build(), errors.isEmpty(), errors.toString())
    }

    override fun correctEntry(input: Set<T>, type: EntryValidator.ValidationType): ValidationResult<Set<T>> {
        val set: MutableSet<T> = mutableSetOf()
        val errors: MutableList<String> = mutableListOf()
        for (entry in input){
            val result = entryHandler.correctEntry(entry, type)
            set.add(result.get())
            if (result.isError()) errors.add(result.getError())
        }
        return if (errors.isNotEmpty()){
            ValidationResult.error(set,"Errors corrected in list: $errors")
        } else {
            ValidationResult.success(set)
        }
    }

    override fun validateEntry(input: Set<T>, type: EntryValidator.ValidationType): ValidationResult<Set<T>> {
        val errors: MutableList<String> = mutableListOf()
        for (entry in input){
            val result = entryHandler.validateEntry(entry, type)
            if (result.isError()) errors.add(result.getError())
        }
        return if (errors.isNotEmpty()){
            ValidationResult.error(input,"Errors corrected in list: $errors")
        } else {
            ValidationResult.success(input)
        }
    }

    override fun copyStoredValue(): Set<T> {
        return storedValue.toSet()
    }

    override fun instanceEntry(): ValidatedSet<T> {
        return ValidatedSet(copyStoredValue(), entryHandler)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<Set<T>>): ClickableWidget {
        TODO("Not yet implemented")
    }

    // List Interface
    //////////////////////////////////
    override val size: Int
        get() = storedValue.size

    override fun isEmpty(): Boolean {
        return storedValue.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return storedValue.iterator()
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return storedValue.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return storedValue.contains(element)
    }

    companion object{

        /**
         * attempts to create a ValidatedSet from the provided set and Entry
         *
         * This is utilized by [me.fzzyhmstrs.fzzy_config.updates.UpdateManagerImpl] to create ValidatedSets reflectively
         * @param T Set type
         * @param set input Set<T>
         * @param entry Entry of *any* type. Will attempt to cast it to a properly-typed Entry, or fail soft to null
         * @return [ValidatedSet] nullable wrapping the set and entry provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <T> tryMake(set: Set<T>, entry: Entry<*,*>): ValidatedSet<T>?{
            return try{
                ValidatedSet(set, entry as Entry<T,*>)
            } catch (e: Exception){
                null
            }
        }

        /**
         * creates a Validated Integer Set
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE
         * @param set input Set<Int>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(set: Set<Int>): ValidatedSet<Int> {
            return ValidatedSet(set, ValidatedInt())
        }
        /**
         * creates a Validated Integer Set
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE
         * @param i vararg int inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(vararg i: Int): ValidatedSet<Int> {
            return ValidatedSet(setOf(*i.toTypedArray()), ValidatedInt())
        }

        /**
         * creates a Validated Byte Set
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE
         * @param set input Set<Byte>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(set: Set<Byte>): ValidatedSet<Byte> {
            return ValidatedSet(set, ValidatedByte())
        }
        /**
         * creates a Validated Byte Set
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE
         * @param b vararg byte inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(vararg b: Byte): ValidatedSet<Byte> {
            return ValidatedSet(setOf(*b.toTypedArray()), ValidatedByte())
        }

        /**
         * creates a Validated Short Set
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE
         * @param set input Set<Short>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(set: Set<Short>): ValidatedSet<Short> {
            return ValidatedSet(set, ValidatedShort())
        }
        /**
         * creates a Validated Short Set
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE
         * @param s vararg short inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(vararg s: Short): ValidatedSet<Short> {
            return ValidatedSet(setOf(*s.toTypedArray()), ValidatedShort())
        }

        /**
         * creates a Validated Long Set
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE
         * @param set input Set<Long>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(set: Set<Long>): ValidatedSet<Long> {
            return ValidatedSet(set, ValidatedLong())
        }
        /**
         * creates a Validated Long Set
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE
         * @param l vararg long inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(vararg l: Long): ValidatedSet<Long> {
            return ValidatedSet(setOf(*l.toTypedArray()), ValidatedLong())
        }

        /**
         * creates a Validated Double Set
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param set input Set<Double>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(set: Set<Double>): ValidatedSet<Double> {
            return ValidatedSet(set, ValidatedDouble())
        }
        /**
         * creates a Validated Double Set
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param d vararg double inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(vararg d: Double): ValidatedSet<Double> {
            return ValidatedSet(setOf(*d.toTypedArray()), ValidatedDouble())
        }

        /**
         * creates a Validated Float Set
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param set input Set<Float>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(set: Set<Float>): ValidatedSet<Float> {
            return ValidatedSet(set, ValidatedFloat())
        }
        /**
         * creates a Validated Float Set
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param f vararg float inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(vararg f: Float): ValidatedSet<Float> {
            return ValidatedSet(setOf(*f.toTypedArray()), ValidatedFloat())
        }

        /**
         * creates a Validated Enum Set
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param set input Set<Enum>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(set: Set<T>): ValidatedSet<T> {
            return ValidatedSet(set, T::class.java.validated())
        }
        /**
         * creates a Validated Enum Set
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param e vararg Enum inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(vararg e: T): ValidatedSet<T> {
            return ValidatedSet(setOf(*e), T::class.java.validated())
        }

        /**
         * creates a Validated String Set
         *
         * Allows any string value, repeating elements allowed
         * @param set input Set<String>
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(set: Set<String>): ValidatedSet<String> {
            return ValidatedSet(set, ValidatedString())
        }
        /**
         * creates a Validated String Set
         *
         * Allows any string value, repeating elements allowed
         * @param s vararg string inputs to construct the set
         * @return [ValidatedSet] from the set provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(vararg s: String): ValidatedSet<String> {
            return ValidatedSet(setOf(*s), ValidatedString())
        }


    }
}