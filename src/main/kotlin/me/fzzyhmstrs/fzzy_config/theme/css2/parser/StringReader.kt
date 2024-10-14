/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.parser

class StringReader(private val input: String) {

    private var currentIndex = 0
    private var currentLine = 1
    private var currentColumn = 0
    private var wasNewline = false

    fun getLine(): Int {
        return currentLine
    }

    fun getColumn(): Int {
        return currentColumn
    }

    fun canRead(length: Int = 1): Boolean {
        return (currentIndex + length) <= input.length
    }

    fun peek(offset: Int = 0): Char {
        return input[currentIndex + offset]
    }

    fun read(): Char {
        val c = input[currentIndex++]
        if (c == '\n') {
            wasNewline = true
        } else if (wasNewline) {
            wasNewline = false
            currentLine++
            currentColumn = 0
        }
        currentColumn++
        return c
    }

    fun skip() {
        currentIndex++
    }
}