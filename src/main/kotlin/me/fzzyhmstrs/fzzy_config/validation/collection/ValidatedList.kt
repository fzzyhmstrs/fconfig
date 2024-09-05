/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.collection

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.DecoratedActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice.WidgetType
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction

/**
 * a validated list
 *
 * This [ValidatedField] implements [List], so you can directly use it as if it were an immutable list
 * @param T any non-null type
 * @param defaultValue default list of values
 * @param entryHandler [Entry] used to handle individual list entries
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.lists
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField.toList
 * @see me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
 * @author fzzyhmstrs
 * @since 0.1.0
 */
open class ValidatedList<T>(defaultValue: List<T>, private val entryHandler: Entry<T, *>): ValidatedField<List<T>>(defaultValue), List<T> {

    init {
        for(thing in defaultValue) {
            if (entryHandler.validateEntry(thing, EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default List entry [$thing] not valid per entryHandler provided")
        }
    }

    /**
     * Converts this ValidatedList into [ValidatedChoice] wrapping this list as the valid choice options
     * @return [ValidatedChoice] with options based on this list's contents
     * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Translation
     * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Description: NOTE: *translation* key, not description key. This is the same base key as provided to [translationProvider]
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @author fzzyhmstrs
     * @since 0.2.0, added optional params 0.3.6
     */
    @JvmOverloads
    fun toChoices(widgetType: WidgetType = WidgetType.POPUP, translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) }, descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit("") }): ValidatedChoice<T> {
        return ValidatedChoice(defaultValue, entryHandler, translationProvider, descriptionProvider, widgetType)
    }
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<List<T>> {
        return try {
            val array = toml.asTomlArray()
            val list: MutableList<T> = mutableListOf()
            val errors: MutableList<String> = mutableListOf()
            for ((index, el) in array.content.withIndex()) {
                val result = entryHandler.deserializeEntry(el, errors, "$fieldName[$index]", 1).report(errors)
                if (!result.isError()) {
                    list.add(index, result.get())
                }
            }
            if (errors.isNotEmpty()) {
                ValidationResult.error(list, "Error(s) encountered while deserializing list, some entries were skipped: $errors")
            } else {
                ValidationResult.success(list)
            }
        } catch (e: Exception) {
            ValidationResult.error(defaultValue, "Critical error enountered while deserializing list [$fieldName], using defaults.")
        }
    }
    @Internal
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    override fun serialize(input: List<T>): ValidationResult<TomlElement> {
        val toml = TomlArrayBuilder()
        val errors: MutableList<String> = mutableListOf()
        try {
            for (entry in input) {
                val tomlEntry = entryHandler.serializeEntry(entry, errors, 1)
                val annotations = if (entry != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(entry!!::class)
                    } catch (e: Exception) {
                        listOf()
                    }
                else
                    listOf()
                toml.element(tomlEntry, annotations)
            }
        } catch (e: Exception) {
            return ValidationResult.error(toml.build(), "Critical error encountered while serializing list: ${e.localizedMessage}")
        }
        return ValidationResult.predicated(toml.build(), errors.isEmpty(), errors.toString())
    }
    @Internal
    override fun correctEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val list: MutableList<T> = mutableListOf()
        val errors: MutableList<String> = mutableListOf()
        for (entry in input) {
            val result = entryHandler.correctEntry(entry, type)
            list.add(result.get())
            if (result.isError()) errors.add(result.getError())
        }
        return ValidationResult.predicated(list, errors.isEmpty(), "Errors corrected in list: $errors")
    }
    @Internal
    override fun validateEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val errors: MutableList<String> = mutableListOf()
        for (entry in input) {
            val result = entryHandler.validateEntry(entry, type)
            if (result.isError()) errors.add(result.getError())
        }
        return ValidationResult.predicated(input, errors.isEmpty(), "Errors found in list: $errors")
    }

    /**
     * Creates a deep copy of the stored value and returns it
     * @return List&lt;T&gt; - copy of the currently stored list
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun copyStoredValue(): List<T> {
        return storedValue.toList()
    }

    /**
     * creates a deep copy of this ValidatedList
     * return ValidatedList wrapping a deep copy of the currently stored list and passes the entry handler
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedList<T> {
        return ValidatedList(copyStoredValue(), entryHandler)
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is List<*>) return false
        return try {
            validateEntry(input as List<T>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Exception) {
            false
        }
    }
    @Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<List<T>>): ClickableWidget {
        return DecoratedActiveButtonWidget("fc.validated_field.list".translate(), 110, 20, TextureIds.DECO_LIST, {true}, { b: ActiveButtonWidget -> openListEditPopup(b) })
    }

    @Suppress("UNCHECKED_CAST")
    @Environment(EnvType.CLIENT)
    private fun openListEditPopup(b: ActiveButtonWidget) {
        try {
            val list = storedValue.map {
                (entryHandler.instanceEntry() as Entry<T, *>).also { entry -> entry.accept(it) }
            }
            val listWidget = ListListWidget(list, entryHandler) { _, _ -> ChoiceValidator.any() }
            val popup = PopupWidget.Builder(this.translation())
                .addElement("list", listWidget, Position.BELOW, Position.ALIGN_LEFT)
                .addDoneButton()
                .onClose { this.setAndUpdate(listWidget.getList()) }
                .positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
                .positionY(PopupWidget.Builder.popupContext { h -> b.y + b.height/2 - h/2 })
                .build()
            PopupWidget.push(popup)
        } catch (e: Exception) {
            FC.LOGGER.error("Unexpected exception caught while opening list popup")
        }
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

    companion object {

        /**
         * attempts to create a ValidatedList from the provided list and Entry
         *
         * This is utilized by [me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager] to create ValidatedLists reflectively
         * @param T List type
         * @param list input List<T>
         * @param entry Entry of *any* type. Will attempt to cast it to a properly-typed Entry, or fail soft to null
         * @return [ValidatedList] nullable wrapping the list and entry provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        internal fun <T> tryMake(list: List<T>, entry: Entry<*, *>): ValidatedList<T>? {
            return try {
                ValidatedList(list, entry as Entry<T, *>)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * creates a Validated Integer List
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Int>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(list: List<Int>): ValidatedList<Int> {
            return ValidatedList(list, ValidatedInt())
        }
        /**
         * creates a Validated Integer List
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param i vararg int inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(vararg i: Int): ValidatedList<Int> {
            return ValidatedList(listOf(*i.toTypedArray()), ValidatedInt())
        }

        /**
         * creates a Validated Byte List
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Byte>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(list: List<Byte>): ValidatedList<Byte> {
            return ValidatedList(list, ValidatedByte())
        }
        /**
         * creates a Validated Byte List
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param b vararg byte inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(vararg b: Byte): ValidatedList<Byte> {
            return ValidatedList(listOf(*b.toTypedArray()), ValidatedByte())
        }

        /**
         * creates a Validated Short List
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Short>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(list: List<Short>): ValidatedList<Short> {
            return ValidatedList(list, ValidatedShort())
        }
        /**
         * creates a Validated Short List
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param s vararg short inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(vararg s: Short): ValidatedList<Short> {
            return ValidatedList(listOf(*s.toTypedArray()), ValidatedShort())
        }

        /**
         * creates a Validated Long List
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Long>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(list: List<Long>): ValidatedList<Long> {
            return ValidatedList(list, ValidatedLong())
        }
        /**
         * creates a Validated Long List
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param l vararg long inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(vararg l: Long): ValidatedList<Long> {
            return ValidatedList(listOf(*l.toTypedArray()), ValidatedLong())
        }

        /**
         * creates a Validated Double List
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param list input List<Double>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(list: List<Double>): ValidatedList<Double> {
            return ValidatedList(list, ValidatedDouble())
        }
        /**
         * creates a Validated Double List
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param d vararg double inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(vararg d: Double): ValidatedList<Double> {
            return ValidatedList(listOf(*d.toTypedArray()), ValidatedDouble())
        }

        /**
         * creates a Validated Float List
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param list input List<Float>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(list: List<Float>): ValidatedList<Float> {
            return ValidatedList(list, ValidatedFloat())
        }
        /**
         * creates a Validated Float List
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param f vararg float inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(vararg f: Float): ValidatedList<Float> {
            return ValidatedList(listOf(*f.toTypedArray()), ValidatedFloat())
        }

        /**
         * creates a Validated Enum List
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param list input List<Enum>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(list: List<T>): ValidatedList<T> {
            return ValidatedList(list, T::class.java.validated())
        }
        /**
         * creates a Validated Enum List
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param e vararg Enum inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(vararg e: T): ValidatedList<T> {
            return ValidatedList(listOf(*e), T::class.java.validated())
        }


        /**
         * creates a Validated String List
         *
         * Allows any string value, repeating elements allowed
         * @param list input List<String>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(list: List<String>): ValidatedList<String> {
            return ValidatedList(list, ValidatedString())
        }
        /**
         * creates a Validated String List
         *
         * Allows any string value, repeating elements allowed
         * @param s vararg string inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(vararg s: String): ValidatedList<String> {
            return ValidatedList(listOf(*s), ValidatedString())
        }
    }


}