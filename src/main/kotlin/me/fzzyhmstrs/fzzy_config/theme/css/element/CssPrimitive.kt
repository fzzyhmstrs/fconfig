/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css.element

import me.fzzyhmstrs.fzzy_config.theme.css.CssElement
import java.awt.Color

@JvmDefaultWithoutCompatibility
class CssPrimitive private constructor(private val content: Any): CssElement {

    companion object {
        fun of(content: Number): CssPrimitive {
            return CssPrimitive(content)
        }

        fun of(content: Boolean): CssPrimitive {
            return CssPrimitive(content)
        }

        fun of(content: String): CssPrimitive {
            return CssPrimitive(content)
        }
    }

    override fun isBoolean(): Boolean {
        return when (content) {
            is Boolean -> true
            is String -> content.lowercase().toBooleanStrictOrNull() != null
            is Number -> content.toInt() == 1 || content.toInt() == 0
            else -> false
        }
    }

    override fun isString(): Boolean {
        return true
    }

    override fun isNumber(): Boolean {
        return when (content) {
            is Number -> true
            is String -> content.toDoubleOrNull() != null
            else -> false
        }
    }

    override fun isColor(): Boolean {
        return content is Int || content is String && content.toIntOrNull() != null
    }

    override fun isList(): Boolean {
        return false
    }

    override fun isMap(): Boolean {
        return false
    }

    override fun asBoolean(): Boolean {
        return when (content) {
            is Boolean -> content
            is String -> content.lowercase().toBooleanStrict()
            is Number -> content.toInt() == 1
            else -> throw UnsupportedOperationException("Not a boolean value")
        }
    }

    override fun asString(): String {
        return content.toString()
    }

    override fun asNumber(): Number {
        return when (content) {
            is Number -> content
            is String -> {
                content.toByteOrNull() ?: content.toShortOrNull() ?: content.toIntOrNull() ?: content.toLongOrNull() ?: content.toFloatOrNull() ?: content.toDoubleOrNull() ?: throw UnsupportedOperationException("Not a valid number")
            }
            else -> throw UnsupportedOperationException("Not a valid number")
        }
    }

    override fun asColor(): Color {
        return when (content) {
            is Int -> Color(content)
            is String -> content.toIntOrNull()?.let { Color(it) } ?: throw UnsupportedOperationException("Not a valid color")
            else -> throw UnsupportedOperationException("Not a valid color")
        }
    }

    override fun asList(): List<CssElement> {
        throw UnsupportedOperationException("Not a list")
    }

    override fun asMap(): Map<String, CssElement> {
        throw UnsupportedOperationException("Not a map")
    }

}