/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.context

import com.mojang.serialization.Codec
import net.minecraft.util.StringIdentifiable
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral

/**
 * Defines the type of input that triggered a context action.
 * @author fzzyhmstrs
 * @since 0.6.0, stringIdentifiable since 0.6.5
 */
enum class ContextInput(private val id: String): StringIdentifiable {
    /**
     * Context event triggered with keyboard input
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    KEYBOARD("keyboard"),
    /**
     * Context event triggered with mouse input
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    MOUSE("mouse");

    override fun asString(): String {
        return id
    }

    companion object {
        val CODEC: Codec<ContextInput> = StringIdentifiable.createCodec { ContextInput.entries.toTypedArray() }

        fun fallback(): TomlElement {
            return TomlLiteral(KEYBOARD.id)
        }
    }
}