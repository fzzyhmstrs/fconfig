/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import me.fzzyhmstrs.fzzy_config.util.Expression.Impl.NamedExpression
import me.fzzyhmstrs.fzzy_config.util.Expression.Impl.validated
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedExpression
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.random.Random
import kotlin.math.pow
import kotlin.reflect.typeOf

/**
 * A math expression for evaluating in-game. Supports a wide array of basic math functions, as well as some [MathHelper] functions
 *
 * Typical usage involves evaluating named doubles in a math equation with matching variable names.
 *
 * Default operations and functions supported:
 * - {constant values} - any double constant value: 5.0
 * - {variable values} - any single-character letter: 'x', 'y'. Must match to the characters the evaluation side expects
 * - {+} - addition
 * - {-} - subtraction
 * - {*} - multiplication
 * - {/} - division
 * - {%} - modulus
 * - {^} - power
 * - {(...)} - parentheses
 * - {sqrt(...)} - square root
 * - {ciel(...)} - round up
 * - {floor(...)} - round down
 * - {round(...)} - round nearest
 * - {ln(...)} - natural logarithm
 * - {log(exp, power)} - logarithm of <expression> to <power>: log(5, 5) is log5(5)
 * - {log10(...)} - log 10
 * - {log2(...)} - log 2
 * - {abs(...)} - absolute value
 * - {sin(...)} - sine (radians)
 * - {cos(...)} - cosine (radians)
 * - {incr(exp, incr)} - round incrementally: incr(0.913, 0.1) will return 0.90
 * - {min(a, b)} - minimum of the two values
 * - {max(a, b)} - maximum of the two values
 * - {mathHelper methods} - Expression will reflectively evaluate any valid [MathHelper] method that takes doubles and returns doubles
 * @see validated for use in configs
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleMath.maths
 * @author fzzyhmstrs
 * @since 0.2.0, min/max since 0.3.7
 */
