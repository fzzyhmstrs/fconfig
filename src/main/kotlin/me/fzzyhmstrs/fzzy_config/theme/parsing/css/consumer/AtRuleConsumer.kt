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
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import java.util.*

object AtRuleConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        if (!queue.canPoll()) return ValidationResult.error(ListOfRulesConsumer.unknownRule(0, 0), INVALID_AT, "Exhausted queue")
        val at = queue.poll()
        val identifier = at.value(CssType.AT) ?: return ValidationResult.error(ListOfRulesConsumer.unknownRule(at.line(), at.column()), INVALID_AT, "Opening token wasn't an @ token")
        val prelude: LinkedList<Token<*>> = LinkedList()
        val errors = ValidationResult.createMutable()

        fun complete(value: StyleBlockConsumer.StyleBlock? = null): ValidationResult<Token<*>> {
            return ValidationResult.ofMutable(Token(
                CssType.AT_RULE,
                AtRule(identifier, TokenQueue.ofFiltered(prelude), Optional.ofNullable(value)),
                at.line(),
                at.column()), errors)
        }

        while (queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                Parser.EOF -> {
                    queue.poll()
                    return ValidationResult.error(ListOfRulesConsumer.unknownRule(at.line(), at.column()), Errors.END_OF_FILE) { b -> b.message("At-rule").content(identifier) }
                }
                CssType.SEMI_COLON -> {
                    queue.poll()
                    return complete()
                }
                CssType.OPEN_BRACE -> {
                    val sb = SimpleBlockConsumer(CssType.CLOSE_BRACE).consume(queue, args).attachTo(errors).get()
                    val value = StyleBlockConsumer.consume(sb.valueStrict(CssType.SIMPLE_BLOCK).values, args).attachTo(errors).get()
                    return complete(value)
                }
                else -> {
                    if (peek.type == CssType.SIMPLE_BLOCK) {
                        if ((peek.value as SimpleBlockConsumer.Block).type == CssType.OPEN_BRACE) {
                            val sb = queue.poll()
                            val q = sb.valueStrict(CssType.SIMPLE_BLOCK as TokenType<SimpleBlockConsumer.Block>)
                            val value = StyleBlockConsumer.consume(q.values, args).attachTo(errors).get()
                            return complete(value)
                        }
                    }
                    prelude.add(ComponentValueConsumer.consume(queue, args).attachTo(errors).get())
                }
            }
        }
        return ValidationResult.error(ListOfRulesConsumer.unknownRule(at.line(), at.column()), UNCLOSED_AT, identifier)
    }

    private val UNCLOSED_AT = ValidationResult.ErrorEntry.Type<String>("Unclosed at-rule")
    private val INVALID_AT = ValidationResult.ErrorEntry.Type<String>("Unreadable at-rule")

    data class AtRule(val identifier: String, val prelude: TokenQueue, val value: Optional<StyleBlockConsumer.StyleBlock>)
}