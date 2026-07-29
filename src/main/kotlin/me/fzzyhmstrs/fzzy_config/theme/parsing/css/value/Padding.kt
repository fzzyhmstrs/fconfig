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
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilders
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.Declaration
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.*

data class Padding(val top: Int = 0, val right: Int = 0, val bottom: Int = 0, val left: Int = 0)

class PaddingDeclaration(private val top: LengthValue,
                         private val topDefault: Boolean,
                         private val right: LengthValue,
                         private val rightDefault: Boolean,
                         private val bottom: LengthValue,
                         private val bottomDefault: Boolean,
                         private val left: LengthValue,
                         private val leftDefault: Boolean): Declaration<Padding> {

    override fun ruleValue(): Padding {
        return Padding(top.getValue(), right.getValue(), bottom.getValue(), left.getValue())
    }

    override fun layer(lower: Declaration<Padding>): Declaration<Padding> {
        lower as PaddingDeclaration
        val top = if (topDefault) lower.top else top
        val right = if (rightDefault) lower.right else right
        val bottom = if (bottomDefault) lower.bottom else bottom
        val left = if (leftDefault) lower.left else left
        return PaddingDeclaration(
            top, topDefault && lower.topDefault,
            right, rightDefault && lower.rightDefault,
            bottom, bottomDefault && lower.bottomDefault,
            left, leftDefault && lower.leftDefault)
    }
}

class PaddingDeclarationCreator: Creator<PaddingDeclaration> {
    var top: LengthValue = LengthValue(0)
        set(value) {
            topDefault = false
            field = value
        }
    private var topDefault: Boolean = true
    var right: LengthValue = LengthValue(0)
        set(value) {
            rightDefault = false
            field = value
        }
    private var rightDefault: Boolean = true
    var bottom: LengthValue = LengthValue(0)
        set(value) {
            bottomDefault = false
            field = value
        }
    private var bottomDefault: Boolean = true
    var left: LengthValue = LengthValue(0)
        set(value) {
            leftDefault = false
            field = value
        }
    private var leftDefault: Boolean = true

    override fun create(): PaddingDeclaration {
        return PaddingDeclaration(top, topDefault, right, rightDefault, bottom, bottomDefault, left, leftDefault)
    }
}

object PaddingDeclarationKey: DeclarationKey<Padding, PaddingDeclarationCreator> {

    override fun createDecl(
        decl: String,
        queue: TokenQueue,
        builder: PaddingDeclarationCreator
    ): ValidationResult<Optional<PaddingDeclarationCreator>> {
        return when (decl) {
            "padding" -> {
                when (queue.size()) {
                    1 -> {
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c ->
                            c.top = t
                            c.right = t
                            c.bottom = t
                            c.left = t
                        }
                    }
                    2 -> {
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c ->
                            c.top = t
                            c.bottom = t
                        }
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c ->
                            c.left = t
                            c.right = t
                        }
                    }
                    3 -> {
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.top = t }
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c ->
                            c.left = t
                            c.right = t
                        }
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.bottom = t }
                    }
                    4 -> {
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.top = t }
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.right = t }
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.bottom = t }
                        builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.left = t }

                    }
                    else -> {
                        ValidationResult.error(Optional.empty(), Errors.INVALID_DECL) { b -> b.message("Padding").content(decl) }
                    }
                }
            }
            "padding-top" -> {
                builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.top = t }
            }
            "padding-right" -> {
                builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.right = t }
            }
            "padding-bottom" -> {
                builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.bottom = t }
            }
            "padding-left" -> {
                builder.applyValue(queue, ValueBuilders.lengthValueBuilder(0)) { t, c -> c.left = t }
            }
            else -> {
                ValidationResult.error(Optional.empty(), Errors.INVALID_DECL) { b -> b.message("Padding").content(decl) }
            }
        }
    }

    override fun defaultValue(): Padding {
        return Padding()
    }

    override fun builder(): PaddingDeclarationCreator {
        return PaddingDeclarationCreator()
    }

    val ALIASES = arrayOf("padding-top", "padding-right", "padding-bottom", "padding-left")
}