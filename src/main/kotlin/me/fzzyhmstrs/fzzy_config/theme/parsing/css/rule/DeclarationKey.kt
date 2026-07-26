/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.util.Optional

interface DeclarationKey<O: Any> {
    fun createDecl(decl: String, queue: TokenQueue, previous: Declaration<O>?): ValidationResult<Optional<Declaration<O>>>
    fun createDeclSafe(decl: String, queue: TokenQueue, previous: Declaration<*>?): ValidationResult<Optional<Declaration<O>>> {
        return createDecl(decl, queue, previous as? Declaration<O>)
    }
    fun defaultValue(): O

    companion object {
        private val declarationKeys: MutableMap<String, DeclarationKey<*>> = mutableMapOf()
        private val aliases: MutableMap<String, String> = mutableMapOf()

        fun register(declaration: String, key: DeclarationKey<*>, vararg alias: String) {
            val k = declarationKeys.put(declaration, key)
            if (k != null) {
                FC.LOGGER.error("RuleKey for $declaration already registered")
            } else {
                aliases[declaration] = declaration
                for (a in alias) {
                    aliases[a] = declaration
                }
            }
        }

        fun parseDecl(declaration: String, values: TokenQueue, soFar: Map<DeclarationKey<*>, Pair<Boolean, Declaration<*>>>): ValidationResult<Optional<Pair<DeclarationKey<*>, Declaration<*>>>> {
            val realDecl = aliases[declaration] ?: return ValidationResult.error(Optional.empty(), "Unknown rule declaration $declaration")
            val key = declarationKeys[realDecl] ?: return ValidationResult.error(Optional.empty(), "Unknown rule declaration $declaration")
            val previous = soFar[key]?.second
            val ruleResult = key.createDeclSafe(declaration, values, previous)
            if (ruleResult.isError()) {
                return ValidationResult.error(Optional.empty(), "Invalid rule declaration: ${ruleResult.getError()}")
            }
            return ruleResult.map { o -> o.map { r -> key to r } }
        }


    }
}