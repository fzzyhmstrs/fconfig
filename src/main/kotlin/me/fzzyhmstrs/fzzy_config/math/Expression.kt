package me.fzzyhmstrs.fzzy_config.math

import com.mojang.brigadier.StringReader
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.math.Expression.Companion.NamedExpression
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedExpression
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.random.Random
import kotlin.math.pow
import kotlin.reflect.typeOf

/**
 * An evaluatable math expression
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
 * - {log(exp,power)} - logarithm of <expression> to <power>: log(5,5) is log5(5)
 * - {log10(...)} - log 10
 * - {log2(...)} - log 2
 * - {abs(...)} - absolute value
 * - {sin(...)} - sine (radians)
 * - {cos(...)} - cosine (radians)
 * - {incr(exp,incr)} - round incrementally: incr(0.913,0.1) will return 0.90
 * - {mathHelper methods} - Expression will reflectively evaluate any valid [MathHelper] method that takes doubles and returns doubles
 * @see validated for use in configs
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ExampleMath]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface Expression {

    /**
     * Evaluates the math expression
     * @param vars Map<Char, Double> - map of the input variables. The Char used must match the variable characters used in the string expression and visa-versa
     * @return Double - The result of the expression evaluation
     */
    fun eval(vars: Map<Char,Double>): Double

    @Suppress("SameParameterValue")
    companion object {

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
                return parseExpression(reader, context,1000)
            } catch (e: Exception){
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
                ValidationResult.success(parseExpression(reader, str,1000))
            } catch (e: Exception){
                ValidationResult.error(null, e.localizedMessage)
            }
        }

        /**
         * Generates a validated Expression for use in configs
         *
         * @param str String. the default math expression to be used in the [ValidatedExpression]
         * @return ValidatedExpression wrapping the passed string as it's default expression
         * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.shorthandMath]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun validated(str: String): ValidatedExpression{
            parse(str)
            return ValidatedExpression(str)
        }

        private val expressions: Map<String, NamedExpression> = mapOf(
            "sqrt" to NamedExpression { reader, context, _ -> val parentheses = parseParentheses(reader, context, false)
                val sqrt = sqrt(parentheses)
                if (reader.canRead())
                    parseExpression(reader, context,1000, sqrt)
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
                val incr = incr(parentheses[0],parentheses[1])
                if (reader.canRead())
                    parseExpression(reader, context, 1000, incr)
                else
                    incr }
        )

        private fun parseExpression(reader: StringReader, context: String,order: Int, vararg inputs: Expression): Expression {
            if (reader.string.isEmpty()) throw IllegalStateException("Empty Expression found in math equation [$context]")
            reader.skipWhitespace()
            if (StringReader.isAllowedNumber(reader.peek())){
                val number1 = reader.readDouble()
                return if (reader.canRead())
                    parseExpression(reader,context,order, constant(number1))
                else
                    constant(number1)
            } else if (reader.peek() == '(') {
                val parentheses = parseParentheses(reader, context)
                return if(reader.canRead())
                    parseExpression(reader,context,1000, parentheses)
                else
                    parentheses
            }else if (reader.peek() == '^') {
                if (1 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context,1)
                return if(reader.canRead())
                    parseExpression(reader,context,1000, pow(expression1,expression2))
                else
                    pow(expression1,expression2)
            }else if (reader.peek() == '*') {
                if (2 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context,2)
                return times(expression1,expression2)
            } else if (reader.peek() == '/') {
                if (2 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 2)
                return divide(expression1,expression2)
            } else if (reader.peek() == '%') {
                if (2 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 3)
                return mod(expression1,expression2)
            } else if (reader.peek() == '+') {
                if (3 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 3)
                return plus(expression1,expression2)
            } else if (reader.peek() == '-') {
                if (3 > order) return inputs[0]
                reader.read()
                val expression1 = inputs[0]
                val expression2 = parseExpression(reader, context, 3)
                return minus(expression1,expression2)
            }  else if (reader.peek().isLetter() && !reader.canRead(2)){
                return variable(reader.peek())
            } else if (reader.peek().isLetter() && reader.canRead(2)){
                return if (!reader.peek(1).isLetter()) {
                    val variable = variable(reader.peek())
                    reader.read()
                    parseExpression(reader, context, order, variable)
                } else {
                    val chunk = reader.readStringUntil('(').trimEnd()
                    expressions[chunk]?.get(reader, context, chunk)
                        ?: mathHelper(reader, context, chunk)
                        ?: throw IllegalStateException("Unknown expression '$chunk' in equation [$context]")
                }
            }
            throw IllegalStateException("Unknown expression '${reader.remaining}' in equation [$context]")
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
            for ((i,c) in toEat.withIndex()){
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


        private fun constant(constant: Double): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return constant
                }
                override fun toString(): String {
                    return "$constant"
                }
            }
        }
        private fun parentheses(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return e1.eval(vars)
                }
                override fun toString(): String {
                    return "($e1)"
                }
            }
        }
        private fun variable(variable: Char): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return vars[variable] ?: throw IllegalStateException("Expected variable '$variable', didn't find")
                }
                override fun toString(): String {
                    return variable.toString()
                }
            }
        }
        private fun plus(e1: Expression, e2: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return e1.eval(vars) + e2.eval(vars)
                }
                override fun toString(): String {
                    return "$e1 + $e2"
                }
            }
        }
        private fun minus(e1: Expression, e2: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return e1.eval(vars) - e2.eval(vars)
                }
                override fun toString(): String {
                    return "$e1 - $e2"
                }
            }
        }
        private fun times(e1: Expression, e2: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return e1.eval(vars) * e2.eval(vars)
                }
                override fun toString(): String {
                    return "$e1 * $e2"
                }
            }
        }
        private fun divide(e1: Expression, e2: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return e1.eval(vars) / e2.eval(vars)
                }
                override fun toString(): String {
                    return "$e1 / $e2"
                }
            }
        }
        private fun mod(e1: Expression, e2: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return e1.eval(vars) % e2.eval(vars)
                }
                override fun toString(): String {
                    return "$e1 % $e2"
                }
            }
        }
        private fun pow(e1: Expression, e2: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return e1.eval(vars).pow(e2.eval(vars))
                }
                override fun toString(): String {
                    return "$e1 ^ $e2"
                }
            }
        }
        private fun sqrt(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.sqrt(e1.eval(vars))
                }
                override fun toString(): String {
                    return "sqrt$e1"
                }
            }
        }
        private fun ceil(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.ceil(e1.eval(vars))
                }
                override fun toString(): String {
                    return "ceil$e1"
                }
            }
        }
        private fun floor(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.floor(e1.eval(vars))
                }
                override fun toString(): String {
                    return "floor$e1"
                }
            }
        }
        private fun round(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.round(e1.eval(vars))
                }
                override fun toString(): String {
                    return "round$e1"
                }
            }
        }
        private fun log(e1: Expression, power: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.log(e1.eval(vars),power.eval(vars))
                }
                override fun toString(): String {

                    return "log[${power.toString()}]$e1"
                }
            }
        }

        private fun log10(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.log10(e1.eval(vars))
                }
                override fun toString(): String {

                    return "log10$e1"
                }
            }
        }

        private fun log2(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.log2(e1.eval(vars))
                }
                override fun toString(): String {

                    return "log2$e1"
                }
            }
        }
        private fun ln(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.ln(e1.eval(vars))
                }
                override fun toString(): String {
                    return "ln$e1"
                }
            }
        }
        private fun abs(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.abs(e1.eval(vars))
                }
                override fun toString(): String {
                    return "abs$e1"
                }
            }
        }
        private fun sin(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.sin(e1.eval(vars))
                }
                override fun toString(): String {
                    return "sin$e1"
                }
            }
        }
        private fun cos(e1: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    return kotlin.math.cos(e1.eval(vars))
                }
                override fun toString(): String {
                    return "cos$e1"
                }
            }
        }
        private fun incr(e1: Expression, e2: Expression): Expression {
            return object : Expression {
                override fun eval(vars: Map<Char, Double>): Double {
                    val base = e1.eval(vars)
                    val increment = e2.eval(vars)
                    return base - (base % increment)
                }
                override fun toString(): String {
                    return "incr($e1,$e2)"
                }
            }
        }

        private val doubleType = typeOf<Double>()
        private val randomClassifier = typeOf<Random>().classifier
        private val random = Random.createLocal()

        private fun mathHelper(reader: StringReader, context: String, chunk: String): Expression?{
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
            for (param in params){
                if (param.type == doubleType){
                    numCount++
                }
            }
            if (numCount > 0) {
                val expressions = parseParenthesesMultiple(reader, context, false)
                var j = 0
                for (param in params){
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
                for (param in params){
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
                    return "$chunk${inputs.toString().replace('[','(').replace(']',')')}"
                }
            }
        }

        fun interface NamedExpression{
            fun get(reader: StringReader, context: String, chunk: String): Expression
        }

    }
}