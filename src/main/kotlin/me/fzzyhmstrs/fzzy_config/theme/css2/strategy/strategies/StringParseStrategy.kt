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
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import org.jetbrains.annotations.ApiStatus.Internal
import java.lang.Appendable
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.lang.UnsupportedOperationException
import java.util.function.Predicate

class StringParseStrategy private constructor(private val type: TokenType<String>, private val delimiter: Predicate<Token<*>>?, private val errorGates: List<Predicate<Token<*>>>, private val unknownStrategy: UnknownStrategy, private val provider: ((String) -> List<Token<*>>)?): ParseStrategy<String, StringParseStrategy.StrategyBuilder> {

    private val errorGate: Predicate<Token<*>> by lazy {
        if (errorGates.isEmpty()) {
            Predicate { _ -> false }
        } else if (errorGates.size == 1) {
            errorGates[0]
        } else {
            var predicate = errorGates[0]
            for (i in 1..errorGates.lastIndex) {
                predicate = predicate.and(errorGates[i])
            }
            predicate
        }
    }

    override fun id(): String {
        return "String"
    }

    override fun builder(): StrategyBuilder {
        return StrategyBuilder()
    }

    override fun canProcessToken(tokens: TokenQueue, args: Array<String>): Boolean {
        return tokens.canPoll() && delimiter?.test(tokens.peek()) ?: (type == tokens.peek().type)
    }

    override fun provideTokens(value: String): List<Token<*>> {
        return provider?.invoke(value) ?: throw UnsupportedOperationException("This strategy doesn't support encoding")
    }

    override fun processTokens(builder: StrategyBuilder, tokens: TokenQueue, args: Array<String>, errored: Boolean): ValidationResult<StrategyBuilder> {
        if (delimiter != null && tokens.canPoll()) {
            tokens.poll()
        }
        while (tokens.canPoll()) {
            val token = tokens.poll()
            if (errorGate.test(token)) {
                return if (delimiter != null) {
                    ValidationResult.error(builder, "Unclosed string found at ln${token.line()}/col${token.column()}")
                } else {
                    ValidationResult.success(builder)
                }
            } else if (delimiter?.test(token) == true) {
                return ValidationResult.success(builder)
            } else if (token.type == type) {
                builder.append(if (args.contains("--quote-strings")) token.asString() else token.value as String)
            } else {
                if (token.type.isSpecial()) {
                    if (!tokens.canPoll() && delimiter != null) {
                        return ValidationResult.error(builder, "Unclosed string found at ln${token.line()}/col${token.column()}")
                    }
                }
                when (unknownStrategy) {
                    UnknownStrategy.THROW -> return ValidationResult.error(builder, "Unknown token for string construction $token found at ln${token.line()}/col${token.column()}")
                    UnknownStrategy.IGNORE -> {
                        if (!tokens.canPoll() && delimiter != null) {
                            ValidationResult.error(builder, "Unclosed string found at ln${token.line()}/col${token.column()}")
                        }
                    }
                    UnknownStrategy.CONVERT -> builder.append(token.asString())
                }
            }
        }
        return if (delimiter != null) {
            ValidationResult.error(builder, "Unclosed string found")
        } else {
            ValidationResult.success(builder)
        }
    }

    @Internal
    @Deprecated("Internal use only")
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

    enum class UnknownStrategy {
        THROW,
        IGNORE,
        CONVERT
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