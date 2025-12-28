/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.token

import java.lang.IllegalStateException
import java.util.*
import java.util.function.Predicate

sealed class TokenQueue(protected val tokens: LinkedList<Token<*>>) {

    fun split(splitConsumer: (Split) -> Unit) {
        val split = Split(LinkedList(tokens), this)
        splitConsumer(split)
    }

    fun slice(sliceTo: Predicate<Token<*>>, sliceConsumer: (TokenQueue) -> Unit) {
        val split = Impl(LinkedList(tokens))
        sliceConsumer(split)
    }

    class Impl internal constructor(tokens: LinkedList<Token<*>>): TokenQueue(tokens)

    class Split internal constructor(tokens: LinkedList<Token<*>>, private val parent: TokenQueue): TokenQueue(tokens) {

        private var commited = false

        fun commit() {
            parent.tokens.clear()
            parent.tokens.addAll(this.tokens)
            commited = true
        }

        override fun canPoll(): Boolean {
            if (commited) throw IllegalStateException("Operation on TokenQueue Split after committing")
            return super.canPoll()
        }

        override fun peek(): Token<*> {
            if (commited) throw IllegalStateException("Operation on TokenQueue Split after committing")
            return super.peek()
        }

        override fun poll(): Token<*> {
            if (commited) throw IllegalStateException("Operation on TokenQueue Split after committing")
            return super.poll()
        }
    }

    fun isEOL(): Boolean {
        return canPoll() && peek().type.isSpecial()
    }

    fun consumeWhitespace() {
        while (canPoll()) {
            if (!tokens.peek().type.isWhitespace()) {
                return
            }
            poll()
        }
    }

    open fun canPoll(): Boolean {
        return tokens.isNotEmpty()
    }

    open fun peek(): Token<*> {
        return tokens.element()
    }

    open fun poll(): Token<*> {
        return tokens.remove()
    }
}