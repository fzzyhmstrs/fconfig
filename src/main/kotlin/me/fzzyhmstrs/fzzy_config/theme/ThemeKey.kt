/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.util.Identifier

class ThemeKey<T: Any> private constructor(private val id: Identifier, private val codec: Codec<T>, private val default: T) {

    fun provideValue(map: Map<ThemeKey<*>, *>): T? {
        return map[this] as? T
    }

    fun provideDefault(): T {
        return default
    }

    internal companion object {

        private val keys: Object2ObjectOpenHashMap<Identifier, ThemeKey<*>> = Object2ObjectOpenHashMap(64, 0.7f)

        private val codec: Codec<ThemeKey<*>> = Identifier.CODEC.comapFlatMap(
            { id -> keys[id]?.let { DataResult.success(it) } ?: DataResult.error { "Unknown theme key $id" } },
            { key -> key.id }
        )

        internal val themeMapCodec: Codec<Map<ThemeKey<*>, Any>> = Codec.dispatchedMap(codec) { key -> key.codec }

        internal fun <T: Any> createAndRegister(id: Identifier, codec: Codec<T>, default: T): ThemeKey<T> {
            if (keys.containsKey(id)) throw IllegalStateException("Duplicate theme key registered: $id")
            val key = ThemeKey(id, codec, default)
            keys[id] = key
            return key
        }

    }
}