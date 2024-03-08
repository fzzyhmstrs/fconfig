package me.fzzyhmstrs.fzzy_config.validated_field.map

import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import net.minecraft.util.Identifier

class ValidatedMapBuilder<V: Any> {
    private var keyHandler: Entry<String>? = null
    private var valueHandler: Entry<V>? = null
    private var defaults: Map<String,V> = mapOf()

    fun keyHandler(keyHandler: Entry<String>): ValidatedMapBuilder<V>{
        this.keyHandler = keyHandler
        return this
    }

    fun valueHandler(valueHandler: Entry<V>): ValidatedMapBuilder<V>{
        this.valueHandler = valueHandler
        return this
    }

    fun defaults(defaults: Map<String,V>): ValidatedMapBuilder<V>{
        this.defaults = defaults
        return this
    }

    fun defaults(vararg defaults: Pair<String,V>): ValidatedMapBuilder<V>{
        this.defaults = mapOf(*defaults)
        return this
    }

    fun default(default: Pair<String,V>): ValidatedMapBuilder<V>{
        this.defaults = mapOf(default)
        return this
    }

    fun default(key: String, value: V): ValidatedMapBuilder<V>{
        this.defaults = mapOf(key to value)
        return this
    }

    fun defaultIds(defaults: Map<Identifier,V>): ValidatedMapBuilder<V>{
        this.defaults = defaults.mapKeys { e -> e.key.toString() }
        return this
    }

    fun defaultIds(vararg defaults: Pair<Identifier,V>): ValidatedMapBuilder<V>{
        this.defaults = (defaults).associate { p -> Pair(p.first.toString(),p.second) }
        return this
    }

    fun defaultId(default: Pair<Identifier,V>): ValidatedMapBuilder<V>{
        this.defaults = mapOf(Pair(default.first.toString(),default.second))
        return this
    }

    fun defaultId(key: Identifier, value: V): ValidatedMapBuilder<V>{
        this.defaults = mapOf(key.toString() to value)
        return this
    }


}