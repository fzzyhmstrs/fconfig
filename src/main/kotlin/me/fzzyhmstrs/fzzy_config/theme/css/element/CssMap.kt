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

class CssMap private constructor(private val content: Map<String, CssElement>): CssElement {

    companion object {
        fun of(map: Map<String, CssElement>): CssMap {
            return CssMap(map)
        }
    }

    override fun isBoolean(): Boolean {
        return false
    }

    override fun isString(): Boolean {
        return false
    }

    override fun isNumber(): Boolean {
        return false
    }

    override fun isColor(): Boolean {
        return false
    }

    override fun isList(): Boolean {
        return false
    }

    override fun isMap(): Boolean {
        return true
    }

    override fun asBoolean(): Boolean {
        throw UnsupportedOperationException("Not a boolean")
    }

    override fun asString(): String {
        throw UnsupportedOperationException("Not a stringr")
    }

    override fun asNumber(): Number {
        throw UnsupportedOperationException("Not a number")
    }

    override fun asColor(): Color {
        throw UnsupportedOperationException("Not a color")
    }

    override fun asList(): List<CssElement> {
        throw UnsupportedOperationException("Not a list")
    }

    override fun asMap(): Map<String, CssElement> {
        return content
    }
}