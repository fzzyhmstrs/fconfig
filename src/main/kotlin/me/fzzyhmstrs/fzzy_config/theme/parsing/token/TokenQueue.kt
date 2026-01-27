/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.token

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParsePrinter
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.ParseStrategy
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.lang.IllegalStateException
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate

sealed class TokenQueue(protected val tokens: LinkedList<Token<*>>): ParsePrinter {

    companion object {
        fun single(token: Token<*>): TokenQueue {
            return Impl(LinkedList(listOf(token)))
        }

        fun empty(): TokenQueue {
            return Impl(LinkedList(listOf()))
        }
    }

    fun <T: Any> attempt(attempt: (Split) -> ValidationResult<T>): ValidationResult<T> {
        val split = Split(LinkedList(tokens), this)
        val result = attempt(split)
        if (result.isValid()) {
            split.commit()
        }
        return result
    }

    fun <T: Any> split(splitConsumer: (Split) -> Optional<ValidationResult<out ParseStrategy.Builder<T>>>): Optional<ValidationResult<out ParseStrategy.Builder<T>>> {
        val split = Split(LinkedList(tokens), this)
        return splitConsumer(split)
    }

    fun <T: Any, B: ParseStrategy.Builder<T>> slice(sliceBefore: Predicate<Token<*>>, sliceConsumer: (Slice) -> Optional<ValidationResult<B>>): Optional<ValidationResult<B>> {
        val sliceIndex = tokens.withIndex().firstOrNull { (_, t) -> sliceBefore.test(t) }?.index?.minus(1) ?: return Optional.empty()
        if (sliceIndex < 0) return Optional.empty()

        val slice = Slice(LinkedList(tokens.subList(0, sliceIndex)), sliceIndex, this)
        return sliceConsumer(slice).also { slice.commit() }
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

    class Slice internal constructor(tokens: LinkedList<Token<*>>, private val maxIndex: Int, private val parent: TokenQueue): TokenQueue(tokens) {

        private var commited = false
        private var index = 0

        fun commit() {
            parent.tokens.clear()
            parent.tokens.addAll(this.tokens)
            commited = true
        }

        override fun canPoll(): Boolean {
            if (commited) throw IllegalStateException("Operation on TokenQueue Split after committing")
            return index < maxIndex && super.canPoll()
        }

        override fun peek(): Token<*> {
            if (commited) throw IllegalStateException("Operation on TokenQueue Split after committing")
            if (index >= maxIndex) throw IllegalStateException("Out-of-bounds operation on TokenQueue Split")
            return super.peek()
        }

        override fun poll(): Token<*> {
            if (commited) throw IllegalStateException("Operation on TokenQueue Split after committing")
            if (index >= maxIndex) throw IllegalStateException("Out-of-bounds operation on TokenQueue Split")
            index++
            return super.poll()
        }
    }

    fun isEOL(): Boolean {
        return canPoll() && peek().type.isSpecial()
    }

    /**
     * Also consumes special tokens like EOL and EOF, which are effectively whitespace
     */
    fun consumeWhitespace() {
        while (canPoll()) {
            if (!(tokens.peek().type.isWhitespace() || tokens.peek().type.isSpecial())) {
                return
            }
            poll()
        }
    }

    open fun canPoll(): Boolean {
        return tokens.isNotEmpty()
    }

    open fun tryPeek(): Token<*>? {
        return tokens.peek()
    }

    open fun peek(): Token<*> {
        return tokens.element()
    }

    open fun tryPoll(): Token<*>? {
        return tokens.poll()
    }

    open fun poll(): Token<*> {
        return tokens.remove()
    }

    override fun toString(): String {
        return tokens.toString()
    }

    override fun print(printer: Consumer<String>) {
        for (token in tokens) {
            token.print { s -> printer.accept("  $s") }
        }
    }
}