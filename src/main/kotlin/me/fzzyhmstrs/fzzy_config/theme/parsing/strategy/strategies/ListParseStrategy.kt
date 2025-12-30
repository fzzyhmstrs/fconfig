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
import java.util.function.Predicate

/**
 * Parses a list of like objects, optionally bound by delimiters, separated by a separator, and gated by some state that marks the end of the list (this may be/seem redundant, but will at the very least help recover parse errors if desired)
 */
class ListParseStrategy<A: Any> private constructor(
    private val strategy: () -> ParseStrategy<A, ParseStrategy.Builder<A>>,
    private val startDelimiter: Predicate<Token<*>>?,
    private val endDelimiter: Predicate<Token<*>>?,
    private val separator: Predicate<Token<*>>,
    private val endGates: List<Predicate<Token<*>>>,
    private val provider: (List<A>) -> List<Token<*>>,
    private val skipErrors: Boolean,
    vararg endGateBehavior: EndGateBehavior)
    : ParseStrategy<List<A>, ListParseStrategy<A>.StrategyBuilder<A>>
{

    private val flags = endGateBehavior.fold(0) { f, b -> f or b.flag }

    private val endGate: Predicate<Token<*>> by lazy {
        if (endGates.isEmpty()) {
            Predicate { _ -> false }
        } else if (endGates.size == 1) {
            endGates[0]
        } else {
            var predicate = endGates[0]
            for (i in 1..endGates.lastIndex) {
                predicate = predicate.or(endGates[i])
            }
            predicate
        }
    }

    override fun id(): String {
        return "List"
    }

    override fun builder(): StrategyBuilder<A> {
        return StrategyBuilder()
    }

    override fun canProcessToken(token: Token<*>, args: Array<String>): Boolean {
        if (startDelimiter != null) {
            return startDelimiter.test(token)
        }
        return strategy().canProcessToken(token, args)
    }

    override fun processTokens(
        builder: StrategyBuilder<A>,
        tokens: TokenQueue,
        args: Array<String>,
        errored: Boolean
    ): ValidationResult<StrategyBuilder<A>> {
        if (!tokens.canPoll()) return ValidationResult.error(builder, "No tokens available")
        val ln = tokens.peek().line()
        val col = tokens.peek().column()
        if (startDelimiter != null) {
            tokens.poll() //consume the found start delimiter
        }
        val slicer = if (EndGateBehavior.SLICE.hasFlag(flags)) {
            if (endDelimiter != null) {
                separator.or(endDelimiter).or(endGate)
            } else {
                separator.or(endGate)
            }
        } else {
            if (endDelimiter != null) {
                separator.or(endDelimiter)
            } else {
                separator
            }
        }

        fun checkEnds(tokens: TokenQueue, e: Boolean):Optional<ValidationResult<StrategyBuilder<A>>> {
            if (endDelimiter?.test(tokens.peek()) == true) {
                tokens.poll() //consume the end delimiter
                return Optional.of(ValidationResult.predicated(builder, !e, "Errors while parsing list at ln${tokens.peek().line()}/col${tokens.peek().column()}"))
            }
            if (endGate.test(tokens.peek())) {
                if (EndGateBehavior.POLL.hasFlag(flags)) {
                    tokens.poll()
                }
                return if (EndGateBehavior.ERROR.hasFlag(flags)) {
                    Optional.of(ValidationResult.error(builder, "List not properly closed at ln${tokens.peek().line()}/col${tokens.peek().column()}"))
                } else {
                    Optional.of(ValidationResult.predicated(builder, !e, "Errors while parsing list at ln${tokens.peek().line()}/col${tokens.peek().column()}"))
                }
            }
            return Optional.empty()
        }

        val unknownTokens: MutableList<Token<*>> = mutableListOf()

        fun TokenQueue.consumeUnknowns(test: Predicate<Token<*>>) {
            while (this.canPoll()) {
                val t = tokens.peek()
                if (t.type.isWhitespace() || tokens.peek().type.isSpecial()) {
                    tokens.poll()
                } else if (!test.test(tokens.peek())) {
                    unknownTokens.add(tokens.poll())
                } else {
                    break
                }
            }
        }

        val strat = strategy()
        var e = false

        while (tokens.canPoll()) {
            val check = tokens.slice(slicer) { slice ->
                slice.consumeUnknowns { t -> strat.canProcessToken(t, args) }
                val result = strat.startProcessingTokens(slice, args, e || errored)
                if (result.isError()) e = true
                Optional.of(result)
            }
            if (check.isEmpty) { //
                while (tokens.canPoll()) {
                    val endCheck = checkEnds(tokens, e)
                    if (endCheck.isPresent) {
                        return endCheck.get()
                    }
                    tokens.consumeWhitespace()
                }
            }
            val endCheck = checkEnds(tokens, e)
            if (endCheck.isPresent) {
                return endCheck.get()
            }
            tokens.consumeUnknowns(separator)
            if (separator.test(tokens.peek())) {
                tokens.poll() //consumer the separator
            }
            builder.add(check.get().map { it.build() })
        }

        return ValidationResult.predicated(builder, !e && unknownTokens.isEmpty(), "Errors found while parsing list at ln$ln/col$col/ unknown tokens:$unknownTokens")
    }

    override fun provideTokens(value: List<A>): List<Token<*>> {
        return provider(value)
    }

    enum class EndGateBehavior(val flag: Int) {
        POLL(1),
        ERROR(2),
        SLICE(4);

        fun hasFlag(flags: Int): Boolean {
            return flag and flags == flag
        }
    }

    inner class StrategyBuilder<A: Any>: ParseStrategy.Builder<List<A>> {
        private var entries: MutableList<ValidationResult<A>> = mutableListOf()

        fun add(a: ValidationResult<ValidationResult<A>>): StrategyBuilder<A> {
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
            this.entries.add(ValidationResult.error(inner.get(), totalError))
            return this
        }

        override fun build(): ValidationResult<List<A>> {
            val errors: MutableList<String> = mutableListOf()
            val list: MutableList<A> = mutableListOf()
            for (a in entries) {
                if (a.isError()) {
                    errors.add(a.getError())
                    if (this@ListParseStrategy.skipErrors) {
                        continue
                    }
                }
                list.add(a.get())
            }
            return ValidationResult.predicated(list, errors.isEmpty(),  if(this@ListParseStrategy.skipErrors) "Errors found constructing list: $errors, skipping entries" else "Errors found constructing list: $errors")
        }
    }

    companion object {

        fun <A: Any> builder(strategy: () -> ParseStrategy<A, ParseStrategy.Builder<A>>, separator: Predicate<Token<*>>): Builder<A> {
            return Builder(strategy, separator)
        }

        class Builder<A: Any> internal constructor(
            private val strategy: () -> ParseStrategy<A, ParseStrategy.Builder<A>>,
            private val separator: Predicate<Token<*>>)
        {
            private var startDelimiter: Predicate<Token<*>>? = null
            private var endDelimiter: Predicate<Token<*>>? = null
            private var endGates: MutableList<Predicate<Token<*>>> = mutableListOf()
            private var provider: ((List<A>) -> List<Token<*>>) = { _ -> listOf() }
            private var skipErrors = false
            private val endGateBehaviors: MutableSet<EndGateBehavior> = mutableSetOf()

            fun delimiter(delimiter: Predicate<Token<*>>): Builder<A> {
                this.startDelimiter = delimiter
                this.endDelimiter = delimiter
                return this
            }

            fun delimiters(startDelimiter: Predicate<Token<*>>, endDelimiter: Predicate<Token<*>>): Builder<A> {
                this.startDelimiter = startDelimiter
                this.endDelimiter = endDelimiter
                return this
            }

            fun endGate(endGate: Predicate<Token<*>>): Builder<A> {
                this.endGates.add(endGate)
                return this
            }

            fun provider(provider: (List<A>) -> List<Token<*>>): Builder<A> {
                this.provider = provider
                return this
            }

            fun skipErrors(): Builder<A> {
                this.skipErrors = true
                return this
            }

            fun endGateBehavior(endGateBehavior: EndGateBehavior): Builder<A> {
                this.endGateBehaviors.add(endGateBehavior)
                return this
            }

            fun build(): ListParseStrategy<A> {
                return ListParseStrategy(strategy, startDelimiter, endDelimiter, separator, endGates, provider, skipErrors, *endGateBehaviors.toTypedArray())
            }

        }

    }
}