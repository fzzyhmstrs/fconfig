package me.fzzyhmstrs.fzzy_config.interfacesV2

import me.fzzyhmstrs.fzzy_config.config.ValidationResult
import net.peanuuutz.tomlkt.TomlElement

interface FzzySerializable {
    fun serialize(): TomlElement
    fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Boolean>
}