/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.value

import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.Creator
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.Creator.Companion.applyValue
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilder
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.Declaration
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.*

class SimpleValueKey<O: Any>(private val decl: String, private val default: O, private val valueBuilder: ValueBuilder<O>, private val auto: O? = default): DeclarationKey<O, SimpleValueCreator<O>> {

    override fun createDecl(
        decl: String,
        queue: TokenQueue,
        builder: SimpleValueCreator<O>
    ): ValidationResult<Optional<SimpleValueCreator<O>>> {
        if (decl != this.decl) {
            return ValidationResult.error(Optional.empty(), Errors.INVALID_DECL) { b -> b.content("Expected: ${this.decl}, got: $decl") }
        }
        return builder.applyValue(queue, valueBuilder) { t, c -> c.value = t }
    }

    override fun autoValue(): O? {
        return auto
    }

    override fun defaultValue(): O {
        return default
    }

    override fun builder(): SimpleValueCreator<O> {
        return SimpleValueCreator(default)
    }
}

class SimpleValueDeclaration<O: Any>(private val value: O, private val valueDefault: Boolean): Declaration<O> {

    override fun ruleValue(): O {
        return value
    }

    override fun layer(lower: Declaration<O>): Declaration<O> {
        lower as SimpleValueDeclaration<O>
        val value = if (valueDefault) lower.value else value
        return SimpleValueDeclaration(value, valueDefault && lower.valueDefault)
    }
}

class SimpleValueCreator<O: Any>(default: O): Creator<SimpleValueDeclaration<O>> {

    var value: O = default
        set(value) {
            valueDefault = false
            field = value
        }
    private var valueDefault: Boolean = true

    override fun create(): SimpleValueDeclaration<O> {
        return SimpleValueDeclaration(value, valueDefault)
    }

}