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
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.lang.IllegalArgumentException
import java.util.*
import java.util.function.Predicate

class NumberParseStrategy private constructor(type: TokenType<Number>,
                                              delimiter: Predicate<Token<*>>?,
                                              errorGates: List<Predicate<Token<*>>>,
                                              unknownStrategy: UnknownStrategy,
                                              provider: ((Number) -> List<Token<*>>)?)
    : BaseParseStrategy<Number, NumberParseStrategy.StrategyBuilder>("Number", type, delimiter, errorGates, unknownStrategy, provider) {

    override fun builder(): StrategyBuilder {
        return StrategyBuilder()
    }

    override fun processToken(builder: StrategyBuilder, token: Token<*>, args: Array<String>): Optional<ValidationResult<StrategyBuilder>> {
        if(builder.accept(token.value as Number)) {
            return Optional.of(ValidationResult.error(builder, "Multiple number tokens found at ln${token.line()}/col${token.column()}, expected 1"))
        }
        return Optional.empty()
    }

    override fun convertToken(builder: StrategyBuilder, token: Token<*>): Optional<ValidationResult<StrategyBuilder>> {
        try {
            val n = token.asString().toDoubleOrNull()
            if (n != null) {
                builder.accept(n)
            }
        } catch (_: Exception) {
            //nop
        }
        return Optional.empty()
    }

    inner class StrategyBuilder: ParseStrategy.Builder<Number> {

        private var number: Number = 0
        private var accepted = false

        fun accept(number: Number): Boolean {
            if (accepted) {
                return true
            }
            this.number = number
            accepted = true
            return false
        }

        override fun build(): ValidationResult<Number> {
            return ValidationResult.success(number)
        }

    }

    companion object {

        fun builder(): Builder {
            return Builder()
        }

        class Builder internal constructor() {
            private var eolBreak = true
            private val errorGates: MutableList<Predicate<Token<*>>> = mutableListOf()
            private var delimiter: Predicate<Token<*>>? = null
            private var type: TokenType<Number>? = null
            private var unknownStrategy: UnknownStrategy = UnknownStrategy.THROW
            private var provider: ((Number) -> List<Token<*>>)? = null

            fun type(type: TokenType<Number>): Builder {
                this.type = type
                return this
            }

            fun delimiter(predicate: Predicate<Token<*>>): Builder {
                delimiter = predicate
                return this
            }

            fun delimiter(type: TokenType<*>): Builder {
                delimiter = Predicate { t -> t.type == type }
                return this
            }

            fun errorOn(predicate: Predicate<Token<*>>): Builder {
                errorGates.add(predicate)
                return this
            }

            fun errorOn(type: TokenType<*>): Builder {
                errorGates.add(Predicate { t -> t.type == type })
                return this
            }

            fun ignoreEol(): Builder {
                eolBreak = false
                return this
            }

            fun unknownStrategy(unknownStrategy: UnknownStrategy): Builder {
                this.unknownStrategy = unknownStrategy
                return this
            }

            fun provider(provider: ((Number) -> List<Token<*>>)): Builder {
                this.provider = provider
                return this
            }

            fun build(): NumberParseStrategy {
                if (type == null) throw IllegalArgumentException("Number strategy builder needs 'type' defined")
                if (eolBreak) {
                    errorGates.add { t -> t.type.isSpecial() }
                }
                return NumberParseStrategy(type!!, delimiter, errorGates, unknownStrategy, provider)
            }
        }
    }


}