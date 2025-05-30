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
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.context.ContextAction
import me.fzzyhmstrs.fzzy_config.screen.context.ContextResultBuilder
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice.WidgetType
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction
import java.util.function.Supplier

/**
 * A validated list
 *
 * This [ValidatedField] implements [List], so you can directly use it as if it were an immutable list
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Collections) for more details and examples.
 * @param T any non-null type
 * @param defaultValue default list of values
 * @param entryHandler [Entry] used to handle individual list entries
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.lists
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField.toList
 * @see me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
 * @author fzzyhmstrs
 * @since 0.1.0
 */
open class ValidatedList<T>(defaultValue: List<T>, private val entryHandler: Entry<T, *>): ValidatedField<List<T>>(defaultValue), List<T>, EntryOpener {

    init {
        for(thing in defaultValue) {
            if (entryHandler.validateEntry(thing, EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default List entry [$thing] not valid per entryHandler provided")
        }
        compositeFlags(entryHandler)
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<List<T>> {
        return try {
            val array = toml.asTomlArray()
            val list: MutableList<T> = mutableListOf()
            val errors = ValidationResult.createMutable("Error(s) found deserializing list $fieldName")
            for ((index, el) in array.content.withIndex()) {
                val result = entryHandler.deserializeEntry(el, "$fieldName[$index]", 1).attachTo(errors)
                if (result.isValid()) {
                    list.add(index, result.get())
                }
            }
            ValidationResult.ofMutable(list, errors)
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, ValidationResult.Errors.DESERIALIZATION, "Exception while deserializing list [$fieldName], using defaults", e)
        }
    }

    @Internal
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    override fun serialize(input: List<T>): ValidationResult<TomlElement> {
        val toml = TomlArrayBuilder()
        try {
            val errors = ValidationResult.createMutable("Error(s) found serializing list")
            for (entry in input) {
                val tomlEntry = entryHandler.serializeEntry(entry, 1).attachTo(errors)
                val annotations = if (entry != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(entry!!::class)
                    } catch (e: Throwable) {
                        emptyList()
                    }
                else
                    emptyList()
                toml.element(tomlEntry.get(), annotations)
            }
            return ValidationResult.ofMutable(toml.build(), errors)
        } catch (e: Throwable) {
            return ValidationResult.error(toml.build(), ValidationResult.Errors.DESERIALIZATION, "Exception while serializing list", e)
        }
    }

    @Internal
    @Suppress("SafeCastWithReturn", "UNCHECKED_CAST")
    override fun deserializedChanged(old: Any?, new: Any?): Boolean {
        old as? List<T> ?: return true
        new as? List<T> ?: return true
        if (old.size != new.size) return true
        for ((index, e) in old.withIndex()) {
            val e2 = new[index]
            if (entryHandler.deserializedChanged(e, e2)) return true
        }
        return false
    }

    @Internal
    override fun correctEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val list: MutableList<T> = mutableListOf()
        val errors = ValidationResult.createMutable("List correction found errors")
        for (entry in input) {
            val result = entryHandler.correctEntry(entry, type).attachTo(errors)
            list.add(result.get())
        }
        return ValidationResult.ofMutable(list, errors)
    }

    @Internal
    override fun validateEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val errors = ValidationResult.createMutable("List validation found errors")
        for (entry in input) {
            entryHandler.validateEntry(entry, type).attachTo(errors)
        }
        return ValidationResult.ofMutable(input, errors)
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
    @Suppress("UNCHECKED_CAST")
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is List<*>) return false
        return try {
            validateEntry(input as List<T>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input List&lt;[T]%gt; input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: List<T>): List<T> {
        return input.toList()
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<List<T>>): ClickableWidget {
        return CustomButtonWidget.builder(TextureIds.LIST_LANG) { b: CustomButtonWidget ->
            openListEditPopup(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 }, PopupWidget.Builder.popupContext { h -> b.y + b.height/2 - h/2 })
        }.size(110, 20).build()
    }

    @Internal
    override fun open(args: List<String>) {
        openListEditPopup()
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset? {
        return Decorated.DecoratedOffset(TextureDeco.DECO_LIST, 2, 2)
    }

    @Internal
    override fun contextActionBuilder(context: EntryCreator.CreatorContext): MutableMap<String, MutableMap<ContextType, ContextAction.Builder>> {
        val map = super.contextActionBuilder(context)
        val clear = ContextAction.Builder("fc.validated_field.list.clear".translate()) { p ->
            Popups.openConfirmPopup(p, "fc.validated_field.list.clear.desc".translate()) { this.accept(emptyList()) }
            true }
            .withActive { s -> Supplier { s.get() && this.isNotEmpty() } }
        map[ContextResultBuilder.COLLECTION] = mutableMapOf(ContextType.CLEAR to clear)
        return map
    }

    @Suppress("UNCHECKED_CAST")
    //client
    private fun openListEditPopup(xPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center(), yPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center()) {
        try {
            val list = storedValue.map {
                (entryHandler.instanceEntry() as Entry<T, *>).also { entry -> entry.accept(it) }
            }
            val listWidget = ListListWidget(list, entryHandler) { _, _ -> ChoiceValidator.any() }
            val popup = PopupWidget.Builder(this.translation())
                .add("list", listWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
                .addDoneWidget()
                .onClose { this.setAndUpdate(listWidget.getList()) }
                .positionX(xPosition)
                .positionY(yPosition)
                .build()
            PopupWidget.push(popup)
        } catch (e: Throwable) {
            FC.LOGGER.error("Unexpected exception caught while opening list popup")
        }
    }

    /**
     * Converts this ValidatedList into [ValidatedChoice] wrapping this list as the valid choice options
     * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Translation
     * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Description: NOTE: *translation* key, not description key. This is the same base key as provided to [translationProvider]
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @return [ValidatedChoice] with options based on this list's contents
     * @author fzzyhmstrs
     * @since 0.2.0, added optional params 0.3.6
     */
    @JvmOverloads
    fun toChoices(widgetType: WidgetType = WidgetType.POPUP, translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) }, descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit("") }): ValidatedChoice<T> {
        return ValidatedChoice(defaultValue, entryHandler, translationProvider, descriptionProvider, widgetType)
    }

    /**
     * Converts this ValidatedList into [ValidatedChoiceList] wrapping this list as the valid choice options
     * @param selectedChoices List&lt;[T]&gt; - The default selected choices of the resulting choice set. Can be empty.
     * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Translation
     * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Description: NOTE: *translation* key, not description key. This is the same base key as provided to [translationProvider]
     * @param widgetType [ValidatedChoiceList.WidgetType] defines the GUI selection type. Defaults to POPUP
     * @return [ValidatedChoiceList] with options based on this list's contents
     * @author fzzyhmstrs
     * @since 0.6.3
     */
    @JvmOverloads
    fun toChoiceList(selectedChoices: List<T> = listOf(), widgetType: ValidatedChoiceList.WidgetType = ValidatedChoiceList.WidgetType.POPUP, translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) }, descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit("") }): ValidatedChoiceList<T> {
        @Suppress("DEPRECATION")
        return ValidatedChoiceList(selectedChoices, defaultValue, entryHandler, translationProvider, descriptionProvider, widgetType)
    }

    // List Interface //////////////////////////////////

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

    // End List Interface //////////////////////////////

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
            } catch (e: Throwable) {
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