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
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.lang.IllegalArgumentException
import java.util.*
import java.util.function.Predicate

class BooleanParseStrategy private constructor(type: TokenType<Boolean>,
                                               delimiter: Predicate<Token<*>>?,
                                               errorGates: List<Predicate<Token<*>>>,
                                               unknownStrategy: UnknownStrategy,
                                               provider: ((Boolean) -> List<Token<*>>)?)
    : BaseParseStrategy<Boolean, BooleanParseStrategy.StrategyBuilder>("Boolean", type, delimiter, errorGates, unknownStrategy, provider) {

    override fun builder(): StrategyBuilder {
        return StrategyBuilder()
    }

    override fun processToken(builder: StrategyBuilder, token: Token<*>, args: Array<String>): Optional<ValidationResult<StrategyBuilder>> {
        if(builder.accept(token.value as Boolean)) {
            return Optional.of(ValidationResult.error(builder, "Multiple number tokens found at ln${token.line()}/col${token.column()}, expected 1"))
        }
        return Optional.empty()
    }

    override fun convertToken(builder: StrategyBuilder, token: Token<*>): Optional<ValidationResult<StrategyBuilder>> {
        try {
            val n = token.asString().lowercase().toBooleanStrictOrNull()
            if (n != null) {
                builder.accept(n)
            }
        } catch (_: Exception) {
            //nop
        }
        return Optional.empty()
    }

    inner class StrategyBuilder: ParseStrategy.Builder<Boolean> {

        private var bl: Boolean = false
        private var accepted = false

        fun accept(bl: Boolean): Boolean {
            if (accepted) {
                return true
            }
            this.bl = bl
            accepted = true
            return false
        }

        override fun build(): ValidationResult<Boolean> {
            return ValidationResult.success(bl)
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
            private var type: TokenType<Boolean>? = null
            private var unknownStrategy: UnknownStrategy = UnknownStrategy.THROW
            private var provider: ((Boolean) -> List<Token<*>>)? = null

            fun type(type: TokenType<Boolean>): Builder {
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

            fun provider(provider: ((Boolean) -> List<Token<*>>)): Builder {
                this.provider = provider
                return this
            }

            fun build(): BooleanParseStrategy {
                if (type == null) throw IllegalArgumentException("Number strategy builder needs 'type' defined")
                if (eolBreak) {
                    errorGates.add { t -> t.type.isSpecial() }
                }
                return BooleanParseStrategy(type!!, delimiter, errorGates, unknownStrategy, provider)
            }
        }
    }


}