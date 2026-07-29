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

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.Creator
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.Creator.Companion.applyValue
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.SequencedValueBuilder
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilders
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.Declaration
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.minecraft.text.Style
import net.minecraft.text.StyleSpriteSource
import net.minecraft.world.attribute.EnvironmentAttributeModifier.override
import java.util.Optional

class FontDeclaration(private val style: Style): Declaration<Style> {
    override fun ruleValue(): Style {
        return style
    }

    override fun layer(lower: Declaration<Style>): Declaration<Style> {
        lower as FontDeclaration
        return FontDeclaration(this.style.withParent(lower.style))
    }
}

class FontCreator(private var style: Style = Style.EMPTY): Creator<FontDeclaration> {

    fun apply(applier: (Style) -> Style) {
        this.style = applier(this.style)
    }

    override fun create(): FontDeclaration {
        return FontDeclaration(style)
    }
}

object FontDeclarationKey: DeclarationKey<Style, FontCreator> {

    override fun createDecl(
        decl: String,
        queue: TokenQueue,
        builder: FontCreator
    ): ValidationResult<Optional<FontCreator>> {
        val fontFamilyBuilder = ValueBuilders.oneOfValueBuilder("minecraft:default".fcId(),
            ValueBuilders.autoValueBuilder("font-family") { "minecraft:default".fcId() },
            ValueBuilders.keyedIdentValueBuilder("minecraft:default".fcId(), "default"),
            ValueBuilders.keyedIdentValueBuilder("minecraft:uniform".fcId(), "uniform"),
            ValueBuilders.keyedIdentValueBuilder("minecraft:alt".fcId(), "alt"),
            ValueBuilders.keyedIdentValueBuilder("minecraft:illageralt".fcId(), "illager"),
            ValueBuilders.identifierValueBuilder())

        return when (decl) {
            "font" -> {
                SequencedValueBuilder.create<FontDeclaration, FontCreator>()
                    .sequence(fontFamilyBuilder) { t, c -> c.apply { style -> style.withFont(StyleSpriteSource.Font(t)) } }
                    .sequence(ValueBuilders.colorValueBuilder()) { t, c -> c.apply { style -> style.withColor(t) }}
                    .sequence(ValueBuilders.colorValueBuilder()) { t, c -> c.apply { style -> style.withShadowColor(t) }}
                    .sequence(ValueBuilders.twoValueValueBuilder("bold", "normal")) { t, c -> c.apply { style -> style.withBold(t) }}
                    .sequence(ValueBuilders.twoValueValueBuilder("italic", "normal")) { t, c -> c.apply { style -> style.withItalic(t) }}
                    .sequence(ValueBuilders.twoValueValueBuilder("underline", "normal")) { t, c -> c.apply { style -> style.withUnderline(t) }}
                    .sequence(ValueBuilders.twoValueValueBuilder("strikethrough", "normal")) { t, c -> c.apply { style -> style.withStrikethrough(t) }}
                    .sequence(ValueBuilders.twoValueValueBuilder("obfuscated", "normal")) { t, c -> c.apply { style -> style.withObfuscated(t) }}
                    .build().applyValue(queue, builder)
            }
            "font-family" -> {
                builder.applyValue(queue, fontFamilyBuilder) { t, c -> c.apply { style -> style.withFont(StyleSpriteSource.Font(t)) } }
            }
            "color" -> {
                builder.applyValue(queue, ValueBuilders.colorValueBuilder()) { t, c -> c.apply { style -> style.withColor(t) }}
            }
            "color-shadow" -> {
                builder.applyValue(queue, ValueBuilders.colorValueBuilder()) { t, c -> c.apply { style -> style.withShadowColor(t) }}
            }
            "font-bold", "font-weight" -> {
                builder.applyValue(queue, ValueBuilders.twoValueValueBuilder("bold", "normal")) { t, c -> c.apply { style -> style.withBold(t) }}
            }
            "font-italic", "font-style" -> {
                builder.applyValue(queue, ValueBuilders.twoValueValueBuilder("italic", "normal")) { t, c -> c.apply { style -> style.withItalic(t) }}
            }
            "font-underline" -> {
                builder.applyValue(queue, ValueBuilders.twoValueValueBuilder("underline", "normal")) { t, c -> c.apply { style -> style.withUnderline(t) }}
            }
            "font-strikethrough" -> {
                builder.applyValue(queue, ValueBuilders.twoValueValueBuilder("strikethrough", "normal")) { t, c -> c.apply { style -> style.withStrikethrough(t) }}
            }
            "font-obfuscated" -> {
                builder.applyValue(queue, ValueBuilders.twoValueValueBuilder("obfuscated", "normal")) { t, c -> c.apply { style -> style.withObfuscated(t) }}
            }
            else -> {
                ValidationResult.error(Optional.empty(), Errors.INVALID_DECL) { b -> b.message("Font").content(decl) }
            }
        }
    }

    override fun autoValue(): Style {
        return Style.EMPTY
    }

    override fun defaultValue(): Style {
        return Style.EMPTY
    }

    override fun builder(): FontCreator {
        return FontCreator()
    }

    val ALIASES = arrayOf("font-family", 
                          "color", 
                          "color-shadow", 
                          "font-bold", 
                          "font-weight", 
                          "font-italic", 
                          "font-style", 
                          "font-underline", 
                          "font-strikethrough", 
                          "font-obfuscated")
}
