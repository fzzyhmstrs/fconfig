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
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.lang.UnsupportedOperationException
import java.util.Optional
import java.util.function.Predicate

abstract class BaseParseStrategy<T: Any, B: ParseStrategy.Builder<T>>(
    private val id: String,
    protected val type: TokenType<T>,
    protected val delimiter: Predicate<Token<*>>?,
    protected val errorGates: List<Predicate<Token<*>>>,
    protected val unknownStrategy: UnknownStrategy,
    protected val provider: ((T) -> List<Token<*>>)?): ParseStrategy<T, B>
{

    protected val errorGate: Predicate<Token<*>> by lazy {
        if (errorGates.isEmpty()) {
            Predicate { _ -> false }
        } else if (errorGates.size == 1) {
            errorGates[0]
        } else {
            var predicate = errorGates[0]
            for (i in 1..errorGates.lastIndex) {
                predicate = predicate.or(errorGates[i])
            }
            predicate
        }
    }

    override fun id(): String {
        return id
    }

    override fun canProcessToken(token: Token<*>, args: Array<String>): Boolean {
        return delimiter?.test(token) ?: (type == token.type)
    }

    override fun provideTokens(value: T): List<Token<*>> {
        return provider?.invoke(value) ?: throw UnsupportedOperationException("This strategy doesn't support encoding")
    }

    override fun processTokens(builder: B, tokens: TokenQueue, args: Array<String>, errored: Boolean): ValidationResult<B> {
        if (delimiter != null && tokens.canPoll()) {
            tokens.poll()
        }
        while (tokens.canPoll()) {
            val token = tokens.poll()
            val delimiterCheck = processDelimiters(builder, token, tokens)
            if (delimiterCheck.isPresent) {
                return delimiterCheck.get()
            }
            if (token.type == type) {
                val typeCheck = processToken(builder, token, args)
                if (typeCheck.isPresent) {
                    return typeCheck.get()
                }
            } else {
                when (unknownStrategy) {
                    UnknownStrategy.THROW -> return ValidationResult.error(builder, "Unknown token for $id construction $token found at ln${token.line()}/col${token.column()}")
                    UnknownStrategy.IGNORE -> {
                        if (!tokens.canPoll() && delimiter != null) {
                            return ValidationResult.error(builder, "Unclosed $id found at ln${token.line()}/col${token.column()}")
                        }
                    }
                    UnknownStrategy.CONVERT -> {
                        val convertCheck = convertToken(builder, token)
                        if (convertCheck.isPresent) {
                            return convertCheck.get()
                        }
                    }
                }
            }
        }
        return if (delimiter != null) {
            ValidationResult.error(builder, "Unclosed token stream found")
        } else {
            ValidationResult.success(builder)
        }
    }

    protected fun processDelimiters(builder: B, token: Token<*>, tokens: TokenQueue): Optional<ValidationResult<B>> {
        if (errorGate.test(token)) {
            return if (delimiter != null) {
                Optional.of(ValidationResult.error(builder, "Unclosed number found at ln${token.line()}/col${token.column()}"))
            } else {
                Optional.of(ValidationResult.success(builder))
            }
        } else if (delimiter?.test(token) == true) {
            return Optional.of(ValidationResult.success(builder))
        } else if (token.type.isSpecial()) {
            if (!tokens.canPoll() && delimiter != null) {
                return Optional.of(ValidationResult.error(builder, "Unclosed string found at ln${token.line()}/col${token.column()}"))
            }
        }
        return Optional.empty()
    }

    abstract fun processToken(builder: B, token: Token<*>, args: Array<String>): Optional<ValidationResult<B>>
    abstract fun convertToken(builder: B, token: Token<*>): Optional<ValidationResult<B>>

    enum class UnknownStrategy {
        THROW,
        IGNORE,
        CONVERT
    }

}