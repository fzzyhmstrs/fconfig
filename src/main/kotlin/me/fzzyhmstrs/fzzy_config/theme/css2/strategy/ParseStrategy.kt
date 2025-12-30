/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.strategy

import me.fzzyhmstrs.fzzy_config.theme.css2.token.Token
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

/*
* Create a parse strategy system that allows you to compose micro strategies into a total strategy builder
* Make it like a RecordBuilderCodec in the sense that you can compose strategies out of "steps", which are themselves strategies
* * Lists -> define a start token, stop token, delimiter token, list element strategy, and whitespace strategy
* * Keyed -> creates a key-pair tuple with a key strategy, a divider strategy, a key->value function strategy
*                The key strategy defines a key that is used by the k->v function to find a value strategy
* * Map -> Uses a Keyed plus a start, stop, and delimiter
* * Group -> A structure with some start and stop delimiters that mark the start/stop but otherwise just wrap the internals
*                Probably would be used internally for standard list/map/etc. impls
* * Alternatives -> A set of valid alternatives. Will attempt to parse
* * Basic Type -> simple strategy to convert a set of tokens into a basic type
*                String
*                Number
*                Boolean
* */

interface ParseStrategy<T: Any, B: ParseStrategy.Builder<T>> {

    fun id(): String
    fun builder(): B

    fun canProcessToken(token: Token<*>, args: Array<String>): Boolean
    fun processTokens(builder: B, tokens: TokenQueue, args: Array<String>, errored: Boolean = false): ValidationResult<B>
    fun startProcessingTokens(tokens: TokenQueue, args: Array<String>, errored: Boolean = false): ValidationResult<B> {
        return processTokens(builder(), tokens, args, errored)
    }

    fun provideTokens(value: T): List<Token<*>>

    interface Builder<T: Any> {
        fun build(): ValidationResult<T>
    }
}