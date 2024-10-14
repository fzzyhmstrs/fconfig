/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.token

import me.fzzyhmstrs.fzzy_config.theme.css2.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.css2.token.tokens.*
import kotlin.math.pow

enum class TokenType {
    COMMENT {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead(2) && reader.peek() == '/' && reader.peek(1) == '*'
        }

        override fun consume(reader: StringReader): CssToken? {
            var lastRead = ' '
            while (reader.canRead()) {
                val read = reader.read()
                if (lastRead == '*' && read == '/') break
                lastRead = read
            }
            return null
        }
    },
    WHITESPACE {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek().isWhitespace()
        }

        override fun consume(reader: StringReader): CssToken {
            while(reader.canRead() && reader.peek().isWhitespace()) {
                reader.skip()
            }
            return WhitespaceToken
        }

    },
    STRING {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            val builder = StringBuilder()
            val open = reader.read() //consume the opening " or '
            var solidusFound = true
            while(reader.canRead()) {
                val c = reader.read()
                if (solidusFound) {
                    solidusFound = false
                    if (c == '\n') {
                        continue
                    }
                    builder.append(consumeEscape(c, reader))
                    continue
                }
                when (c) {
                    '"', '\'' -> {
                        if (c == open)
                            return StringToken(builder.toString())
                        else
                            builder.append(c)
                    }
                    '\n' -> {
                        return BadStringToken(builder.toString(), startColumn, startLine, "Illegal newline")
                    }
                    '\u005C' -> {
                        solidusFound = true
                    }
                    else -> {
                        builder.append(c)
                    }
                }
            }
            return BadStringToken(builder.toString(), startColumn, startLine, "Unterminated string value; EOF reached")
        }
    },
    HASH {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '#'
        }

        override fun consume(reader: StringReader): CssToken {
            val hash = reader.read()
            if (!reader.canRead()) return DelimToken(hash, reader.getColumn(), reader.getLine())
            if (isIdentSequenceStart(reader)) {
                val id = consumeIdent(reader)
                return HashToken(id)
            }
            return DelimToken(hash, reader.getColumn(), reader.getLine())
        }
    },
    OPEN_PARENTHESIS {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '('
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return OpenParenthesisToken
        }
    },
    CLOSE_PARENTHESIS {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == ')'
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return CloseParenthesisToken
        }
    },
    PLUS {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '+'
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            if (isValidNumber(reader)) {
                return DIGIT.consume(reader)!!
            }
            return DelimToken(reader.read(), startColumn, startLine)
        }
    },
    MINUS {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '-'
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            if (isValidNumber(reader)) {
                return DIGIT.consume(reader)!!
            } else if (reader.canRead(3) && reader.peek(1) == '-' && reader.peek(2) == '>') {
                reader.skip()
                reader.skip()
                reader.skip()
                return CDCToken
            } else if (isIdentSequenceStart(reader)) {
                return IDENT_LIKE.consume(reader)!!
            }
            return DelimToken(reader.read(), startColumn, startLine)
        }
    },
    COMMA {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == ','
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return CommaToken
        }
    },
    PERIOD {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '.'
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            if (reader.canRead(2) && reader.peek(1).isDigit()) {
                return DIGIT.consume(reader)!!
            }
            return DelimToken(reader.read(), startColumn, startLine)
        }
    },
    COLON {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == ':'
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return ColonToken
        }
    },
    SEMICOLON {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == ';'
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return SemicolonToken
        }
    },
    LESS_THAN {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '<'
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            val lessThan = reader.read()
            if (reader.canRead(3) && reader.peek() == '!' && reader.peek(1) == '-' && reader.peek(2) == '-') {
                reader.skip()
                reader.skip()
                reader.skip()
                return CDOToken
            }
            return DelimToken(lessThan, startColumn, startLine)
        }
    },
    AT {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '@'
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            val at = reader.read()
            if (isIdentSequenceStart(reader)) {
                return AtToken(consumeIdent(reader))
            }
            return DelimToken(at, startColumn, startLine)
        }
    },
    OPEN_BRACKET {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '['
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return OpenBracketToken
        }
    },
    CLOSE_BRACKET {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == ']'
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return CloseBracketToken
        }
    },
    OPEN_BRACE {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '{'
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return OpenBracketToken
        }
    },
    CLOSE_BRACE {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '}'
        }

        override fun consume(reader: StringReader): CssToken {
            reader.skip()
            return CloseBracketToken
        }
    },
    SOLIDUS {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek() == '\\'
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            if (isValidEscape(reader)) {
                val str = consumeIdent(reader)
            }
            return DelimToken('\u005C', startColumn, startLine)
        }
    },
    DIGIT {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && reader.peek().isDigit()
        }
        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            val result = consumeNumber(reader)
            val token = if (isIdentSequenceStart(reader)) {
                val unit = consumeIdent(reader)
                DimensionToken(result.number, !result.hasDecimal, unit)
            } else if (reader.canRead() && reader.peek() == '%') {
                PercentageToken(result.number, !result.hasDecimal)
            } else {
                NumberToken(result.number, !result.hasDecimal)
            }
            return if (result.isError()) {
                NumberErrorToken(token, result.error, startColumn, startLine)
            } else {
                token
            }
        }
    },
    IDENT_LIKE {
        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead() && isIdentStartCodePoint(reader.peek())
        }

        override fun consume(reader: StringReader): CssToken {
            val ident = consumeIdent(reader)
            if (ident.lowercase() == "url" && reader.canRead() && reader.peek() == '(') {
                reader.skip()
                if (!reader.canRead()) return IdentToken(ident)
                return if (reader.peek() == '"'
                    || reader.peek() == '\''
                    || reader.peek().isWhitespace() && reader.canRead(2) && reader.peek(1) == '"'
                    || reader.peek().isWhitespace() && reader.canRead(2) && reader.peek(1) == '\'') {
                    TODO("Function token with string")
                } else {
                    TODO("URL token")
                }
            } else if (reader.canRead() && reader.peek() == '(') {
                return TODO("Function token")
            } else {
                return IdentToken(ident)
            }
        }
    },
    EOF {

        override fun canConsume(reader: StringReader): Boolean {
            return !reader.canRead()
        }

        override fun consume(reader: StringReader): CssToken {
            return EOFToken
        }
    },
    OTHER {

        override fun canConsume(reader: StringReader): Boolean {
            return reader.canRead()
        }

        override fun consume(reader: StringReader): CssToken {
            val startColumn = reader.getColumn()
            val startLine = reader.getLine()
            return DelimToken(reader.read(), startColumn, startLine)
        }
    },
    CDO {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    CDC {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    DELIM {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    NUMBER {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    PERCENT {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    DIMENSION {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    IDENT {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    FUNCTION {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    URL {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    BAD_URL {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    },
    BAD_STRING {
        override fun canConsume(reader: StringReader): Boolean {
            return false
        }

        override fun consume(reader: StringReader): CssToken? {
            return null
        }
    }

    ;
    abstract fun canConsume(reader: StringReader): Boolean
    abstract fun consume(reader: StringReader): CssToken?

    companion object {
        private fun isEscapeChar(char: Char): Boolean {
            return Character.digit(char, 16) != -1
        }

        private fun isIdentStartCodePoint(char: Char): Boolean {
            return char.isLetter() || char.code >= '\u0080'.code || char == '\u005F'
        }

        private fun isIdentCodePoint(char: Char): Boolean {
            return isIdentStartCodePoint(char) || char.isDigit() || char == '\u002D'
        }

        private fun isValidEscape(reader: StringReader, offset: Int = 0): Boolean {
            return (reader.canRead(2 + offset) && reader.peek(offset) == '\\' && reader.peek(1 + offset) != '\n')
        }

        private fun isIdentSequenceStart(reader: StringReader): Boolean {
            if (!reader.canRead()) return false
            val c = reader.peek()
            if (isIdentStartCodePoint(c)) return true
            if (isValidEscape(reader)) return true
            if (c == '\u002D') {
                if(reader.canRead(2) && reader.peek(1) == '\u002D') return true
                if (isValidEscape(reader, 1)) return true
            }
            return false
        }

        private fun consumeEscape(c: Char, reader: StringReader): Char {
            if (isEscapeChar(c)) {
                val escapeBuilder = StringBuilder(8)
                escapeBuilder.append(c)
                var hexLen = 1
                while(reader.canRead() && hexLen++ < 6) {
                    val cc = reader.read()
                    if (isEscapeChar(cc)) {
                        escapeBuilder.append(cc)
                    } else if (cc.isWhitespace()) {
                        break
                    }
                }
                val escapeCode = escapeBuilder.toString().toIntOrNull() ?: return '\uFFFD'
                if (escapeCode < 0 || escapeCode > Char.MAX_VALUE.code) return '\uFFFD'
                return escapeCode.toChar()
            } else {
                return '\u005C'
            }
        }

        private fun consumeIdent(reader: StringReader): String {
            val builder = StringBuilder()
            while(reader.canRead()) {
                if (isIdentCodePoint(reader.peek())) {
                    builder.append(reader.read())
                } else if (isValidEscape(reader)) {
                    reader.skip()
                    builder.append(consumeEscape(reader.read(), reader))
                } else {
                    return builder.toString()
                }
            }
            return builder.toString()
        }

        private fun isValidNumber(reader: StringReader): Boolean {
            if (!reader.canRead()) return false
            val c = reader.peek()
            if (c.isDigit()) return true
            if (c == '+' || c == '-') {
                if (!reader.canRead(2)) return false
                val cc = reader.peek(1)
                if (cc == '.') {
                    if (!reader.canRead(3)) return false
                    return reader.peek(2).isDigit()
                }
                return cc.isDigit()
            } else if (c == '.') {
                if (!reader.canRead(2)) return false
                return reader.peek(1).isDigit()
            }
            return false
        }

        private fun consumeNumber(reader: StringReader): Result {
            var positive = true
            var expPositive = true
            var hasDecimal = false
            var hasExponent = false
            var error = ""
            val num = StringBuilder()
            val expNum = StringBuilder()

            fun isErrored(): Boolean {
                return error.isNotEmpty()
            }

            while (reader.canRead()) {
                val c = reader.read()
                if (c == '+') {
                    if (isErrored()) continue
                    positive = true
                } else if (c == '-') {
                    if (isErrored()) continue
                    positive = false
                } else if (c.isDigit()) {
                    if (isErrored()) continue
                    if (!hasExponent)
                        num.append(c)
                    else
                        expNum.append(c)
                } else if (c == '.') {
                    if (isErrored()) continue
                    if (hasDecimal) {
                        error = "Multiple decimal points in a number token"
                        continue
                    }
                    if (hasExponent) {
                        error = "Decimals not allowed in exponent part of a number"
                        continue
                    }
                    if (!reader.canRead()) return Result(0, false, "Number terminated by EOF")
                    val cc = reader.peek()
                    if (!cc.isDigit()) {
                        error = "Decimal with no fractional digits"
                        continue
                    }
                    num.append(c)
                    hasDecimal = true
                } else if (c == 'e' || c == 'E') {
                    if (isErrored()) continue
                    if (hasExponent) {
                        error = "Multiple exponents in number token"
                        continue
                    }
                    if (!reader.canRead()) return Result(0, false, "Number terminated by EOF")
                    val cc = reader.peek()
                    if (cc == '+') {
                        expPositive = true
                        if (!reader.canRead(2)) return Result(0, false, "Number terminated by EOF")
                        val ccc = reader.peek(1)
                        if (!ccc.isDigit()) return Result(0, false, "Exponent with no numeric input")
                    } else if (cc == '-') {
                        expPositive = false
                        if (!reader.canRead(2)) Result(0, false, "Number terminated by EOF")
                        val ccc = reader.peek(1)
                        if (!ccc.isDigit()) return Result(0, false, "Exponent with no numeric input")
                    } else if (!cc.isDigit()) {
                        return Result(0, false, "Exponent with no numeric input")
                    }
                    hasExponent = true
                } else {
                    break
                }
            }

            if (isErrored()) {
                return Result(0, false, error)
            }

            val number: Number = if (hasDecimal) {
                val d = num.toString().toDoubleOrNull() ?: return Result(0, false, "Unparseable number: $num")
                (if(positive) 1.0 else -1.0) * d * if(hasExponent) 10.0.pow((if (expPositive) 1.0 else -1.0) * (expNum.toString().toDoubleOrNull() ?: return Result(0, false, "Unparseable exponent: $expNum"))) else 1.0
            } else {
                val i = num.toString().toIntOrNull() ?: return Result(0, false, "Unparseable number: $num")
                (if(positive) 1 else -1) * i * (if(hasExponent) 10.0.pow((if (expPositive) 1.0 else -1.0) * (expNum.toString().toDoubleOrNull() ?: return Result(0, false, "Unparseable exponent: $expNum"))) else 1.0).toInt()
            }

            return Result(number, hasDecimal)
        }

        private class Result(val number: Number, val hasDecimal: Boolean, val error: String = "") {
            fun isError(): Boolean {
                return error.isNotEmpty()
            }
        }

    }
}