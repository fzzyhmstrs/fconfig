/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseTokenizerType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.tokens.*
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.*
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import kotlin.math.pow

object CssType: ParseTokenizerType {

    val STRING = TokenType<String>("String", this, valueCreator = { s -> "\"$s\"" })
    val BAD_STRING = TokenType<String>("Bad String", this, true)
    val IDENT = TokenType<String>("Identifier", this)
    val FUNCTION = TokenType<String>("Function", this, valueCreator = { s -> "$s(" })
    val URL = TokenType<String>("URL", this, valueCreator = { s -> "url($s)" })
    val BAD_URL = TokenType<Unit>("Bad URL", this, true)
    val WHITESPACE = TokenType<String>("Whitespace", this, isWhitespace = true, raw = " ")
    val HASH = TokenType<String>("Hash", this, valueCreator = { s -> "#$s" })
    val DELIM = TokenType<String>("Delimiter", this)
    val NUMBER = TokenType<Parser.NumberValue>("Number", this)
    val NUMBER_DIMENSION = TokenType<NumberWithUnitValue>("Dimension-Number", this)
    val NUMBER_PERCENTAGE = TokenType<NumberWithUnitValue>("Percentage-Number", this)
    val BAD_NUMBER = TokenType<Token<out Parser.NumberValue>>("Bad Number", this, true)
    val CDO = TokenType<Unit>("cdo", this, raw = "<!--")
    val CDC = TokenType<Unit>("cdc", this, raw = "-->")
    val AT = TokenType<String>("at", this, valueCreator = { s -> "@$s" })
    val SEMI_COLON = TokenType<Char>("Semi-colon", this)
    val COLON = TokenType<Char>("Colon", this)
    val COMMA = TokenType<Char>("Comma", this)
    val OPEN_PARENTHESIS = TokenType<Char>("Parentheses Open", this)
    val CLOSE_PARENTHESIS = TokenType<Char>("Parentheses Close", this)
    val OPEN_BRACKET = TokenType<Char>("Bracket Open", this)
    val CLOSE_BRACKET = TokenType<Char>("Bracket Close", this)
    val OPEN_BRACE = TokenType<Char>("Brace Open", this)
    val CLOSE_BRACE = TokenType<Char>("Brace Close", this)

    val SIMPLE_BLOCK = TokenType<SimpleBlockConsumer.Block>("Simple Block", this)
    val FUNCTION_CONSUMED = TokenType<FunctionConsumer.Function>("Function", this)
    val STYLE_BLOCK = TokenType<StyleBlockConsumer.StyleBlock>("Style Block", this)
    val DECLARATION = TokenType<DeclarationConsumer.Declaration>("Declaration", this)
    val AT_RULE = TokenType<ListOfRulesConsumer.Rule>("At-Rule", this)
    val QUALIFIED_RULE = TokenType<ListOfRulesConsumer.Rule>("Qualified Rule", this)
    val EMPTY_RULE = TokenType<ListOfRulesConsumer.Rule>("Empty Rule", this)
    val UNKNOWN_RULE = TokenType<ListOfRulesConsumer.Rule>("Unknown Rule", this, true)

    init {
        Parser.addTokenizer(this, listOf(
            WhitespaceProducer,
            TokenProducer.SingleChar(SEMI_COLON, ';', "Semi-colon"),
            TokenProducer.SingleChar(COLON, ':', "Colon"),
            TokenProducer.SingleChar(OPEN_PARENTHESIS, '(', "Open Parenthesis"),
            TokenProducer.SingleChar(CLOSE_PARENTHESIS, ')', "Close Parenthesis"),
            TokenProducer.SingleChar(OPEN_BRACE, '{', "Open Brace"),
            TokenProducer.SingleChar(CLOSE_BRACE, '}', "Close Brace"),
            TokenProducer.SingleChar(OPEN_BRACKET, '[', "Open Bracket"),
            TokenProducer.SingleChar(CLOSE_BRACKET, ']', "Close Bracket"),
            TokenProducer.SingleChar(COMMA, ',', "Comma"),
            PlusProducer,
            MinusProducer,
            DigitProducer,
            HashProducer,
            IdentProducer,
            StringProducer,
            CommentProducer,
            PeriodProducer,
            LessThanProducer,
            AtProducer,
            OtherProducer
        ))
    }

    override fun id(): String {
        return "CSS"
    }

    override fun filterInput(string: String): String {
        val builder = StringBuilder()
        for (c in string) {
            when (c) {
                '\u0000' -> {
                    builder.append('\uFFFD')
                }
                else -> {
                    if (c.isSurrogate()) {
                        builder.append('\uFFFD')
                    } else {
                        builder.append(c)
                    }
                }
            }
        }
        return builder.toString()
    }

    ///////////////////////////////
    /////        Utils        /////
    ///////////////////////////////

    internal fun isIdentStartCodePoint(char: Char): Boolean {
        return char.isLetter() || char.code >= '\u0080'.code || char == '\u005F'
    }

    internal fun isIdentCodePoint(char: Char): Boolean {
        return isIdentStartCodePoint(char) || char.isDigit() || char == '\u002D'
    }

    internal fun isIdentSequenceStart(reader: StringReader): Boolean {
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

    internal fun isValidEscape(reader: StringReader, offset: Int = 0): Boolean {
        return (reader.canRead(2 + offset) && reader.peek(offset) == '\\' && reader.peek(1 + offset) != '\n')
    }

    internal fun consumeIdent(reader: StringReader): String {
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

    internal fun isEscapeChar(char: Char): Boolean {
        return Character.digit(char, 16) != -1
    }

    internal fun consumeEscape(c: Char, reader: StringReader): Char {
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

    internal fun isValidNumber(reader: StringReader): Boolean {
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

    internal fun consumeNumber(reader: StringReader): Result {
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

        fun toNumber(): Result {
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

        while (reader.canRead()) {
            val c = reader.peek()
            if (c == '+') {
                reader.skip()
                if (isErrored()) continue
                positive = true
            } else if (c == '-') {
                reader.skip()
                if (isErrored()) continue
                positive = false
            } else if (c.isDigit()) {
                reader.skip()
                if (isErrored()) continue
                if (!hasExponent)
                    num.append(c)
                else
                    expNum.append(c)
            } else if (c == '.') {
                reader.skip()
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
                if (reader.canRead(2) && reader.peek(1) == 'm') {
                    return toNumber()
                }
                reader.skip()
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

        return toNumber()
    }

    internal class Result(val number: Number, val hasDecimal: Boolean, val error: String = "") {
        fun isError(): Boolean {
            return error.isNotEmpty()
        }
    }

    class NumberWithUnitValue(value: Number, private val unit: String): Parser.NumberValue(value) {

        fun unit(): String {
            return unit
        }

        override fun toString(): String {
            return "${getValue()}$unit"
        }
    }

}