/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.json5

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseTokenizerType
import me.fzzyhmstrs.fzzy_config.theme.parsing.json5.tokens.NullProducer
import me.fzzyhmstrs.fzzy_config.theme.parsing.json5.tokens.StringProducer
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType

object Json5Type: ParseTokenizerType {

    val OPEN_ARRAY = TokenType<Char>("Array Open", this)
    val CLOSE_ARRAY = TokenType<Char>("Array Close", this)
    val OPEN_OBJECT = TokenType<Char>("Object Open", this)
    val CLOSE_OBJECT = TokenType<Char>("Object Close", this)
    val COMMA = TokenType<Char>("Comma", this)
    val BOOLEAN = TokenType<Boolean>("Boolean", this)
    val NULL = TokenType<Unit>("Null", this, raw = "null")
    val STRING = TokenType<String>("String", this, valueCreator = { s -> "\"$s\"" })
    val BAD_STRING = TokenType<String>("Bad String", this, isError = true)
    val NUMBER = TokenType<Number>("Number", this)
    val BAD_NUMBER = TokenType<Unit>("Bad Number", this, isError = true)
    val IDENT = TokenType<String>("Identifier", this)
    val BAD_IDENT = TokenType<Unit>("Bad Identifier", this, isError = true)

    init {
        Parser.addTokenizer(this, listOf(
            TokenProducer.SingleChar(OPEN_ARRAY, '[', "Open Array"),
            TokenProducer.SingleChar(CLOSE_ARRAY, ']', "Close Array"),
            TokenProducer.SingleChar(OPEN_OBJECT, '{', "Open Object"),
            TokenProducer.SingleChar(CLOSE_OBJECT, '}', "Close Object"),
            TokenProducer.SingleChar(COMMA, ',', "Comma"),
            StringProducer,


            NullProducer
        ))
    }

    override fun id(): String {
        return "JSON5"
    }

    override fun filterInput(string: String): String {
        return string
    }

    ///////////////////////////////
    /////        Utils        /////
    ///////////////////////////////

    internal fun Char.isHex(): Boolean {
        return Character.digit(this, 16) != -1
    }

    internal fun consumeEscape(c: Char, reader: StringReader, builder: StringBuilder): Boolean {
        when (c) {
            '\'' -> {
                builder.append('\'')
            }
            '"' -> {
                builder.append('"')
            }
            '\\' -> {
                builder.append('\\')
            }
            'b' -> {
                builder.append('\u0008')
            }
            'f'-> {
                builder.append('\u000C')
            }
            'n' -> {
                builder.append('\u000A')
            }
            'r' -> {
                builder.append('\u000D')
            }
            't' -> {
                builder.append('\u0009')
            }
            'v' -> {
                builder.append('\u000B')
            }
            '0' -> {
                builder.append('\u0000')
            }
            'x' -> {
                if (reader.canRead(3)) {
                    if (reader.peek(1).isHex() && reader.peek(2).isHex()) {
                        reader.skip()
                        val str = reader.read(2)
                        val code = str.toIntOrNull()
                        if (code != null && code >= 0 && code <= Char.MAX_VALUE.code) {
                            builder.append(code.toChar())
                        } else {
                            builder.append('x')
                            builder.append(str)
                        }
                    }
                } else {
                    return false
                }
            }
            'u' -> {
                if (reader.canRead(5)) {
                    if (reader.peek(1).isHex() &&
                        reader.peek(2).isHex() &&
                        reader.peek(3).isHex() &&
                        reader.peek(4).isHex())
                    {
                        reader.skip()
                        val str = reader.read(4)
                        val code = str.toIntOrNull()
                        if (code != null && code >= 0 && code <= Char.MAX_VALUE.code) {
                            builder.append(code.toChar())
                        } else {
                            builder.append('u')
                            builder.append(str)
                        }
                    }
                } else {
                    return false
                }
            }
            else -> {
                if (Character.digit(c, 10) != -1) {
                    return false
                }
                builder.append(c)
            }
        }
        return true
    }

    internal fun isValidNumber(reader: StringReader): Boolean {
        if (!reader.canRead()) return false
        val c = reader.peek()
        if (reader.canRead(2) && reader.peek() == '0' && (reader.peek(1) == 'x' || reader.peek(1) == 'X')) {
            return true
        }
        if (c.isDigit()) return true
        if (c == '+' || c == '-') {
            if (!reader.canRead(2)) return false
            val cc = reader.peek(1)
            if (cc == '.') {
                if (!reader.canRead(3)) return false
                return reader.peek(2).isDigit()
            }
            if (reader.peekFor("+Infinity") || reader.peekFor("-Infinity")) {
                return true
            }
            if (reader.canRead(3) && cc == '0' && (reader.peek(2) == 'x' || reader.peek(2) == 'X')) {
                return true
            }
            return cc.isDigit()
        } else if (c == '.') {
            if (!reader.canRead(2)) return false
            return reader.peek(1).isDigit()
        } else if (reader.peekFor("NaN")) {
            return true
        }
        return false
    }

    private val reservedWords: Array<String> = arrayOf(
        "break", "case", "catch", "continue",
        "debugger", "default", "delete", "do",
        "else", "finally", "for", "function",
        "if", "in", "instanceof", "new",
        "return", "switch", "this", "throw",
        "try", "typeof", "var", "void",
        "while", "with", "class", "const",
        "enum", "export", "extends", "import",
        "super", "null", "true", "false"
    )

    internal fun isIdentReserved(reader: StringReader): Boolean {
        for (word in reservedWords) {
            if (reader.peekFor(word)) return true
        }
        return false
    }

    internal fun isIdentStart(reader: StringReader): Boolean {
        if (!reader.canRead()) return false
        val c = reader.peek()
        return c.isLetter() ||
                c == '$' ||
                c == '_' ||
                (c == '\\' &&
                    reader.canRead(6) &&
                    reader.peek(1) == 'u' &&
                    reader.peek(2).isHex() &&
                    reader.peek(3).isHex() &&
                    reader.peek(4).isHex() &&
                    reader.peek(5).isHex())
    }

    internal fun isIdentPart(reader: StringReader): Boolean {
        if (!reader.canRead()) return false
        if (isIdentStart(reader)) return true
        val c = reader.peek()
        return Character.isUnicodeIdentifierPart(c)
    }
}