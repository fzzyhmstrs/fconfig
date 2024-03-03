package me.fzzyhmstrs.fzzy_config.interfaces

import com.google.gson.JsonElement
import me.fzzyhmstrs.fzzy_config.config.ValidationResult

/**
 * A class that inherits ConfigSerializable will be automatically serialized and deserialized via [SyncedConfigHelperV1](me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1)
 *
 * By default [ConfigSection](me.fzzyhmstrs.fzzy_config.config_util.ConfigSection) and [ValidatedField](me.fzzyhmstrs.fzzy_config.config_util.ValidatedField) are Config Serializable, so a [ConfigClass](me.fzzyhmstrs.fzzy_config.config_util.ConfigClass) built from Validated fields and sections will completely auto serialize.
 */
interface ConfigSerializable{
    fun serialize(): JsonElement
    fun deserialize(json: JsonElement, fieldName: String): ValidationResult<Boolean>
}