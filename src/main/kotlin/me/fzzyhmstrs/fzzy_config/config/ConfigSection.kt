package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.interfaces.DirtyMarkable
import me.fzzyhmstrs.fzzy_config.interfaces.DirtySerializable
import me.fzzyhmstrs.fzzy_config.interfaces.FzzySerializable
import net.peanuuutz.tomlkt.TomlElement

/**
 *
 */
open class ConfigSection()
    :
    FzzySerializable,
    DirtySerializable,
    DirtyMarkable
{

    private var dirty = false

    private val dirtyListeners: MutableList<DirtyMarkable> = mutableListOf()

    override fun markDirty() {
        dirty = true
        dirtyListeners.forEach {
            it.markDirty()
        }
    }

    override fun isDirty(): Boolean {
        return dirty
    }

    override fun addDirtyListener(listener: DirtyMarkable){
        dirtyListeners.add(listener)
    }

    override fun serialize(errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement {
        return ConfigHelper.serializeToToml(this,errorBuilder,ignoreNonSync)
    }

    override fun deserialize(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<Boolean> {
        val result = ConfigHelper.deserializeFromToml(this, toml, errorBuilder, ignoreNonSync)
        return if (result.isError()) return ValidationResult.error(true, result.getError()) else ValidationResult.success(false)
    }

    override fun toString(): String {
        return ConfigHelper.serializeConfig(this, mutableListOf())
    }
}