/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Imbued Sorcery, a mod made for minecraft; as such it falls under the license of Imbued Sorcery.
 *
 * Imbued Sorcery is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.token

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseTokenizerType
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser

/**
 * simple interface used to make types for tokens to enable composition of tokens into result objects
 * @author fzzyhmstrs
 * @since ?.?.?
 */
class TokenType<T: Any>(val id: String, private val parseType: ParseTokenizerType, private val isError: Boolean = false, private val isWhitespace: Boolean = false, private val raw: String = "", private val valueCreator: (String) -> String = { s -> s }) {

    fun isSpecial(): Boolean = parseType == Parser.SPECIAL_TYPE
    fun isType(type: ParseTokenizerType): Boolean = parseType == type
    fun isError(): Boolean = isError
    fun isWhitespace(): Boolean = isWhitespace
    fun raw(): String = raw
    fun createValue(string: String): String = valueCreator(string)

    override fun toString(): String {
        return id
    }
}