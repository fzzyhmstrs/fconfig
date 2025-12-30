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
import java.util.*

/**
 *
 */
class AlternativesParseStrategy<A: Any> (private val provider: (A) -> List<Token<*>>, private vararg val strategies: () -> ParseStrategy<A, out ParseStrategy.Builder<A>>): ParseStrategy<Optional<A>, AlternativesParseStrategy.StrategyBuilder<A>> {

    override fun id(): String {
        return "Alternatives"
    }

    override fun builder(): StrategyBuilder<A> {
        return StrategyBuilder()
    }

    override fun canProcessToken(token: Token<*>, args: Array<String>): Boolean {
        return strategies.any { it().canProcessToken(token, args) } //we can start processing when the head of the tuple can go
    }

    override fun processTokens(
        builder: StrategyBuilder<A>,
        tokens: TokenQueue,
        args: Array<String>,
        errored: Boolean
    ): ValidationResult<StrategyBuilder<A>> {
        if (!tokens.canPoll()) return ValidationResult.error(builder, "No tokens available")
        val startToken = tokens.peek()
        for (strategy in strategies) {
            val s = strategy()
            if (!s.canProcessToken(startToken, args)) continue
            val altCheck = tokens.split { split ->
                val result = s.startProcessingTokens(split, args, errored)
                if (result.isValid()) {
                    split.commit()
                    Optional.of(result)
                } else {
                    Optional.empty()
                }
            }
            if (altCheck.isPresent) {
                return ValidationResult.success(builder.a(altCheck.get().map { it.build() }))
            }
        }
        return ValidationResult.error(builder, "No valid alternatives")
    }

    override fun provideTokens(value: Optional<A>): List<Token<*>> {
        return value.map { provider(it) }.orElse(listOf())
    }

    class StrategyBuilder<A: Any>: ParseStrategy.Builder<Optional<A>> {
        private var a: ValidationResult<A>? = null

        fun a(a: ValidationResult<ValidationResult<A>>): StrategyBuilder<A> {
            val inner = a.get()
            val totalError = if (a.isError()) {
                if (inner.isError()) {
                    a.getError() + " " + inner.getError()
                } else {
                    a.getError()
                }
            } else {
                inner.getError()
            }
            this.a = ValidationResult.error(inner.get(), totalError)
            return this
        }

        override fun build(): ValidationResult<Optional<A>> {
            val aa = a ?: return ValidationResult.error(Optional.empty(), "Couldn't find alternative")
            return aa.map { a -> Optional.of(a) }
        }
    }
}