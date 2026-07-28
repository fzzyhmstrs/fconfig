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
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder.Creator
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.util.IdentityHashMap
import java.util.Optional

interface DeclarationKey<O: Any, C: Creator<out Declaration<O>>> {
    fun createDecl(decl: String, queue: TokenQueue, builder: C): ValidationResult<Optional<C>>
    fun createDeclSafe(decl: String, queue: TokenQueue, builder: Creator<out Declaration<*>>): ValidationResult<Optional<C>> {
        return createDecl(decl, queue, (builder as? C) ?: builder())
    }
    fun defaultValue(): O
    fun autoValue(): O? = null
    fun builder(): C

    companion object {
        private val declarationKeys: MutableMap<String, DeclarationKey<*, *>> = mutableMapOf()
        private val keysDeclaration: IdentityHashMap<DeclarationKey<*, *>, String> = IdentityHashMap()
        private val aliases: MutableMap<String, String> = mutableMapOf()

        fun register(declaration: String, key: DeclarationKey<*, *>, vararg alias: String) {
            val k = declarationKeys.put(declaration, key)
            keysDeclaration[key] = declaration
            if (k != null) {
                FC.LOGGER.error("RuleKey for $declaration already registered")
            } else {
                aliases[declaration] = declaration
                for (a in alias) {
                    aliases[a] = declaration
                }
            }
        }

        fun parseDecl(declaration: String, values: TokenQueue, soFar: Map<DeclarationKey<*, *>, Pair<Boolean, Creator<Declaration<*>>>>): ValidationResult<Optional<Pair<DeclarationKey<*, *>, Creator<Declaration<*>>>>> {
            val realDecl = aliases[declaration] ?: return ValidationResult.error(Optional.empty(), Errors.INVALID_DECL, declaration)
            val key = declarationKeys[realDecl] ?: return ValidationResult.error(Optional.empty(), Errors.INVALID_DECL, declaration)
            val previous = soFar[key]?.second ?: key.builder()
            val ruleResult = key.createDeclSafe(declaration, values, previous)
            if (ruleResult.isError()) {
                return ruleResult.map { Optional.empty() }
            }
            return ruleResult.map { o -> o.map { r -> key to r.cast() } }
        }

        fun declName(key: DeclarationKey<*, *>): String {
            return keysDeclaration[key] ?: "Unknown"
        }
    }
}