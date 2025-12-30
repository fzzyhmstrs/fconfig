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
import org.jetbrains.annotations.ApiStatus.Internal
import java.lang.Appendable
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.util.*
import java.util.function.Predicate

class StringParseStrategy private constructor(type: TokenType<String>,
                                              delimiter: Predicate<Token<*>>?,
                                              errorGates: List<Predicate<Token<*>>>,
                                              unknownStrategy: UnknownStrategy,
                                              provider: ((String) -> List<Token<*>>)?)
    : BaseParseStrategy<String, StringParseStrategy.StrategyBuilder>("String", type, delimiter, errorGates, unknownStrategy, provider)
{

    override fun builder(): StrategyBuilder {
        return StrategyBuilder()
    }

    override fun processToken(builder: StrategyBuilder, token: Token<*>, args: Array<String>): Optional<ValidationResult<StrategyBuilder>> {
        builder.append(if (args.contains("--quote-strings")) token.asString() else token.value as String)
        return Optional.empty()
    }

    override fun convertToken(builder: StrategyBuilder, token: Token<*>): Optional<ValidationResult<StrategyBuilder>> {
        builder.append(token.asString())
        return Optional.empty()
    }

    @Internal
    inner class StrategyBuilder: ParseStrategy.Builder<String>, Appendable {

        private val strBuilder = StringBuilder()

        override fun append(csq: CharSequence?): StrategyBuilder {
            strBuilder.append(csq)
            return this
        }

        override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
            strBuilder.append(csq, start, end)
            return this
        }

        override fun append(c: Char): Appendable {
            strBuilder.append(c)
            return this
        }

        override fun build(): ValidationResult<String> {
            return ValidationResult.success(strBuilder.toString())
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
            private var type: TokenType<String>? = null
            private var unknownStrategy: UnknownStrategy = UnknownStrategy.THROW
            private var provider: ((String) -> List<Token<*>>)? = null

            fun type(type: TokenType<String>): Builder {
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

            fun provider(provider: ((String) -> List<Token<*>>)): Builder {
                this.provider = provider
                return this
            }

            fun build(): StringParseStrategy {
                if (type == null) throw IllegalArgumentException("String strategy builder needs 'type' defined")
                if (eolBreak) {
                    errorGates.add { t -> t.type.isSpecial() }
                }
                return StringParseStrategy(type!!, delimiter, errorGates, unknownStrategy, provider)
            }
        }
    }
}