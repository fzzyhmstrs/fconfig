package me.fzzyhmstrs.fzzy_config.validated_field_v2.list

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validated_field_v2.ValidatedField
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray

class ValidatedList<T: Any>(defaultValue: List<T>, private val entryHandler: Entry<T>): ValidatedField<List<T>>(defaultValue), List<T> {

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<List<T>> {
        return try{
            val array = toml.asTomlArray()
            val list: MutableList<T> = mutableListOf()
            val errors: MutableList<String> = mutableListOf()
            for ((index, el) in array.content.withIndex()){
                val result = entryHandler.deserializeEntry(el, fieldName + "[$index]")
                if (result.isError()){
                    errors.add("Skipping list element!: ${result.getError()}")
                } else {
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

    override fun serializeEntry(input: List<T>): TomlElement {
        val toml = TomlArrayBuilder()
        for (entry in input){
            val tomlEntry = entryHandler.serializeEntry(entry)
            val annotations = ConfigApiImpl.tomlAnnotations(entry::class.java.kotlin)
            toml.element(tomlEntry, annotations)
        }
        return toml.build()
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
        return ValidatedList(defaultValue, entryHandler)
    }

    override fun widgetEntry(): ClickableWidget {
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
}