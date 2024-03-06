package me.fzzyhmstrs.fzzy_config.validated_field_v2

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral

class ValidatedList<T,V: EntryHandler<T>>(defaultValue: List<T>, private val entryHandler: V): ValidatedField<List<T>>(defaultValue) {

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<T> {
        TODO()
    }

    override fun serializeEntry(input: T): TomlElement {
        TODO()
    }
    override fun validateAndCorrectInputs(input: T, type: ValidationType): ValidationResult<T> {
        TODO()
    }

    override fun validate(input: T, type: ValidationType): ValidationResult<T> {
        TODO()
    }

    override fun createEntry(name: Text, desc: Text): ConfigEntry {
        TODO("Not yet implemented")
    }
}
