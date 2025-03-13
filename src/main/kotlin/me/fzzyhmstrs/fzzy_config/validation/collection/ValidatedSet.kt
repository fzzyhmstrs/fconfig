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
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
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
 * a validated set
 *
 * This [ValidatedField] implements [Set], so you can directly use it as if it were an immutable list
 * @param T any non-null type
 * @param defaultValue default set of values
 * @param entryHandler [Entry] used to handle individual set entries
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.sets
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ValidatedSet<T>(defaultValue: Set<T>, private val entryHandler: Entry<T, *>): ValidatedField<Set<T>>(defaultValue), Set<T>, EntryOpener {

    init {
        for(thing in defaultValue) {
            if (entryHandler.validateEntry(thing, EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default Set entry [$thing] not valid per entryHandler provided")
        }
        compositeFlags(entryHandler)
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Set<T>> {
        return try {
            val array = toml.asTomlArray()
            val set: MutableSet<T> = mutableSetOf()
            val errors: MutableList<String> = mutableListOf()
            for ((index, el) in array.content.withIndex()) {
                val result = entryHandler.deserializeEntry(el, errors, "$fieldName[$index]", 1).report(errors)
                if (!result.isError()) {
                    set.add(result.get())
                }
            }
            ValidationResult.predicated(set, errors.isEmpty(), "Error(s) encountered while deserializing set, some entries were skipped: $errors")
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, "Critical error encountered while deserializing set [$fieldName], using defaults.")
        }
    }

    @Internal
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    override fun serialize(input: Set<T>): ValidationResult<TomlElement> {
        val toml = TomlArrayBuilder()
        val errors: MutableList<String> = mutableListOf()
        try {
            for (entry in input) {
                val tomlEntry = entryHandler.serializeEntry(entry, errors, 1)
                val annotations = if (entry != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(entry!!::class)
                    } catch (e: Throwable) {
                        emptyList()
                    }
                else
                    emptyList()
                toml.element(tomlEntry, annotations)
            }
        } catch (e: Throwable) {
            return ValidationResult.error(toml.build(), "Critical error encountered while serializing set: ${e.localizedMessage}")
        }
        return ValidationResult.predicated(toml.build(), errors.isEmpty(), errors.toString())
    }

    @Internal
    @Suppress("SafeCastWithReturn", "UNCHECKED_CAST")
    override fun deserializedChanged(old: Any?, new: Any?): Boolean {
        old as? Set<T> ?: return true
        new as? Set<T> ?: return true
        if (old.size != new.size) return true
        val o = old.toList()
        val n = new.toList()
        for ((index, e) in o.withIndex()) {
            val e2 = n[index]
            if (entryHandler.deserializedChanged(e, e2)) return true
        }
        return false
    }

    @Internal
    override fun correctEntry(input: Set<T>, type: EntryValidator.ValidationType): ValidationResult<Set<T>> {
        val set: MutableSet<T> = mutableSetOf()
        val errors: MutableList<String> = mutableListOf()
        for (entry in input) {
            val result = entryHandler.correctEntry(entry, type)
            set.add(result.get())
            if (result.isError()) errors.add(result.getError())
        }
        return ValidationResult.predicated(set, errors.isEmpty(), "Errors corrected in set: $errors")
    }

    @Internal
    override fun validateEntry(input: Set<T>, type: EntryValidator.ValidationType): ValidationResult<Set<T>> {
        val errors: MutableList<String> = mutableListOf()
        for (entry in input) {
            val result = entryHandler.validateEntry(entry, type)
            if (result.isError()) errors.add(result.getError())
        }
        return ValidationResult.predicated(input, errors.isEmpty(), "Errors found in set: $errors")
    }

    /**
     * creates a deep copy of this ValidatedSet
     * return ValidatedSet wrapping a deep copy of the currently stored set and passes the entry handlers
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedSet<T> {
        return ValidatedSet(copyStoredValue(), entryHandler)
    }

    @Internal
    @Suppress("UNCHECKED_CAST")
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is Set<*>) return false
        return try {
            validateEntry(input as Set<T>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input Set&lt;[T]%gt; input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: Set<T>): Set<T> {
        return input.toSet()
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Set<T>>): ClickableWidget {
        return CustomButtonWidget.builder(TextureIds.SET_LANG) { b: CustomButtonWidget ->
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
        val clear = ContextAction.Builder("fc.validated_field.set.clear".translate()) { p ->
            Popups.openConfirmPopup(p, "fc.validated_field.set.clear.desc".translate()) { this.accept(emptySet()) }
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
            val choiceValidator: BiFunction<ListListWidget<T>, ListListWidget.ListEntry<T>?, ChoiceValidator<T>> = BiFunction{ ll, le ->
                ListListWidget.ExcludeSelfChoiceValidator(le) { self -> ll.getRawList(self) }
            }
            val listWidget = ListListWidget(list, entryHandler, choiceValidator)
            val popup = PopupWidget.Builder(this.translation())
                .add("list", listWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
                .addDoneWidget()
                .onClose { this.setAndUpdate(listWidget.getList().toSet()) }
                .positionX(xPosition)
                .positionY(yPosition)
                .build()
            PopupWidget.push(popup)
        } catch (e: Throwable) {
            FC.LOGGER.error("Unexpected exception caught while opening list popup")
        }
    }

    /**
     * Converts this ValidatedSet into [ValidatedChoice] wrapping this set as the valid choice options
     * @return [ValidatedChoice] with options based on this set's contents
     * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this into a text Translation. This will be the "title" of the choice widget
     * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this into a text Description: NOTE: *translation* key, not description key. This is the same base key as provided to [translationProvider]
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @author fzzyhmstrs
     * @since 0.2.0, added optional params 0.4.0
     */
    fun toChoices(widgetType: WidgetType = WidgetType.POPUP, translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) }, descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit("") }): ValidatedChoice<T> {
        return ValidatedChoice(defaultValue.toList(), entryHandler, translationProvider, descriptionProvider, widgetType)
    }

    /**
     * Converts this ValidatedSet into [ValidatedChoiceList] wrapping this set as the valid choice options
     * @param selectedChoices List&lt;[T]&gt; - The default selected choices of the resulting choice set. Can be empty.
     * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this into a text Translation. This will be the "title" of the choice widget
     * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this into a text Description: NOTE: *translation* key, not description key. This is the same base key as provided to [translationProvider]
     * @param widgetType [ValidatedChoiceList.WidgetType] defines the GUI selection type. Defaults to POPUP
     * @return [ValidatedChoiceList] with options based on this list's contents
     * @author fzzyhmstrs
     * @since 0.6.3
     */
    @JvmOverloads
    fun toChoiceList(selectedChoices: List<T> = listOf(), widgetType: ValidatedChoiceList.WidgetType = ValidatedChoiceList.WidgetType.POPUP, translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) }, descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit("") }): ValidatedChoiceList<T> {
        @Suppress("DEPRECATION")
        return ValidatedChoiceList(selectedChoices, defaultValue.toList(), entryHandler, translationProvider, descriptionProvider, widgetType)
    }

    // Set Interface //////////////////////////////////

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

    // End Set Interface //////////////////////////////

    companion object {

        /**
         * attempts to create a ValidatedSet from the provided set and Entry
         *
         * This is utilized by [me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager] to create ValidatedSets reflectively
         * @param T Set type
         * @param set input Set<T>
         * @param entry Entry of *any* type. Will attempt to cast it to a properly-typed Entry, or fail soft to null
         * @return [ValidatedSet] nullable wrapping the set and entry provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Suppress("UNCHECKED_CAST")
        internal fun <T> tryMake(set: Set<T>, entry: Entry<*, *>): ValidatedSet<T>? {
            return try {
                ValidatedSet(set, entry as Entry<T, *>)
            } catch (e: Throwable) {
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