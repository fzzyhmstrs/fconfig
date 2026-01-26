/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer.Companion.ERROR
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.SimpleBlockConsumer.Block
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.*

object DeclarationConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        val identifier = queue.poll() //pull the ident back out of the sliced queue
        val componentValues: LinkedList<Token<*>> = LinkedList()
        val errors: MutableList<String> = mutableListOf()
        queue.consumeWhitespace()
        if (!queue.canPoll()) return ValidationResult.error(ERROR, "Empty declaration for [$identifier]")
        val colon = queue.peek()
        if (colon.type != CssType.COLON) return ValidationResult.error(ERROR, "Declaration without : for [$identifier]")
        queue.poll()
        queue.consumeWhitespace()
        if (!queue.canPoll()) return ValidationResult.error(ERROR, "Empty declaration for [$identifier]")
        while (queue.canPoll()) {
            componentValues.add(ComponentValueConsumer.consume(queue, args).also { it.writeError(errors) }.get())
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

        return ValidationResult.predicated(Token(CssType.DECLARATION, Declaration(identifier.asString(), TokenQueue.Impl(componentValues), isImportant), identifier.line(), identifier.column()), errors.isEmpty(), errors.toString())
    }

    data class Declaration(val identifier: String, val values: TokenQueue, val important: Boolean)
}