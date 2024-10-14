/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css

import java.awt.Color

interface CssElement {

    fun isBoolean(): Boolean
    fun isString(): Boolean
    fun isNumber(): Boolean
    fun isColor(): Boolean
    fun isList(): Boolean
    fun isMap(): Boolean

    fun asBoolean(): Boolean
    fun asString(): String
    fun asNumber(): Number
    fun asColor(): Color
    fun asList(): List<CssElement>
    fun asMap(): Map<String, CssElement>

    enum class Parsers {
        BYTE {
            override fun tryParse(input: String): CssElement? {
                TODO("Not yet implemented")
            }
        }
        ;
        abstract fun tryParse(input: String): CssElement?
    }

}