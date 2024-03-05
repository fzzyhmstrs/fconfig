package me.fzzyhmstrs.fzzy_config.interfaces

import me.fzzyhmstrs.fzzy_config.config.ConfigHelper
import me.fzzyhmstrs.fzzy_config.config.ValidationResult
import net.peanuuutz.tomlkt.TomlElement

/**
 * Denotes that the implementing class has elements of its own that may be `dirty` and need de/serialization
 *
 * Internal to FzzyConfig. Used by [ConfigSection].
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 *
 */
internal interface DirtySerializable {

    fun serializeDirty(errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): TomlElement{
        return ConfigHelper.serializeDirtyToToml(this,errorBuilder, ignoreNonSync)
    }
    fun deserializeDirty(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, ignoreNonSync: Boolean = false): ValidationResult<Boolean>{
        val result = ConfigHelper.deserializeDirtyFromToml(this,toml, errorBuilder, ignoreNonSync)
        return if (result.isError()) ValidationResult.error(true, result.getError()) else ValidationResult.success(false)
    }

}