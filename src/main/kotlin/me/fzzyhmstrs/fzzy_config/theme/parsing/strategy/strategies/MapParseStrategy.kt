/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.strategies

import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.ParseStrategy
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.util.*

/**
 *
 */
class MapParseStrategy<A: Any, B: Any> private constructor(private val listParser: ListParseStrategy<Optional<Pair<A, B>>>)
    : ParseStrategy<Map<A, B>, MapParseStrategy.StrategyBuilder<A, B>>
{

    override fun id(): String {
        return "Map"
    }

    override fun builder(): StrategyBuilder<A, B> {
        return StrategyBuilder()
    }

    override fun canProcessToken(token: Token<*>, args: Array<String>): Boolean {
        return listParser.canProcessToken(token, args) //we can start processing when the head of the tuple can go
    }

    override fun processTokens(
        builder: StrategyBuilder<A, B>,
        tokens: TokenQueue,
        args: Array<String>,
        errored: Boolean
    ): ValidationResult<StrategyBuilder<A, B>> {
        if (!tokens.canPoll()) return ValidationResult.error(builder, "No tokens available")
        val ln = tokens.peek().line()
        val col = tokens.peek().column()
        val listResult = listParser.startProcessingTokens(tokens, args, errored)
        builder.apply(listResult.map { it.build().get() })
        return ValidationResult.predicated(builder, listResult.isValid(), "Errors found parsing map at ln$ln/col$col")
    }

    override fun provideTokens(value: Map<A, B>): List<Token<*>> {
        val input = value.map { (k, v) -> Optional.of(k to v) }
        return listParser.provideTokens(input)
    }

    class StrategyBuilder<A: Any, B: Any>: ParseStrategy.Builder<Map<A, B>> {
        private var list: ValidationResult<List<Optional<Pair<A, B>>>> = ValidationResult.success(listOf())

        fun apply(list: ValidationResult<List<Optional<Pair<A, B>>>>): StrategyBuilder<A, B> {
            this.list = list
            return this
        }

        override fun build(): ValidationResult<Map<A, B>> {
            val l = list.get()
            var emptyCount = 0
            val seenKeys: MutableSet<A> = mutableSetOf()
            val map: MutableMap<A, B> = mutableMapOf()
            for (o in l) {
                if (o.isEmpty) {
                    emptyCount++
                    continue
                }
                val k = o.get().first
                val v = o.get().second
                val prev = map.put(k, v)
                if (prev != null) {
                    seenKeys.add(k)
                }
            }
            val totalError = if (list.isError()) {
                if (emptyCount != 0) {
                    if (seenKeys.isNotEmpty()) {
                        "Parse errors: ${list.getError()}, $emptyCount key-value pairs missed, Duplicated keys found: $seenKeys"
                    } else {
                        "Parse errors: ${list.getError()}, $emptyCount key-value pairs missed"
                    }
                } else if (seenKeys.isNotEmpty()) {
                    "Parse errors: ${list.getError()}, Duplicated keys found: $seenKeys"
                } else {
                    "Parse errors: ${list.getError()}"
                }
            } else if (emptyCount != 0) {
                if (seenKeys.isNotEmpty()) {
                    "$emptyCount key-value pairs missed, Duplicated keys found: $seenKeys"
                } else {
                    "$emptyCount key-value pairs missed"
                }
            } else if (seenKeys.isNotEmpty()) {
                "Duplicated keys found: $seenKeys"
            } else {
                ""
            }
            return ValidationResult.predicated(map, totalError.isEmpty(), totalError)
        }
    }
}