@FunctionalInterface
@JvmDefaultWithCompatibility
fun interface Expression {

    /**
     * Evaluates the math expression
     * @param vars Map<Char, Double> - map of the input variables. The Char used must match the variable characters used in the string expression and visa-versa
     * @return Double - The result of the expression evaluation
     * @throws IllegalStateException if the evaluation hits a critical error. Often this will be because an expected variable is not passed to vars
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Deprecated("Where possible use safeEval() to avoid throwing exceptions on evaluation failure")
    fun eval(vars: Map<Char, Double>): Double

    /**
     * Evaluates an expression with a fallback. Will fail to fallback instead of throwing. This call is recommended over the 'raw' eval call
     * @param vars Map<Char, Double> - map of the input variables. The Char used must match the variable characters used in the string expression and visa-versa
     * @param fallback Double - fallback value in case eval() throws
     * @return Double - The result of the expression evaluation or the fallback if evaluation fails
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun evalSafe(vars: Map<Char, Double>, fallback: Double): Double {
        return try {
            this.eval(vars)
        } catch(e: Exception) {
            fallback
        }
    }

    @Suppress("SameParameterValue", "OVERRIDE_DEPRECATION")
    companion object Impl {

        /**
         * Codec for expressions, storing the expression in string form
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        val CODEC = Codec.STRING.comapFlatMap(
            {s -> try{ DataResult.success(parse(s, s)) } catch(e:Exception) { DataResult.error { "Error while deserializing math equation: ${e.localizedMessage}" }}},
            {e -> e.toString()}
        )

        /**
         * parses an expression
         *
         * @param str the math expression to try parsing. Used as the expression context (this is typical)
         * @return Expression parsed from the passed string
         * @throws IllegalStateException when parsing fails
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun parse(str: String): Expression {
            return parse(str, str)
        }
        /**
         * parses an expression
         *
         * @param str the math expression to try parsing
         * @param context the context the expression is coming from
         * @return Expression parsed from the passed string
         * @throws IllegalStateException when parsing fails
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Consider using parse(str) to automatically pass the string expression as it's own context")
        fun parse(str: String, context: String): Expression {
            try {
                val reader = StringReader(str)
                return parseExpression(reader, context, 1000)
            } catch (e: Exception) {
                throw IllegalStateException("Error parsing math equation [$context]: ${e.localizedMessage}")
            }
        }

        /**
         * attempts to parse an expression, failing null instead of throwing
         * @param str the math expression to try parsing
         * @return ValidationResult<Expression?> wrapping an Expression if parsing succeeds, or null if it fails (with the exception message passed back with the Result)
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun tryParse(str: String): ValidationResult<Expression?> {
            return try {
                val reader = StringReader(str)
                ValidationResult.success(parseExpression(reader, str, 1000))
            } catch (e: Exception) {
                ValidationResult.error(null, e.localizedMessage)
            }
        }

        /**
         * Attempts to parse and evaluate an expression with dummy values, failing null instead of throwing
         * @param str the math expression to try parsing
         * @param vars [Set] of valid variable characters. This method will build a dummy Map<Char, Double> to test eval() of the expression
         * @return ValidationResult<Expression?> wrapping an Expression if parsing succeeds, or null if it fails (with the exception message passed back with the Result)
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun tryTest(str: String, vars: Set<Char>): ValidationResult<Expression?> {
            val result = tryParse(str)
            if (result.isError() || vars.isEmpty()) return result
            val varMap = vars.associate { it -> it to 0.0 }
            return try {
                result.get()?.eval(varMap)
                result
            } catch(e: Exception) {
                ValidationResult.error(null, "Incorrect variables used in expression: [$str], available: [$vars]")
            }
        }

        /**
         * Generates a validated Expression for use in configs
         *
         * @param str String. the default math expression to be used in the [ValidatedExpression]
         * @param vars Set<Char> defining the relevant and allowable variable names
         * @return ValidatedExpression wrapping the passed string as it's default expression
         * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.maths
         * @throws IllegalStateException if the passed string is not parsable
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun validated(str: String, vars: Set<Char> = setOf()): ValidatedExpression {
            parse(str)
            return ValidatedExpression(str, vars)
        }

        private val expressions: Map<String, NamedExpression> = mapOf(
            "sqrt" to NamedExpression { reader, context, _ -> val parentheses = parseParentheses(reader, context, false)
                val sqrt = sqrt(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, sqrt)
                else
                    sqrt },
            "ceil" to NamedExpression { reader, context, _ ->
                val parentheses = parseParentheses(reader, context, false)
                val ceil = ceil(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, ceil)
                else
                    ceil },
            "floor" to NamedExpression { reader, context, _ ->
                val parentheses = parseParentheses(reader, context, false)
                val floor = floor(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, floor)
                else
                    floor },
            "round" to NamedExpression { reader, context, _ ->
                val parentheses = parseParentheses(reader, context, false)
                val round = round(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, round)
                else
                    round },
            "ln" to NamedExpression { reader, context, _ ->
                val parentheses = parseParentheses(reader, context, false)
                val ln = ln(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, ln)
                else
                    ln },
            "log" to NamedExpression { reader, context, _ ->
                val params = parseParenthesesMultiple(reader, context, false)
                if (params.size != 2) throw IllegalStateException("Improper number of log arguments in equation [$context]")
                val log = log(params[0], params[1])
                if (reader.canRead())
                    parseExpression(reader, context, 1000, log)
                else
                    log },
            "log10" to NamedExpression { reader, context, _ ->
                val param = parseParentheses(reader, context, false)
                val log = log10(param)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, log)
                else
                    log },
            "log2" to NamedExpression { reader, context, _ ->
                val param = parseParentheses(reader, context, false)
                val log = log2(param)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, log)
                else
                    log },
            "abs" to NamedExpression { reader, context, _ ->
                val parentheses = parseParentheses(reader, context, false)
                val abs = abs(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, abs)
                else
                    abs },
            "sin" to NamedExpression { reader, context, _ ->
                val parentheses = parseParentheses(reader, context, false)
                val sin = sin(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, sin)
                else
                    sin },
            "cos" to NamedExpression { reader, context, _ ->
                val parentheses = parseParentheses(reader, context, false)
                val cos = cos(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context, 1000, cos)
                else
                    cos },
            "incr" to NamedExpression { reader, context, _ ->
                val parentheses = parseParenthesesMultiple(reader, context, false)
                val incr = incr(parentheses[0], parentheses[1])
                if (reader.canRead())
                    parseExpression(reader, context, 1000, incr)
                else
                    incr },
            "min" to NamedExpression { reader, context, _ ->
                val parentheses = parseParenthesesMultiple(reader, context, false)
                val min = min(parentheses[0], parentheses[1])
                if (reader.canRead())
                    parseExpression(reader, context, 1000, min)
                else
                    min },
            "max" to NamedExpression { reader, context, _ ->
                val parentheses = parseParenthesesMultiple(reader, context, false)
                val max = max(parentheses[0], parentheses[1])
                if (reader.canRead())
                    parseExpression(reader, context, 1000, max)
                else
                    max }
        )

        private fun parseExpression(reader: StringReader, context: String, order: Int, vararg inputs: Expression): Expression {
            if (reader.string.isEmpty()) throw IllegalStateException("Empty Expression found in math equation [$context]")
            reader.skipWhitespace()
            if (StringReader.isAllowedNumber(reader.peek())) {
                if (reader.peek() == '-') {
                    if(reader.canRead(1) && (reader.peek(1).isWhitespace() || !StringReader.isAllowedNumber(reader.peek(1)))) {
                        if (3 > order) return inputs[0]
                        reader.read()
                        val expression1 = inputs[0]
                        val expression2 = parseExpression(reader, context, 3)
                        return minus(expression1, expression2)
                    } else if (reader.canRead(1)) {
                        val number1 = reader.readDouble()
                        return if (reader.canRead())
                            parseExpression(reader, context, order, constant(number1))
                        else
                            constant(number1)
                    } else {
                        throw SimpleCommandExceptionType {"trailing '-' found in expression [$context]"}.createWithContext(reader)
                    }

                }
                val number1 = reader.readDouble()
                return if (reader.canRead())
                    parseExpression(reader, context, order, constant(number1))
                else
                    constant(number1)
            } else if (reader.peek() == '(') {
                val parentheses = parseParentheses(reader, context)
                return if(reader.canRead())
                    parseExpression(reader, context, 1000, parentheses)
                else
                    parentheses
            } else if (reader.peek() == '^') {
                if (1 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 1)
                return if(reader.canRead())
                    parseExpression(reader, context, 1000, pow(expression1, expression2))
                else
                    pow(expression1, expression2)
            } else if (reader.peek() == '*') {
                if (2 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 2)
                return times(expression1, expression2)
            } else if (reader.peek() == '/') {
                if (2 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 2)
                return divide(expression1, expression2)
            } else if (reader.peek() == '%') {
                if (2 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 3)
                return mod(expression1, expression2)
            } else if (reader.peek() == '+') {
                if (3 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 3)
                return plus(expression1, expression2)
            } else if (reader.peek() == '-') {
                if (3 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 3)
                return minus(expression1, expression2)
            }  else if (reader.peek().isLetter() && !reader.canRead(2)) {
                return variable(reader.peek())
            } else if (reader.peek().isLetter() && reader.canRead(2)) {
                return if (!reader.peek(1).isLetter()) {
                    val variable = variable(reader.peek())
                    reader.read()
                    parseExpression(reader, context, order, variable)
                } else {
                    val chunk = reader.readStringUntil('(').trimEnd()
                    expressions[chunk]?.get(reader, context, chunk)
                        ?: mathHelper(reader, context, chunk)
                        ?: throw SimpleCommandExceptionType { "Unknown expression '$chunk' in equation [$context]" }.createWithContext(reader)
                }
            }
            throw SimpleCommandExceptionType {"Unknown expression '${reader.remaining}' in equation [$context]"}.createWithContext(reader)
        }

        private fun parseParentheses(reader: StringReader, context: String, read: Boolean = true): Expression {
            val builder = StringBuilder()
            if(read) reader.read()
            var count = 1
            while (count != 0 && reader.canRead()) {
                val c = reader.read()
                if (c == '(') count++
                else if (c == ')') count--
                if (count != 0) builder.append(c)
            }
            if (count != 0) throw IllegalStateException("Unclosed parentheses found in equation [$context] from string $builder")
            return parentheses(parseExpression(StringReader(builder.toString()), context, 1000))
        }

        private fun parseParenthesesMultiple(reader: StringReader, context: String, read: Boolean = true): List<Expression> {
            val builder = StringBuilder()
            if(read) reader.read()
            var count = 1
            while (count != 0 && reader.canRead()) {
                val c = reader.read()
                if (c == '(') count++
                else if (c == ')') count--
                if (count != 0) builder.append(c)
            }
            if (count != 0) throw IllegalStateException("Unclosed parentheses found in equation [$context] from string $builder")
            val str: MutableList<String> = mutableListOf()
            var splitIndex = 0
            var count2 = 0
            val toEat = builder.toString()
            for ((i, c) in toEat.withIndex()) {
                when (c) {
                    '(' -> count2++
                    ')' -> count2--
                    ',' -> {
                        if (count2 == 0) {
                            str.add(toEat.substring(splitIndex, i))
                            splitIndex = i + 1
                        }
                    }
                }
            }
            str.add(toEat.substring(splitIndex))

            return str.map { parseExpression(StringReader(it), context, 1000) }
        }

        private interface Const {
            fun c(): Double
        }

        private fun constant(constant: Double): Expression {
            return Constant(constant)
        }
        private class Constant(val c1: Double): Expression, Const {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1
            }
            override fun c(): Double {
                return c1
            }
            override fun toString(): String {
                return c1.toString()
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Constant
                return c1 == other.c1
            }
            override fun hashCode(): Int {
                return c1.hashCode()
            }
        }

        private fun parentheses(e1: Expression): Expression {
            return if(e1 is Const) {
                ConstParentheses(e1.c(), e1.toString())
            } else if (e1 is ConstParentheses) {
                ConstParentheses(e1.c1, e1.s1)
            } else if (e1 is ExpParentheses) {
                ExpParentheses(e1.e1)
            } else {
                ExpParentheses(e1)
            }
        }
        private class ExpParentheses(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars)
            }
            override fun toString(): String {
                return "($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpParentheses
                return e1 == other.e1
            }
            override fun hashCode(): Int {
                return e1.hashCode()
            }
        }
        private class ConstParentheses(val c1: Double, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1
            }
            override fun toString(): String {
                return "($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstParentheses
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * c1.hashCode() + s1.hashCode()
            }
        }

        private fun variable(variable: Char): Expression {
            return Variable(variable)
        }
        private class Variable(val variable: Char): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return vars[variable] ?: throw IllegalStateException("Expected variable '$variable', didn't find")
            }
            override fun toString(): String {
                return variable.toString()
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Variable
                return variable == other.variable
            }
            override fun hashCode(): Int {
                return variable.hashCode()
            }
        }

        private fun plus(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstPlus(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstPlus(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondPlus(e1, e2.c(), e2.toString())
            } else {
                ExpPlus(e1, e2)
            }
        }
        private class ExpPlus(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) + e2.eval(vars)
            }
            override fun toString(): String {
                return "$e1 + $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpPlus
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * e1.hashCode() + e2.hashCode()
            }
        }
        private class ConstFirstPlus(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1 + e2.eval(vars)
            }
            override fun toString(): String {
                return "$s1 + $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstPlus
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * c1.hashCode() + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondPlus(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) + c2
            }
            override fun toString(): String {
                return "$e1 + $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondPlus
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 95821 * (92821 * e1.hashCode() + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstPlus(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression, Const {
            private val c3: Double = c1 + c2
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun c(): Double {
                return c3
            }
            override fun toString(): String {
                return "$s1 + $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstPlus
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * c1.hashCode() + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }


        private fun minus(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstMinus(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstMinus(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondMinus(e1, e2.c(), e2.toString())
            } else {
                ExpMinus(e1, e2)
            }
        }
        private class ExpMinus(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) - e2.eval(vars)
            }
            override fun toString(): String {
                return "$e1 - $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpMinus
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 1) + e2.hashCode()
            }
        }
        private class ConstFirstMinus(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1 - e2.eval(vars)
            }
            override fun toString(): String {
                return "$s1 - $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstMinus
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (c1.hashCode() + 1) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondMinus(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) - c2
            }
            override fun toString(): String {
                return "$e1 - $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondMinus
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (e1.hashCode() + 1) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstMinus(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression {
            private val c3: Double = c1 - c2
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun toString(): String {
                return "$s1 - $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstMinus
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * (c1.hashCode() + 1) + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }

        private fun times(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstTimes(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstTimes(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondTimes(e1, e2.c(), e2.toString())
            } else {
                ExpTimes(e1, e2)
            }
        }
        private class ExpTimes(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) * e2.eval(vars)
            }
            override fun toString(): String {
                return "$e1 * $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpTimes
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 2) + e2.hashCode()
            }
        }
        private class ConstFirstTimes(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1 * e2.eval(vars)
            }
            override fun toString(): String {
                return "$s1 * $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstTimes
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (c1.hashCode() + 2) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondTimes(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) * c2
            }
            override fun toString(): String {
                return "$e1 * $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondTimes
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (e1.hashCode() + 2) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstTimes(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression {
            private val c3: Double = c1 * c2
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun toString(): String {
                return "$s1 * $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstTimes
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * (c1.hashCode() + 2) + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }

        private fun divide(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstDivide(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstDivide(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondDivide(e1, e2.c(), e2.toString())
            } else {
                ExpDivide(e1, e2)
            }
        }
        private class ExpDivide(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) / e2.eval(vars)
            }
            override fun toString(): String {
                return "$e1 / $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpDivide
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 3) + e2.hashCode()
            }
        }
        private class ConstFirstDivide(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1 / e2.eval(vars)
            }
            override fun toString(): String {
                return "$s1 / $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstDivide
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (c1.hashCode() + 3) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondDivide(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) / c2
            }
            override fun toString(): String {
                return "$e1 / $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondDivide
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (e1.hashCode() + 3) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstDivide(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression {
            private val c3: Double = c1 / c2
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun toString(): String {
                return "$s1 / $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstDivide
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * (c1.hashCode() + 3) + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }

        private fun mod(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstMod(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstMod(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondMod(e1, e2.c(), e2.toString())
            } else {
                ExpMod(e1, e2)
            }
        }
        private class ExpMod(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) % e2.eval(vars)
            }
            override fun toString(): String {
                return "$e1 % $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpMod
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 4) + e2.hashCode()
            }
        }
        private class ConstFirstMod(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1 % e2.eval(vars)
            }
            override fun toString(): String {
                return "$s1 % $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstMod
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (c1.hashCode() + 4) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondMod(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars) % c2
            }
            override fun toString(): String {
                return "$e1 % $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondMod
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (e1.hashCode() + 4) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstMod(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression {
            private val c3: Double = c1 % c2
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun toString(): String {
                return "$s1 % $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstMod
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * (c1.hashCode() + 4) + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }

        private fun pow(e1: Expression, e2: Expression): Expression {
            return if (e2 is Const) {
                val c2 = e2.c()
                if (c2 == 0.0) {
                    ZeroPower(e1.toString(), e2.toString())
                } else if (c2 == 1.0) {
                    if(e1 is Const) {
                        OneConstPower(e1.c(), e2.toString())
                    } else {
                        OneExpPower(e1, e2.toString())
                    }
                } else if (e1 is Const) {
                    ConstPower(e1.c(), c2, e1.toString(), e2.toString())
                } else if (c2 % 1 == 0.0) {
                    ExpIntPower(e1, c2.toInt(), e2.toString())
                } else {
                    ExpConstPower(e1, c2, e2.toString())
                }
            } else if (e1 is Const) {
                ConstExpPower(e1.c(), e2, e1.toString())
            } else {
                ExpPower(e1, e2)
            }
        }
        private class ConstPower(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression, Const {
            private val c3 = c1.pow(c2)
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun c(): Double {
                return c3
            }
            override fun toString(): String {
                return "$s1 ^ $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstPower
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (s1.hashCode() + 5) + s2.hashCode()
            }
        }
        private class ZeroPower(val s1: String, val s2: String): Expression, Const {
            override fun eval(vars: Map<Char, Double>): Double {
                return 1.0
            }
            override fun c(): Double {
                return 1.0
            }
            override fun toString(): String {
                return "$s1 ^ $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ZeroPower
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (s1.hashCode() + 5) + s2.hashCode()
            }
        }
        private class OneExpPower(val e1: Expression, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars)
            }
            override fun toString(): String {
                return "$e1 ^ $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as OneExpPower
                if (e1 != other.e1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 5) + s2.hashCode()
            }
        }
        private class OneConstPower(val c1: Double, val s2: String): Expression, Const {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1
            }
            override fun c(): Double {
                return c1
            }
            override fun toString(): String {
                return "$c1 ^ $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as OneConstPower
                if (c1 != other.c1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 5) + s2.hashCode()
            }
        }
        private class ExpIntPower(val e1: Expression, val c2: Int, val s2: String): Expression {
            val e2: Expression
            init {
                var exp = times(e1, e1)
                for (i in 2 until c2) {
                    exp = times(exp, e1)
                }
                e2 = exp
            }
            override fun eval(vars: Map<Char, Double>): Double {
                return e2.eval(vars)
            }
            override fun toString(): String {
                return "$e1 ^ $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpIntPower
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 95821 * (92821 * (e1.hashCode() + 5) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ExpConstPower(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars).pow(c2)
            }
            override fun toString(): String {
                return "$e1 ^ $s2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpConstPower
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 95821 * (92821 * (e1.hashCode() + 5) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstExpPower(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return c1.pow(e2.eval(vars))
            }
            override fun toString(): String {
                return "$s1 ^ $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstExpPower
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 95821 * (92821 * (c1.hashCode() + 5) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ExpPower(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return e1.eval(vars).pow(e2.eval(vars))
            }
            override fun toString(): String {
                return "$e1 ^ $e2"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpPower
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 5) + e2.hashCode()
            }
        }

        private fun sqrt(e1: Expression): Expression {
            return if (e1 is ConstParentheses) {
                ConstSqrt(e1.c1, e1.s1)
            } else {
                ExpSqrt((e1 as ExpParentheses).e1)
            }
        }
        private class ConstSqrt(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.sqrt(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "sqrt($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSqrt
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 6) + s1.hashCode()
            }
        }
        private class ExpSqrt(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.sqrt(e1.eval(vars))
            }
            override fun toString(): String {
                return "sqrt($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpSqrt
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 6
            }
        }


        private fun ceil(e1: Expression): Expression {
            return if (e1 is ConstParentheses) {
                ConstCeil(e1.c1, e1.s1)
            } else {
                ExpCeil((e1 as ExpParentheses).e1)
            }
        }
        private class ConstCeil(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.ceil(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "ceil($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstCeil
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 7) + s1.hashCode()
            }
        }
        private class ExpCeil(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.ceil(e1.eval(vars))
            }
            override fun toString(): String {
                return "ceil($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpCeil
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 7
            }
        }


        private fun floor(e1: Expression): Expression {
            return if (e1 is ConstParentheses) {
                ConstFloor(e1.c1, e1.s1)
            } else {
                ExpFloor((e1 as ExpParentheses).e1)
            }
        }
        private class ConstFloor(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.floor(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "floor($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFloor
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 8) + s1.hashCode()
            }
        }
        private class ExpFloor(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.floor(e1.eval(vars))
            }
            override fun toString(): String {
                return "floor($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpFloor
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 8
            }
        }


        private fun round(e1: Expression): Expression {
            return if (e1 is ConstParentheses) {
                ConstRound(e1.c1, e1.s1)
            } else {
                ExpRound((e1 as ExpParentheses).e1)
            }
        }
        private class ConstRound(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.round(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "round($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstRound
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 9) + s1.hashCode()
            }
        }
        private class ExpRound(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.round(e1.eval(vars))
            }
            override fun toString(): String {
                return "round($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpRound
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 9
            }
        }

        private fun log(e1: Expression, base: Expression): Expression {
            return if (base is Const) {
                if (base.c() == 2.0) {
                    log2(e1)
                } else if (base.c() == 10.0) {
                    log10(e1)
                } else if (e1 is Const) {
                    ConstLog(e1.c(), base.c(), e1.toString(), base.toString())
                } else {
                    ConstBaseLog(e1, base.c(), base.toString())
                }
            } else if (e1 is Const) {
                ConstOperandLog(e1.c(), base, e1.toString())
            } else {
                ExpLog(e1, base)
            }
        }
        private class ConstLog(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression, Const {
            private val c3 = kotlin.math.log(c1, c2)
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun c(): Double {
                return c3
            }
            override fun toString(): String {
                return "log($s1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstLog
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }

            override fun hashCode(): Int {
                var result = c1.hashCode() + 10
                result = 92821 * result + c2.hashCode()
                result = 92821 * result + s1.hashCode()
                result = 92821 * result + s2.hashCode()
                return result
            }

        }
        private class ConstBaseLog(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.log(e1.eval(vars), c2)
            }
            override fun toString(): String {
                return "log($e1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstBaseLog
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                var result = e1.hashCode()
                result = 92821 * result + c2.hashCode()
                result = 92821 * result + s2.hashCode()
                return result
            }
        }
        private class ConstOperandLog(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.log(c1, e2.eval(vars))
            }
            override fun toString(): String {
                return "log($s1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstOperandLog
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                var result = c1.hashCode()
                result = 92821 * result + e2.hashCode()
                result = 92821 * result + s1.hashCode()
                return result
            }
        }
        private class ExpLog(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.log(e1.eval(vars), e2.eval(vars))
            }
            override fun toString(): String {
                return "log($e1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpLog
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                var result = e1.hashCode()
                result = 92821 * result + e2.hashCode()
                return result
            }
        }


        private fun log10(e1: Expression): Expression {
            return if(e1 is ConstParentheses) {
                ConstLog10(e1.c1, e1.s1)
            } else {
                ExpLog10((e1 as ExpParentheses).e1)
            }
        }
        private class ConstLog10(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.log10(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "log10($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstLog10
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 11) + s1.hashCode()
            }
        }
        private class ExpLog10(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.log10(e1.eval(vars))
            }
            override fun toString(): String {
                return "log10($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpLog10
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 11
            }
        }

        private fun log2(e1: Expression): Expression {
            return if(e1 is ConstParentheses) {
                ConstLog2(e1.c1, e1.s1)
            } else {
                ExpLog2((e1 as ExpParentheses).e1)
            }
        }
        private class ConstLog2(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.log2(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "log2($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstLog2
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 12) + s1.hashCode()
            }
        }
        private class ExpLog2(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.log2(e1.eval(vars))
            }
            override fun toString(): String {
                return "log2($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpLog2
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 12
            }
        }


        private fun ln(e1: Expression): Expression {
            return if(e1 is ConstParentheses) {
                ConstLn(e1.c1, e1.s1)
            } else {
                ExpLn((e1 as ExpParentheses).e1)
            }
        }
        private class ConstLn(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.ln(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "ln($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstLn
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 13) + s1.hashCode()
            }
        }
        private class ExpLn(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.ln(e1.eval(vars))
            }
            override fun toString(): String {
                return "ln($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpLn
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 13
            }
        }

        private fun abs(e1: Expression): Expression {
            return if(e1 is ConstParentheses) {
                ConstAbs(e1.c1, e1.s1)
            } else {
                ExpAbs((e1 as ExpParentheses).e1)
            }
        }
        private class ConstAbs(val c1: Double, val s1: String): Expression, Const {
            private val c2 = kotlin.math.abs(c1)
            override fun eval(vars: Map<Char, Double>): Double {
                return c2
            }
            override fun c(): Double {
                return c2
            }
            override fun toString(): String {
                return "abs($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstAbs
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 14) + s1.hashCode()
            }
        }
        private class ExpAbs(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.abs(e1.eval(vars))
            }
            override fun toString(): String {
                return "abs($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpAbs
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 14
            }
        }

        private fun sin(e1: Expression): Expression {
            return if(e1 is ConstParentheses) {
                ConstSin(e1.c1, e1.s1)
            } else {
                ExpSin((e1 as ExpParentheses).e1)
            }
        }
        private class ConstSin(val c1: Double, val s1: String): Expression, Const {
            private val c3 = MathHelper.sin(c1.toFloat()).toDouble()
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun c(): Double {
                return c3
            }
            override fun toString(): String {
                return "sin($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSin
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 15) + s1.hashCode()
            }
        }
        private class ExpSin(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return MathHelper.sin(e1.eval(vars).toFloat()).toDouble()
            }
            override fun toString(): String {
                return "sin($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpSin
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 15
            }
        }


        private fun cos(e1: Expression): Expression {
            return if(e1 is ConstParentheses) {
                ConstCos(e1.c1, e1.s1)
            } else {
                ExpCos((e1 as ExpParentheses).e1)
            }
        }
        private class ConstCos(val c1: Double, val s1: String): Expression, Const {
            private val c3 = MathHelper.cos(c1.toFloat()).toDouble()
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun c(): Double {
                return c3
            }
            override fun toString(): String {
                return "cos($s1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstCos
                if (c1 != other.c1) return false
                if (s1 != other.s1) return false

                return true
            }
            override fun hashCode(): Int {
                return 92821 * (c1.hashCode() + 16) + s1.hashCode()
            }
        }
        private class ExpCos(val e1: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return MathHelper.cos(e1.eval(vars).toFloat()).toDouble()
            }
            override fun toString(): String {
                return "cos($e1)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpCos
                return (e1 == other.e1)
            }
            override fun hashCode(): Int {
                return e1.hashCode() + 16
            }
        }


        private fun incr(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstIncr(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstIncr(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondIncr(e1, e2.c(), e2.toString())
            } else {
                ExpIncr(e1, e2)
            }
        }
        private class ExpIncr(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                val base = e1.eval(vars)
                val increment = e2.eval(vars)
                return base - (base % increment)
            }
            override fun toString(): String {
                return "incr($e1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpIncr
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 17) + e2.hashCode()
            }
        }
        private class ConstFirstIncr(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                val increment = e2.eval(vars)
                return c1 - (c1 % increment)
            }
            override fun toString(): String {
                return "incr($s1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstIncr
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (c1.hashCode() + 17) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondIncr(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                val base = e1.eval(vars)
                return base - (base % c2)
            }
            override fun toString(): String {
                return "incr($e1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondIncr
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (e1.hashCode() + 17) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstIncr(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression {
            private val c3 = c1 - (c1 % c2)
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun toString(): String {
                return "incr($s1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstIncr
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * (c1.hashCode() + 17) + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }

        private fun min(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstMin(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstMin(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondMin(e1, e2.c(), e2.toString())
            } else {
                ExpMin(e1, e2)
            }
        }
        private class ExpMin(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.min(e1.eval(vars), e2.eval(vars))
            }
            override fun toString(): String {
                return "min($e1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpMin
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 18) + e2.hashCode()
            }
        }
        private class ConstFirstMin(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.min(c1, e2.eval(vars))
            }
            override fun toString(): String {
                return "min($s1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstMin
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (c1.hashCode() + 18) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondMin(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.min(e1.eval(vars), c2)
            }
            override fun toString(): String {
                return "min($e1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondMin
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (e1.hashCode() + 18) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstMin(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression {
            private val c3 = kotlin.math.min(c1, c2)
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun toString(): String {
                return "min($s1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstMin
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * (c1.hashCode() + 18) + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }

        private fun max(e1: Expression, e2: Expression): Expression {
            return if(e1 is Const) {
                if (e2 is Const) {
                    ConstMax(e1.c(), e2.c(), e1.toString(), e2.toString())
                } else {
                    ConstFirstMax(e1.c(), e2, e1.toString())
                }
            } else if (e2 is Const) {
                ConstSecondMax(e1, e2.c(), e2.toString())
            } else {
                ExpMax(e1, e2)
            }
        }
        private class ExpMax(val e1: Expression, val e2: Expression): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.max(e1.eval(vars), e2.eval(vars))
            }
            override fun toString(): String {
                return "max($e1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ExpMax
                if (e1 != other.e1) return false
                if (e2 != other.e2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (e1.hashCode() + 19) + e2.hashCode()
            }
        }
        private class ConstFirstMax(val c1: Double, val e2: Expression, val s1: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.max(c1, e2.eval(vars))
            }
            override fun toString(): String {
                return "max($s1, $e2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstFirstMax
                if (c1 != other.c1) return false
                if (e2 != other.e2) return false
                if (s1 != other.s1) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (c1.hashCode() + 19) + e2.hashCode()) + s1.hashCode()
            }
        }
        private class ConstSecondMax(val e1: Expression, val c2: Double, val s2: String): Expression {
            override fun eval(vars: Map<Char, Double>): Double {
                return kotlin.math.max(e1.eval(vars), c2)
            }
            override fun toString(): String {
                return "max($e1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstSecondMax
                if (e1 != other.e1) return false
                if (c2 != other.c2) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (e1.hashCode() + 19) + c2.hashCode()) + s2.hashCode()
            }
        }
        private class ConstMax(val c1: Double, val c2: Double, val s1: String, val s2: String): Expression {
            private val c3 = kotlin.math.max(c1, c2)
            override fun eval(vars: Map<Char, Double>): Double {
                return c3
            }
            override fun toString(): String {
                return "max($s1, $s2)"
            }
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ConstMax
                if (c1 != other.c1) return false
                if (c2 != other.c2) return false
                if (s1 != other.s1) return false
                if (s2 != other.s2) return false
                return true
            }
            override fun hashCode(): Int {
                return 92821 * (92821 * (92821 * (c1.hashCode() + 19) + c2.hashCode()) + s1.hashCode()) + s2.hashCode()
            }
        }


        private val doubleType = typeOf<Double>()
        private val randomClassifier = typeOf<Random>().classifier
        private val random = Random.createLocal()

        private fun mathHelper(reader: StringReader, context: String, chunk: String): Expression? {
            val member = MathHelper::class.members.firstOrNull {
                it.name == chunk && it.parameters.mapNotNull {
                    p -> if(p.type == doubleType || p.type.classifier == randomClassifier) true else null
                }.isNotEmpty()
            }
            val params = member?.parameters
                ?: return null

            /*println(typeOf<Double>())
            println(params.map { it.type })
            println(randomType)
            println(doubleType)*/
            var numCount = 0
            val inputs: MutableList<Any> = mutableListOf()
            for (param in params) {
                if (param.type == doubleType) {
                    numCount++
                }
            }
            if (numCount > 0) {
                val expressions = parseParenthesesMultiple(reader, context, false)
                var j = 0
                for (param in params) {
                    if (param.type == doubleType) {
                        //println("Double TYPE")
                        inputs.add(expressions[j])
                        j++
                    } else if (param.type.classifier == randomClassifier) {
                        //println("Random TYPE")
                        inputs.add(random)
                    }

                }
                if (expressions.size != numCount) {
                    //println(expressions)
                    //println("Num count doesn't match!!")
                    throw IllegalStateException("Incorrect number of parameters passed to [$chunk], expected [${numCount}] found [${expressions.size}]")
                }
                //println(inputs)
            } else {
                for (param in params) {
                    //println("Random TYPE")
                    inputs.add(random)
                }
                if (reader.peek() == '(')
                    reader.read()
            }
            return object: Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return member.call(*params.mapIndexed{index, kParameter -> if(kParameter.type.classifier == randomClassifier) inputs[index] else (inputs[index] as Expression).eval(vars) }.toTypedArray()) as Double
                }

                override fun toString(): String {
                    return "$chunk${inputs.toString().replace('[', '(').replace(']', ')')}"
                }
            }
        }

        fun interface NamedExpression {
            fun get(reader: StringReader, context: String, chunk: String): Expression
        }

    }
}