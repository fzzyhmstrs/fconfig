/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.parser

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseTokenizerType
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.*
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.io.BufferedReader
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.collections.ArrayList

object Parser {
    private val parseSpecs: MutableMap<ParseTokenizerType, Spec> = hashMapOf()

    private val UNKNOWN_TYPE = object : ParseTokenizerType {
        override fun id(): String {
            return "unknown"
        }

        override fun filterInput(string: String): String {
            return string
        }
    }

    val SPECIAL_TYPE = object : ParseTokenizerType {
        override fun id(): String {
            return "special"
        }

        override fun filterInput(string: String): String {
            return string
        }
    }

    private val UNKNOWN = TokenType<String>("Unknown", UNKNOWN_TYPE, true)
    val EOL = TokenType<Unit>("EOL", SPECIAL_TYPE, false, raw = "\n")
    val EOF = TokenType<Unit>("EOF", SPECIAL_TYPE, false, valueCreator = { "" })

    fun <T: Any> parse(input: BufferedReader, type: ParseTokenizerType, consumer: TokenConsumer<T>, vararg args: String): ValidationResult<Result<T>> {
        val argSet = args.toSet()
        val a = System.currentTimeMillis()
        val spec = parseSpecs[type] ?: throw IllegalStateException("Parse spec ${type.id()} not registered")
        val lines = input.use { inputReader ->
            inputReader.lines().collect(Collectors.toCollection(::ArrayList))
        }
        val b = System.currentTimeMillis()

        val tokens: LinkedList<Token<*>> = LinkedList()

        var pendingProducer: TokenProducer? = null

        for ((index, line) in lines.withIndex()) {

            val lineTokens: MutableList<Token<*>> = mutableListOf()

            val context = object : ParseContext() {
                private val reader = StringReader(type.filterInput(line), index + 1)

                override fun reader(): StringReader {
                    return reader
                }

                override fun token(token: Token<*>) {
                    lineTokens.add(token)
                }
            }

            var unknownBuilder = StringBuilder()

            val pp = pendingProducer
            if (context.reader().canRead() && pp != null) {
                pendingProducer = null
                val before = context.reader().getColumn()
                val complete = pp.produce(context)
                if (context.reader().getColumn() == before) {
                    throw IllegalStateException("Provider ${pp.id()} didn't consume any input characters at column $before")
                }
                if (!complete) {
                    pendingProducer = pp
                }
            }

            while (context.reader().canRead()) {
                var unknown = true
                for (producer in spec.producers) {
                    if (producer.canProduce(context.reader())) {
                        if (unknownBuilder.isNotEmpty()) {
                            context.token(UNKNOWN, unknownBuilder.toString(), context.reader().getLine(), context.reader().getColumn())
                            unknownBuilder = StringBuilder()
                        }
                        val before = context.reader().getColumn()
                        val complete = producer.produce(context)
                        if (context.reader().getColumn() == before) {
                            throw IllegalStateException("Provider ${producer.id()} didn't consume any input characters at column $before")
                        }
                        if (!complete) {
                            pendingProducer = producer
                        }
                        unknown = false
                        break
                    }
                }
                if (unknown) {
                    unknownBuilder.append(context.reader().read())
                }
            }
            if (argSet.contains("--eol")) {
                context.token(EOL, context.reader().getLine(), context.reader().getColumn(), "EOL")
            }
            tokens.addAll(lineTokens)
        }

        if (tokens.peekLast().type == EOL) {
            val t = tokens.pollLast()
            tokens.add(Token.unit(EOF, t.line(), t.column(), "EOF"))
        }

        val size = tokens.size

        if (argSet.contains("--print-tokens")) {
            for (token in tokens) {
                println(token)
            }
        }

        val cs = if (argSet.contains("--count-tokens")) {
            val counts: MutableMap<TokenType<*>, AtomicInteger> = mutableMapOf()
            for (token in ArrayList(tokens)) {
                counts.computeIfAbsent(token.type) { _ -> AtomicInteger(0) }.incrementAndGet()
            }
            counts.entries.map { it.key to it.value }.sortedWith { aa, bb -> (aa.second.get().compareTo(bb.second.get()) * -1) }
        } else {
            listOf()
        }
        val queue = TokenQueue.Impl(tokens)

        val c = System.currentTimeMillis()

        val buildResult = consumer.consume(queue, argSet)


        val d = System.currentTimeMillis()

        return buildResult.map { result -> Result(result, size, "Timing: 1:${b - a}ms / 2:${c - b}ms / 3:${d - c}ms", cs) }
    }

    fun addTokenizer(type: ParseTokenizerType, providers: List<TokenProducer>) {
        parseSpecs[type] = Spec(providers)
    }

    open class NumberValue(private val value: Number) {
        fun getValue(): Number {
            return value
        }

        override fun toString(): String {
            return value.toString()
        }
    }

    class Result<T: Any>(val value: T, val size: Int, val timings: String, val counts: Collection<Pair<TokenType<*>, AtomicInteger>>)

    private class Spec(val producers: List<TokenProducer>)
}