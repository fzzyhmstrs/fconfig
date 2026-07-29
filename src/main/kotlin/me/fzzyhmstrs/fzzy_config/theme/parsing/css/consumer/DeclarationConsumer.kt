/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenConsumer.Companion.ERROR
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import java.util.*

object DeclarationConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        val identifier = queue.poll() //pull the ident back out of the sliced queue
        val componentValues: LinkedList<Token<*>> = LinkedList()
        val errors = ValidationResult.createMutable()
        queue.consumeWhitespace()
        if (!queue.canPoll()) return ValidationResult.error(ERROR, EMPTY_DECLARATION, identifier.asString())
        val colon = queue.peek()
        if (colon.type != CssType.COLON) return ValidationResult.error(ERROR, NO_COLON, identifier.asString())
        queue.poll()
        queue.consumeWhitespace()
        if (!queue.canPoll()) return ValidationResult.error(ERROR, EMPTY_DECLARATION, identifier.asString())
        while (queue.canPoll()) {
            componentValues.add(ComponentValueConsumer.consume(queue, args).attachTo(errors).get())
        }
        var important = false
        var bang = false
        val itr = componentValues.descendingIterator()
        while (itr.hasNext()) {
            val tkn = itr.next()
            if (tkn.type.isWhitespace()) continue
            if (!important) {
                if (tkn.type == CssType.IDENT && tkn.asString().lowercase() == "important") {
                    important = true
                } else {
                    break
                }
            } else {
                if (tkn.type == CssType.DELIM && tkn.asString() == "!") {
                    bang = true
                }
                break
            }
        }
        val isImportant = important && bang

        return ValidationResult.ofMutable(Token(CssType.DECLARATION, Declaration(identifier.asString(), TokenQueue.ofFiltered(componentValues), isImportant), identifier.line(), identifier.column()), errors)
    }

    private val EMPTY_DECLARATION = ValidationResult.ErrorEntry.Type<String>("Empty Declaration")
    private val NO_COLON = ValidationResult.ErrorEntry.Type<String>("No Declaration Colon")

    data class Declaration(val identifier: String, val values: TokenQueue, val important: Boolean)
}