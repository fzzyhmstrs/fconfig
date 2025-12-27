/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Imbued Sorcery, a mod made for minecraft; as such it falls under the license of Imbued Sorcery.
 *
 * Imbued Sorcery is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.parser

import net.minecraft.util.StringIdentifiable
import java.util.function.Predicate

class StringReader(private val input: String, private var currentLine: Int = 1, val last: Boolean = false) {

    private var currentIndex = 0
    private var offset = 0

    fun getLine(): Int {
        return currentLine
    }

    private fun setOffset(offset: Int): StringReader {
        this.offset = offset
        return this
    }

    fun getColumn(): Int {
        return currentIndex + 1 + offset
    }

    fun isEmpty(): Boolean {
        return input.isEmpty()
    }

    fun canRead(length: Int = 1): Boolean {
        return (currentIndex + length) <= input.length
    }

    fun peek(offset: Int = 0): Char {
        return input[currentIndex + offset]
    }

    fun peekFromToFor(startIndex: Int, sequence: CharSequence): Int? {
        for (i in startIndex..(input.length - sequence.length)) {
            val chrs = input.subSequence(i, i + sequence.length)
            var match = true
            for (ii in chrs.indices) {
                if (chrs[ii] != sequence[ii]) match = false
            }
            if (match) return i
        }
        return null
    }

    fun peekFromTo(startIndex: Int, predicate: Predicate<Char>): Int? {
        for (i in startIndex..input.lastIndex) {
            val c = input[i]
            if (predicate.test(c)) return i
        }
        return null
    }

    fun peekTo(predicate: Predicate<Char>): Int? {
        return peekFromTo(currentIndex, predicate)
    }

    fun peekFor(sequence: CharSequence): Boolean {
        if (!canRead(sequence.length)) return false
        for (i in sequence.indices) {
            if (peek(i) != sequence[i]) return false
        }
        return true
    }

    fun peekToFor(sequence: CharSequence): Int? {
        return peekFromToFor(currentIndex, sequence)
    }

    fun peekTotal(test: String): Boolean {
        return currentIndex == 0 && input == test
    }

    fun <T: StringIdentifiable> peekCandidate(candidates: List<T>): T? {
        if (!canRead()) return null
        var c: List<T> = candidates.filter { canRead(it.asString().length) }
        var offset = 0
        while (canRead(offset + 1) && c.size > 1) {
            val char = peek(offset)
            c = c.filter { it.asString()[offset] == char }
            offset += 1
        }
        return c.firstOrNull()
    }

    fun peekRemainingHas(predicate: Predicate<Char>): Boolean {
        for (i in currentIndex..input.lastIndex) {
            if (predicate.test(input[i])) return true
        }
        return false
    }

    fun peekRemainingIs(predicate: Predicate<Char>): Boolean {
        for (i in currentIndex..input.lastIndex) {
            if (!predicate.test(input[i])) return false
        }
        return true
    }

    fun readTo(finalIndex: Int, action: (Char) -> Unit) {
        while (currentIndex <= finalIndex && canRead()) {
            val t = input[currentIndex++]
            action(t)
        }
    }

    fun readTo(finalIndex: Int): String {
        val b = StringBuilder()
        readTo(finalIndex, b::append)
        return b.toString()
    }

    fun read(): Char {
        return input[currentIndex++]
    }

    fun skip(len: Int = 1) {
        currentIndex += len
    }

    fun skipWhitespace() {
        while (canRead() && peek().isWhitespace()) {
            skip()
        }
    }

    fun skipIf(predicate: Predicate<Char>) {
        while (canRead() && predicate.test(peek())) {
            skip()
        }
    }

    fun complete(): String {
        val str = input.substring(currentIndex)
        currentIndex = input.length
        return str
    }

    fun subReader(finalIndex: Int, l: Boolean = this.last): StringReader {
        val str = input.substring(currentIndex, finalIndex + 1)
        val len = str.length
        val reader = StringReader(str, this.currentLine, l).setOffset(currentIndex)
        currentIndex += len
        return reader
    }

    fun subReader(l: Boolean = this.last): StringReader {
        val str = input.substring(currentIndex)
        val len = str.length
        val reader = StringReader(str, this.currentLine, l).setOffset(currentIndex)
        currentIndex += len
        return reader
    }

    /**
     * Does not advance the reader. after
     */
    fun split(): StringReader {
        val str = input.substring(currentIndex)
        return StringReader(str, this.currentLine, this.last).setOffset(currentIndex)
    }
}