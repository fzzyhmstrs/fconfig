package me.fzzyhmstrs.fzzy_config.validated_field_v2

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral

class ValidatedList<T>(defaultValue: List<T>, private val entryHandler: EntryHandler<T>): ValidatedField<List<T>>(defaultValue) {

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<T> {
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
            return if (errors.isNotEmpty()) {
                ValidationResult.error(list, "Error(s) encountered while deserializing list, some entries were skipped: $errors")
            } else {
                ValidationResult.success(list)
            }
        }
    }

    override fun serializeEntry(input: List<T>): TomlElement {
        val toml = TomlArrayBuilder()
        for (entry in input){
            val tomlEntry = entryHandler.serializeEntry(entry)
            val annotaions = ConfigApiImpl.tomlAnnotations(::entry)
            toml.element(tomlEntry, annotations)
        }
        return toml.build()
    }
    
    override fun correctEntry(input: List<T>, type: ValidationType): ValidationResult<List<T>> {
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

    override fun validateEntry(input: List<T>, type: ValidationType): ValidationResult<List<T>> {
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

    override fun createEntry(name: Text, desc: Text): ConfigEntry {
        TODO("Not yet implemented")
    }
}
