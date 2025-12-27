/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.test

import me.fzzyhmstrs.fzzy_config.theme.css2.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.css2.ParseTokenizerType
import me.fzzyhmstrs.fzzy_config.theme.css2.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.css2.token.*
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.io.BufferedReader
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

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

    private val UNKNOWN = TokenType("Unknown", UNKNOWN_TYPE, true)

    val NOTHING_VALUE = TokenValue<Unit>("Nothing")
    val STRING_VALUE = TokenValue<String>("String")
    val NUMBER_VALUE = TokenValue<NumberValue>("Number")
    val NUMBER_TOKEN_VALUE = TokenValue<Token<out NumberValue>>("Number Token")

    fun <T: Any, B: ParseStrategy.Builder<T>> parse(input: BufferedReader, type: ParseTokenizerType, strategy: ParseStrategy<T, B>, vararg args: String): ValidationResult<T> {
        val spec = parseSpecs[type] ?: throw IllegalStateException("Parse spec ${type.id()} not registered")
        val builder = strategy.builder()
        val tokens = input.use { inputReader ->
            inputReader.indexedLines()
                .mapMulti { (index, line), consumer ->
                    val context = object : ParseContext() {
                        val reader = StringReader(type.filterInput(line), index + 1)

                        override fun reader(): StringReader {
                            return reader
                        }

                        override fun token(token: Token<*>) {
                            consumer.accept(token)
                        }
                    }

                    var unknownBuilder = StringBuilder()

                    while (context.reader().canRead()) {
                        var unknown = true
                        for (provider in spec.producers) {
                            if (provider.canProduce(context.reader())) {
                                if (unknownBuilder.isNotEmpty()) {
                                    context.token(UNKNOWN, STRING_VALUE, unknownBuilder.toString())
                                    unknownBuilder = StringBuilder()
                                }
                                val before = context.reader().getColumn()
                                provider.produce(context)
                                if (context.reader().getColumn() == before) {
                                    throw IllegalStateException("Provider ${provider.id()} didn't consume any input characters at column $before")
                                }
                                unknown = false
                                break
                            }
                        }
                        if (unknown) {
                            unknownBuilder.append(context.reader().read())
                        }
                    }
                }.collect(Collectors.toCollection(::LinkedList))
        }

        val queue = TokenQueue(tokens)

        @Suppress("UNCHECKED_CAST")
        val buildResult = strategy.processTokens(builder, queue, args as Array<String>)

        return buildResult.map { it.build() }

        /*val contexts = lines.mapIndexed { index, it ->
            object : ParseContext() {
                val reader = StringReader(it, index + 1)

                override fun reader(): StringReader {
                    return reader
                }

                override fun token(token: Token<*>) {
                    tokens.add(token)
                }
            }
        }



        for (context in contexts) {
            var unknownBuilder = StringBuilder()
            while (context.reader().canRead()) {
                var unknown = true
                for (provider in spec.producers) {
                    if (provider.canProduce(context.reader())) {
                        if (unknownBuilder.isNotEmpty()) {
                            context.token(UNKNOWN, STRING_VALUE, unknownBuilder.toString())
                            unknownBuilder = StringBuilder()
                        }
                        val before = context.reader().getColumn()
                        provider.produce(context)
                        if (context.reader().getColumn() == before) {
                            throw IllegalStateException("Provider ${provider.id()} didn't consume any input characters at column $before")
                        }
                        unknown = false
                        break
                    }
                }
                if (unknown) {
                    unknownBuilder.append(context.reader().read())
                }
            }
        }
        return tokens*/
    }

    fun addTokenizer(type: ParseTokenizerType, providers: List<TokenProducer>) {
        this.parseSpecs[type] = Spec(providers)
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
            return "Number($value)"
        }
    }

    private class Spec(val producers: List<TokenProducer>)
}