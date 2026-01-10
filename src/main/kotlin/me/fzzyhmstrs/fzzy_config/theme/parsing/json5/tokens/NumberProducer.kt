/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.json5.tokens

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.Result
import me.fzzyhmstrs.fzzy_config.theme.parsing.json5.Json5Type
import me.fzzyhmstrs.fzzy_config.theme.parsing.json5.Json5Type.isHex
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer
import java.lang.StringBuilder
import kotlin.math.pow

object NumberProducer: TokenProducer() {

    override fun id(): String {
        return "Boolean"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return Json5Type.isValidNumber(reader)
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val ln = reader.getLine()
        val col = reader.getColumn()
        if (reader.peekFor("+Infinity")) {
            reader.skip(9)
            context.token(Token(Json5Type.NUMBER, Double.POSITIVE_INFINITY, ln, col))
            return true
        } else if (reader.peekFor("-Infinity")) {
            reader.skip(9)
            context.token(Token(Json5Type.NUMBER, Double.NEGATIVE_INFINITY, ln, col))
            return true
        } else if (reader.peekFor("NaN")) {
            reader.skip(3)
            context.token(Token(Json5Type.NUMBER, Double.NaN, ln, col))
            return true
        }
        var negative = false
        var positive = false
        var exponentFound = false
        var exponentNegative = false
        var exponentPositive = false
        var decimalFound = false

        val num = StringBuilder()
        val expNum = StringBuilder()

        while (reader.canRead()) {
            val c = reader.peek()
            if (c == '+') {
                reader.skip()
                if (exponentFound) {
                    if (exponentPositive || exponentNegative) {
                        context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "Multiple positive/negative signs found"))
                        return true
                    }
                    exponentPositive = true
                } else {
                    if (positive || negative) {
                        context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "Multiple positive/negative signs found"))
                        return true
                    }
                    positive = true
                }
            } else if (c == '-') {
                reader.skip()
                if (exponentFound) {
                    if (exponentPositive || exponentNegative) {
                        context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "Multiple positive/negative signs found"))
                        return true
                    }
                    exponentNegative = true
                } else {
                    if (positive || negative) {
                        context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "Multiple positive/negative signs found"))
                        return true
                    }
                    negative = true
                }
            } else if (c == 'e' || c == 'E') {
                reader.skip()
                if (exponentFound) {
                    context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "Multiple exponent signs found"))
                    return true
                }
                exponentFound = true
                continue
            } else if (reader.peekFor("0x") || reader.peekFor("0X")) {
                reader.skip(2)
                if (!reader.canRead()) {
                    context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "hexadecimal number without hex numerals"))
                    return true
                }
                val stringBuilder = StringBuilder()
                while (reader.canRead()) {
                    val h = reader.peek()
                    if (!h.isHex()) {
                        val hexValue = stringBuilder.toString().toIntOrNull(16)
                        if (hexValue == null) {
                            context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "invalid hexadecimal number"))
                        } else {
                            if (negative) {
                                context.token(Token(Json5Type.NUMBER, -hexValue, ln, col))
                            } else {
                                context.token(Token(Json5Type.NUMBER, hexValue, ln, col))
                            }
                        }
                        return true
                    } else {
                        reader.skip()
                        stringBuilder.append(h)
                    }
                }
            } else if (c == '.') {
                reader.skip()
                if (decimalFound) {
                    context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "multiple decimal points in number"))
                    return true
                }
                if (exponentFound) {
                    context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "fractional exponent not allowed"))
                    return true
                }
                decimalFound = true
                num.append(c)
            } else if (c.isDigit()) {
                reader.skip()
                if (exponentFound) {
                    expNum.append(c)
                } else {
                    num.append(c)
                }
            } else {
                val n: Number? = if (decimalFound) {
                    num.toString().toDoubleOrNull()
                } else {
                    num.toString().toLongOrNull()
                }
                if (n == null) {
                    context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "unparsable number"))
                    return true
                }
                val en = if (exponentFound) {
                    val enn = expNum.toString().toDoubleOrNull()
                    if (enn == null) {
                        context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "unparsable exponent"))
                        return true
                    }
                    enn
                } else {
                    0.0
                }
                val number: Number = if (decimalFound) {
                    (if(negative) -1.0 else 1.0) * n.toDouble() * 10.0.pow((if (exponentNegative) -1.0 else 1.0) * (en))
                } else {
                    ((if(negative) -1L else 1L) * n.toLong() * 10.0.pow((if (exponentNegative) -1.0 else 1.0) * (en))).toLong()
                }
                context.token(Token(Json5Type.NUMBER, number, ln, col))
                return true
            }
        }
        context.token(Token.unit(Json5Type.BAD_NUMBER, ln, col, "invalid number"))
        return true
    }
}