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
import java.awt.Color.green
import java.awt.Color.red

class CssColor private constructor(private val content: Color): CssElement {

    companion object {
        fun of(color: Color): CssColor {
            return CssColor(color)
        }
        fun of(a: Int, r: Int, g: Int, b: Int): CssColor {
            return CssColor(Color(r, g, b, a))
        }
    }

    override fun isBoolean(): Boolean {
        return false
    }

    override fun isString(): Boolean {
        return false
    }

    override fun isNumber(): Boolean {
        return true
    }

    override fun isColor(): Boolean {
        return true
    }

    override fun isList(): Boolean {
        return false
    }

    override fun isMap(): Boolean {
        return false
    }

    override fun asBoolean(): Boolean {
        throw UnsupportedOperationException("Not a boolean")
    }

    override fun asString(): String {
        throw UnsupportedOperationException("Not a stringr")
    }

    override fun asNumber(): Number {
        return content.rgb
    }

    override fun asColor(): Color {
        return content
    }

    override fun asList(): List<CssElement> {
        throw UnsupportedOperationException("Not a list")
    }

    override fun asMap(): Map<String, CssElement> {
        throw UnsupportedOperationException("Not a map")
    }
}