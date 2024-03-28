package me.fzzyhmstrs.fzzy_config.validation.list

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray

/**
 * Validated List of Identifiers
 *
 * This [ValidatedField] implements [List], so you can directly sue it as if it were an immutable list
 * This is separate from a ValidatedList<Identifier> because [ValidatedIdentifier] is actually an [Entry]<String> under the hood. This class works directly with Identifier lists, instead of needing to abstract them to String lists.
 * @param defaultValue the default Identifier list
 * @param entryHandler [ValidatedIdentifier] handling the entry validation
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.validatedIdList]
 * @see [me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedList]
 * @see [me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedTag]
 * @see [me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedRegistry]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ValidatedIdentifierList(defaultValue: List<Identifier>, private val entryHandler: ValidatedIdentifier): ValidatedField<List<Identifier>>(defaultValue), List<Identifier> {

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<List<Identifier>> {
        return try{
            val array = toml.asTomlArray()
            val list: MutableList<Identifier> = mutableListOf()
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

    override fun serialize(input: List<Identifier>): ValidationResult<TomlElement> {
        val toml = TomlArrayBuilder()
        val errors: MutableList<String> = mutableListOf()
        try {
            for (entry in input) {
                val tomlEntry = entryHandler.serializeEntry(entry, errors, true)
                val annotations = ConfigApiImpl.tomlAnnotations(entry::class.java.kotlin)
                toml.element(tomlEntry, annotations)
            }
        } catch (e: Exception){
            return ValidationResult.error(toml.build(),"Critical error encountered while serializing list: ${e.localizedMessage}")
        }
        return ValidationResult.predicated(toml.build(), errors.isEmpty(), errors.toString())
    }

    override fun correctEntry(input: List<Identifier>, type: EntryValidator.ValidationType): ValidationResult<List<Identifier>> {
        val list: MutableList<Identifier> = mutableListOf()
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

    override fun validateEntry(input: List<Identifier>, type: EntryValidator.ValidationType): ValidationResult<List<Identifier>> {
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

    override fun copyStoredValue(): List<Identifier> {
        return storedValue.toList()
    }

    override fun instanceEntry(): Entry<List<Identifier>> {
        return ValidatedIdentifierList(defaultValue, entryHandler)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<List<Identifier>>): ClickableWidget {
        TODO("Not yet implemented")
    }

    // List Interface
    //////////////////////////////////
    override val size: Int
        get() = storedValue.size

    override fun get(index: Int): Identifier {
        return storedValue[index]
    }

    override fun isEmpty(): Boolean {
        return storedValue.isEmpty()
    }

    override fun iterator(): Iterator<Identifier> {
        return storedValue.iterator()
    }

    override fun listIterator(): ListIterator<Identifier> {
        return storedValue.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<Identifier> {
        return storedValue.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Identifier> {
        return storedValue.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: Identifier): Int {
        return storedValue.lastIndexOf(element)
    }

    override fun indexOf(element: Identifier): Int {
        return storedValue.indexOf(element)
    }

    override fun containsAll(elements: Collection<Identifier>): Boolean {
        return storedValue.containsAll(elements)
    }

    override fun contains(element: Identifier): Boolean {
        return storedValue.contains(element)
    }
}