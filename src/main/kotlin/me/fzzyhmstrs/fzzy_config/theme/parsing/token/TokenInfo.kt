/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.token

class TokenInfo(val line: Int, val column: Int, val error: String) {

    fun isError(): Boolean {
        return error.isNotEmpty()
    }

    override fun toString(): String {
        return if (error.isNotEmpty()) "l=$line, c=$column, e=$error" else "l=$line, c=$column"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TokenInfo) return false

        if (line != other.line) return false
        if (column != other.column) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = line
        result = 92821 * result + column
        result = 92821 * result + error.hashCode()
        return result
    }

    companion object {
        val EMPTY = TokenInfo(0, 0, "")
    }
}