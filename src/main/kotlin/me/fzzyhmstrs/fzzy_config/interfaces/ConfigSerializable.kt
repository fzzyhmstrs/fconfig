package me.fzzyhmstrs.fzzy_config.interfaces

import com.google.gson.JsonElement
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult

interface ConfigSerializable{
    fun serialize(): JsonElement
    fun deserialize(json: JsonElement, fieldName: String): ValidationResult<Boolean>
}