/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.grammar

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Attr
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Selector
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.SelectorContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.Optional


object AttrSelectorGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume an attr selector from an empty queue")
        return queue.attempt { split ->
            val token = split.poll()
            if (token.type != CssType.OPEN_BRACKET) {
                return@attempt ValidationResult.error(Optional.empty(), "not an attr selector")
            }
            if (!split.canPoll()) return@attempt ValidationResult.error(Optional.empty(), "Can't consume an attr selector from an empty queue")
            WqNameGrammar { s, innerSplit ->
                Attr.getAttr(s)?.let {
                    if (!innerSplit.canPoll()) return@let null
                    val t = innerSplit.poll()
                    if (t.type == CssType.CLOSE_BRACKET) {
                        AttrName(it, s)
                    } else {
                        when (t.type) {
                            CssType.DELIM -> {
                                handleAttrMatching(it, s, t, innerSplit)
                            } else -> {
                                null
                            }
                        }
                    }
                }
            }.consume(split, args)
        }
    }

    private fun handleAttrMatching(attr: Attr, name: String, token: Token<*>, split: TokenQueue): Selector? {

        fun getAttrMatchValueAndMod(split: TokenQueue): Pair<String, String?>? {
            val t = split.tryPoll() ?: return null
            val v = when (t.type) {
                CssType.IDENT, CssType.STRING -> {
                    t.value as String
                }
                else -> return null
            }
            split.consumeWhitespace()
            val t2 = split.tryPeek() ?: return null
            return if (t2.value == "i" || t2.value == "I" || t2.value == "s" || t2.value == "S") {
                split.poll()
                v to (t2.value as String)
            } else {
                v to null
            }
        }

        fun handleRemainingAttrMatch(split: TokenQueue, selectorMaker: (String, String?) -> Selector?): Selector? {
            val t = split.tryPoll() ?: return null
            if (t.value != "=") return null
            val vmod = getAttrMatchValueAndMod(split) ?: return null
            val t2 = split.tryPoll() ?: return null
            if (t2.type != CssType.CLOSE_BRACKET) return null
            return selectorMaker(vmod.first, vmod.second)
        }

        return when (token.value) {
            "=" -> {
                val vmod = getAttrMatchValueAndMod(split) ?: return null
                AttrValue(attr, name, vmod.first, vmod.second)
            }
            "~" -> {
                handleRemainingAttrMatch(split) { s, mod -> AttrListContains(attr, name, s, mod) }
            }
            "|" -> {
                handleRemainingAttrMatch(split) { s, mod -> AttrValueDash(attr, name, s, mod) }
            }
            "^" -> {
                handleRemainingAttrMatch(split) { s, mod -> AttrValueStarts(attr, name, s, mod) }
            }
            "$" -> {
                handleRemainingAttrMatch(split) { s, mod -> AttrValueEnds(attr, name, s, mod) }
            }
            "*" -> {
                handleRemainingAttrMatch(split) { s, mod -> AttrValueContains(attr, name, s, mod) }
            }
            else -> {
                null
            }
        }
    }

    class AttrName(private val attr: Attr, private val name: String): Selector {

        override fun matches(context: SelectorContext): Boolean {
            return context.attrGetter.getAttrValue(attr) != null
        }

        override fun selector(): String {
            return "[$name]"
        }
    }

    sealed class AttrWithMatch(protected val attr: Attr, protected val name: String, value: String, mod: String?): Selector {

        protected val value: String
        protected val transformer: ((String) -> String)

        init {
            val bl = mod == "i" || mod == "I" || !(attr.caseSensitive || mod == "s" || mod == "S")
            this.value = if (bl) value.lowercase() else value
            this.transformer = if(bl) { v -> v.lowercase() } else { v -> v }
        }
    }

    class AttrValue(attr: Attr, name: String, value: String, mod: String?): AttrWithMatch(attr, name, value, mod) {

        override fun matches(context: SelectorContext): Boolean {
            val v = context.attrGetter.getAttrValue(attr) ?: return false
            val v2 = transformer(v)
            return value == v2
        }

        override fun selector(): String {
            return "[$name=\"$value\"]"
        }
    }

    class AttrListContains(attr: Attr, name: String, value: String, mod: String?): AttrWithMatch(attr, name, value, mod) {

        override fun matches(context: SelectorContext): Boolean {
            val v = context.attrGetter.getAttrValue(attr) ?: return false
            val v2 = transformer(v)
            return v2.split(" ").count { s -> s == value } == 1
        }

        override fun selector(): String {
            return "[$name~=\"$value\"]"
        }
    }

    class AttrValueDash(attr: Attr, name: String, value: String, mod: String?): AttrWithMatch(attr, name, value, mod) {

        override fun matches(context: SelectorContext): Boolean {
            val v = context.attrGetter.getAttrValue(attr) ?: return false
            val v2 = transformer(v)
            return value == v2 ||  v2.startsWith("$value-")
        }

        override fun selector(): String {
            return "[$name]"
        }
    }

    class AttrValueStarts(attr: Attr, name: String, value: String, mod: String?): AttrWithMatch(attr, name, value, mod) {

        override fun matches(context: SelectorContext): Boolean {
            val v = context.attrGetter.getAttrValue(attr) ?: return false
            val v2 = transformer(v)
            return v2.startsWith(value)
        }

        override fun selector(): String {
            return "[$name^=\"$value\"]"
        }
    }

    class AttrValueEnds(attr: Attr, name: String, value: String, mod: String?): AttrWithMatch(attr, name, value, mod) {

        override fun matches(context: SelectorContext): Boolean {
            val v = context.attrGetter.getAttrValue(attr) ?: return false
            val v2 = transformer(v)
            return v2.endsWith(value)
        }

        override fun selector(): String {
            return "[$name$=\"$value\"]"
        }
    }

    class AttrValueContains(attr: Attr, name: String, value: String, mod: String?): AttrWithMatch(attr, name, value, mod) {

        override fun matches(context: SelectorContext): Boolean {
            val v = context.attrGetter.getAttrValue(attr) ?: return false
            val v2 = transformer(v)
            return v2.contains(value)
        }

        override fun selector(): String {
            return "[$name*=\"$value\"]"
        }
    }

}