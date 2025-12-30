/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.strategy.strategies

import me.fzzyhmstrs.fzzy_config.theme.css2.strategy.ParseStrategy
import me.fzzyhmstrs.fzzy_config.theme.css2.token.Token
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.predicated
import java.util.*
import java.util.function.Predicate

/**
 *
 */
class PairParseStrategy<A: Any, B: Any> (
    private val aStrategy: () -> ParseStrategy<A, ParseStrategy.Builder<A>>,
    private val bStrategy: () -> ParseStrategy<B, ParseStrategy.Builder<B>>,
    private val delimiter: Predicate<Token<*>>? = null,
    private val delimiterToken: () -> List<Token<*>> = { listOf() }): ParseStrategy<Optional<Pair<A, B>>, PairParseStrategy.StrategyBuilder<A, B>> {

    override fun id(): String {
        return "Pair"
    }

    override fun builder(): StrategyBuilder<A, B> {
        return StrategyBuilder()
    }

    override fun canProcessToken(token: Token<*>, args: Array<String>): Boolean {
        return aStrategy().canProcessToken(token, args) //we can start processing when the head of the tuple can go
    }

    override fun processTokens(
        builder: StrategyBuilder<A, B>,
        tokens: TokenQueue,
        args: Array<String>,
        errored: Boolean
    ): ValidationResult<StrategyBuilder<A, B>> {
        val ln = tokens.peek().line()
        val col = tokens.peek().column()
        val aStrat = aStrategy()
        val bStrat = bStrategy()
        var e = errored
        val slicePredicate = delimiter ?: Predicate { t -> bStrat.canProcessToken(t, args) }
        val aCheck = tokens.slice(slicePredicate) { tq ->
            Optional.of(aStrat.startProcessingTokens(tq, args, e))
        }
        if (aCheck.isEmpty) {
            return ValidationResult.error(builder, "Pair split not found")
        }
        if (aCheck.get().isError()) {
            e = true
        }
        val a = aCheck.get().map { it.build().get() }
        builder.a(a)
        tokens.consumeWhitespace()
        val unknownDelimiterTokens: MutableList<Token<*>> = mutableListOf()
        if (delimiter != null) {
            while (tokens.canPoll() && !delimiter.test(tokens.peek())) {
                unknownDelimiterTokens.add(tokens.poll())
            }
            tokens.poll() //poll the delimiter. We've already gated the case of the delimiter simply not being found at all
        }
        builder.b(bStrat.startProcessingTokens(tokens, args, e).map { it.build().get() }) //TODO improve this error collection
        return predicated(builder, (!builder.isError()) && (!e) && unknownDelimiterTokens.isEmpty(), "Errors parsing tuple at ln$ln/col$col/ unknown tokens:$unknownDelimiterTokens")
    }

    override fun provideTokens(value: Optional<Pair<A, B>>): List<Token<*>> {
        val list: MutableList<Token<*>> = mutableListOf()
        value.ifPresent { p ->
            list.addAll(aStrategy().provideTokens(p.first))
            list.addAll(delimiterToken())
            list.addAll(bStrategy().provideTokens(p.second))
        }
        return list
    }

    class StrategyBuilder<A: Any, B: Any>: ParseStrategy.Builder<Optional<Pair<A, B>>> {
        private var a: ValidationResult<A>? = null
        private var b: ValidationResult<B>? = null

        fun a(a: ValidationResult<A>): StrategyBuilder<A, B> {
            this.a = a
            return this
        }

        fun b(b: ValidationResult<B>): StrategyBuilder<A, B> {
            this.b = b
            return this
        }

        fun isError(): Boolean {
            return a == null || b == null || a?.isError() == true || b?.isError() == true
        }

        override fun build(): ValidationResult<Optional<Pair<A, B>>> {
            val aa = a
            val bb = b
            if (aa == null || bb == null) return ValidationResult.error(Optional.empty(), "Couldn't construct pair")
            val totalError = if (aa.isError()) {
                if (bb.isError()) {
                    aa.getError() + " " + bb.getError()
                } else {
                    aa.getError()
                }
            } else {
                bb.getError()
            }
            return predicated(Optional.of(aa.get() to bb.get()), totalError.isEmpty(), totalError)
        }
    }
}