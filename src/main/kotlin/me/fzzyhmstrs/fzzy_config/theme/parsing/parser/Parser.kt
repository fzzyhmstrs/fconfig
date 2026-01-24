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
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.ParseStrategy
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.*
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.io.BufferedReader
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

//non-selector strings aren't giving back their quotes
//multi-line comments not handled properly

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

    fun <T: Any, B: ParseStrategy.Builder<T>> parse(input: BufferedReader, type: ParseTokenizerType, strategy: ParseStrategy<T, B>, vararg args: String): ValidationResult<Pair<T, Int>> {
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
            context.token(EOL, context.reader().getLine(), context.reader().getColumn(), "EOL")
            tokens.addAll(lineTokens)
        }

        if (tokens.peekLast().type == EOL) {
            val t = tokens.pollLast()
            tokens.add(Token.unit(EOF, t.line(), t.column(), "EOF"))
        }

        val size = tokens.size

        if (args.contains("-print-tokens")) {
            for (token in tokens) {
                println(token)
            }
        }

        val queue = TokenQueue.Impl(tokens)

        val c = System.currentTimeMillis()

        @Suppress("UNCHECKED_CAST")
        val buildResult = strategy.startProcessingTokens(queue, args as Array<String>)

        val d = System.currentTimeMillis()

        println("Timing: 1:${b - a}ms / 2:${c - b}ms / 3:${d - c}ms")

        return buildResult.map { builder -> builder.build().map{ result -> result to size } }.get()
    }

    fun addTokenizer(type: ParseTokenizerType, providers: List<TokenProducer>) {
        parseSpecs[type] = Spec(providers)
    }

    private fun BufferedReader.indexedLines(): Stream<Pair<Int, String>> {
        val itr: Iterator<Pair<Int, String>> = object : Iterator<Pair<Int, String>> {
            var nextLine: String? = null
            var index = 0

            override fun hasNext(): Boolean {
                return if (nextLine != null) {
                    true
                } else {
                    generateNext() != null
                }
            }

            private fun generateNext(): String? {
                return try {
                    val nl = readLine()
                    nextLine = nl
                    nl
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }

            override fun next(): Pair<Int, String> {
                val line = nextLine ?: generateNext()
                if (line != null) {
                    nextLine = null
                    return index++ to line
                } else {
                    throw NoSuchElementException()
                }
            }
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                itr, Spliterator.ORDERED or Spliterator.NONNULL), false)
    }

    open class NumberValue(private val value: Number) {
        fun getValue(): Number {
            return value
        }

        override fun toString(): String {
            return value.toString()
        }
    }

    private class Spec(val producers: List<TokenProducer>)
}