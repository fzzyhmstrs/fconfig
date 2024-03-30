package me.fzzyhmstrs.fzzy_config.validation.collection

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.Expression.Impl.validated
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray

/**
 * a validated list
 *
 * This [ValidatedField] implements [List], so you can directly sue it as if it were an immutable list
 * @param T any non-null type
 * @param defaultValue default list of values
 * @param entryHandler [Entry] used to handle individual list entries
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.validatedList
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField.toList
 * @see me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedArray<T>(defaultValue: List<T>, private val entryHandler: Entry<T>): ValidatedField<List<T>>(defaultValue), List<T> {

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<List<T>> {
        return try{
            val array = toml.asTomlArray()
            val list: MutableList<T> = mutableListOf()
            val errors: MutableList<String> = mutableListOf()
            for ((index, el) in array.content.withIndex()){
                val result = entryHandler.deserializeEntry(el, errors, "$fieldName[$index]", true).report(errors)
                if (!result.isError()){
                    list.add(result.get())
                }
            }
            if (errors.isNotEmpty()) {
                ValidationResult.error(list, "Error(s) encountered while deserializing list, some entries were skipped: $errors")
            } else {
                ValidationResult.success(list)
            }
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Critical error enountered while deserializing list [$fieldName], using defaults.")
        }
    }

    override fun serialize(input: List<T>): ValidationResult<TomlElement> {
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

    override fun correctEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val list: MutableList<T> = mutableListOf()
        val errors: MutableList<String> = mutableListOf()
        for (entry in input){
            val result = entryHandler.correctEntry(entry, type)
            list.add(result.get())
            if (result.isError()) errors.add(result.getError())
        }
        return if (errors.isNotEmpty()){
            ValidationResult.error(list,"Errors corrected in list: $errors")
        } else {
            ValidationResult.success(list)
        }
    }

    override fun validateEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
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

    override fun copyStoredValue(): List<T> {
        return storedValue.toList()
    }

    override fun instanceEntry(): Entry<List<T>> {
        return ValidatedArray(copyStoredValue(), entryHandler)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<List<T>>): ClickableWidget {
        TODO("Not yet implemented")
    }

    // List Interface
    //////////////////////////////////
    override val size: Int
        get() = storedValue.size

    override fun get(index: Int): T {
        return storedValue[index]
    }

    override fun isEmpty(): Boolean {
        return storedValue.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return storedValue.iterator()
    }

    override fun listIterator(): ListIterator<T> {
        return storedValue.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return storedValue.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return storedValue.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: T): Int {
        return storedValue.lastIndexOf(element)
    }

    override fun indexOf(element: T): Int {
        return storedValue.indexOf(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return storedValue.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return storedValue.contains(element)
    }

    companion object{

        fun <T> tryMake(list: List<T>, validatedField: ValidatedField<*>): ValidatedArray<T>?{
            return try{
                ValidatedArray(list, validatedField as ValidatedField<T>)
            } catch (e: Exception){
                null
            }
        }

        /**
         * creates a Validated Integer List
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE
         * @param list input List<Int>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(list: List<Int>): ValidatedArray<Int> {
            return ValidatedArray(list, ValidatedInt())
        }
        /**
         * creates a Validated Integer List
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param i vararg int inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(vararg i: Int): ValidatedArray<Int> {
            return ValidatedArray(listOf(*i.toTypedArray()), ValidatedInt())
        }

        /**
         * creates a Validated Byte List
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Byte>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(list: List<Byte>): ValidatedArray<Byte> {
            return ValidatedArray(list, ValidatedByte())
        }
        /**
         * creates a Validated Byte List
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param b vararg byte inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(vararg b: Byte): ValidatedArray<Byte> {
            return ValidatedArray(listOf(*b.toTypedArray()), ValidatedByte())
        }

        /**
         * creates a Validated Short List
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Short>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(list: List<Short>): ValidatedArray<Short> {
            return ValidatedArray(list, ValidatedShort())
        }
        /**
         * creates a Validated Short List
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param s vararg short inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(vararg s: Short): ValidatedArray<Short> {
            return ValidatedArray(listOf(*s.toTypedArray()), ValidatedShort())
        }

        /**
         * creates a Validated Long List
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Long>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(list: List<Long>): ValidatedArray<Long> {
            return ValidatedArray(list, ValidatedLong())
        }
        /**
         * creates a Validated Long List
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param l vararg long inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(vararg l: Long): ValidatedArray<Long> {
            return ValidatedArray(listOf(*l.toTypedArray()), ValidatedLong())
        }

        /**
         * creates a Validated Double List
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param list input List<Double>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(list: List<Double>): ValidatedArray<Double> {
            return ValidatedArray(list, ValidatedDouble())
        }
        /**
         * creates a Validated Double List
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param d vararg double inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(vararg d: Double): ValidatedArray<Double> {
            return ValidatedArray(listOf(*d.toTypedArray()), ValidatedDouble())
        }

        /**
         * creates a Validated Float List
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param list input List<Float>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(list: List<Float>): ValidatedArray<Float> {
            return ValidatedArray(list, ValidatedFloat())
        }
        /**
         * creates a Validated Float List
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param f vararg float inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(vararg f: Float): ValidatedArray<Float> {
            return ValidatedArray(listOf(*f.toTypedArray()), ValidatedFloat())
        }

        /**
         * creates a Validated Enum List
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param list input List<Enum>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(list: List<T>): ValidatedArray<T> {
            return ValidatedArray(list, T::class.java.validated())
        }
        /**
         * creates a Validated Enum List
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param e vararg Enum inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(vararg e: T): ValidatedArray<T> {
            return ValidatedArray(listOf(*e), T::class.java.validated())
        }


        /**
         * creates a Validated String List
         *
         * Allows any string value, repeating elements allowed
         * @param list input List<String>
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(list: List<String>): ValidatedArray<String> {
            return ValidatedArray(list, ValidatedString())
        }
        /**
         * creates a Validated String List
         *
         * Allows any string value, repeating elements allowed
         * @param s vararg string inputs to construct the list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(vararg s: String): ValidatedArray<String> {
            return ValidatedArray(listOf(*s), ValidatedString())
        }


    }
}