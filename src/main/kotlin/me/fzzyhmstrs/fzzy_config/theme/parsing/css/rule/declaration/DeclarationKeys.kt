/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.declaration

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilders
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.LengthValue
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.PaddingDeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.ScrollBarDeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.SimpleValueKey
import net.minecraft.util.Identifier

object DeclarationKeys {
    val SCROLL_BAR = ScrollBarDeclarationKey.register("scroll-bar", *ScrollBarDeclarationKey.ALIASES)
    val NARRATION_KEY = SimpleValueKey("narration-key", "fc.narrator.position.list", ValueBuilders.STRING).register("narration-key")
    val TRANSLATION_KEY = SimpleValueKey("translation-key", "default.translation.key", ValueBuilders.STRING).register("translation-key")
    val PADDING = PaddingDeclarationKey.register("padding", *PaddingDeclarationKey.ALIASES)
    val HEIGHT = SimpleValueKey(
        "height",
        LengthValue(100.0, LengthValue.Unit.PERCENTAGE),
        ValueBuilders.dimensionValueBuilder(::height)).register("height")
    val WIDTH = SimpleValueKey(
        "width",
        LengthValue(100.0, LengthValue.Unit.PERCENTAGE),
        ValueBuilders.dimensionValueBuilder(::width)).register("width")
    val FONT_FAMILY = SimpleValueKey("font-family",
        "minecraft:default".fcId(),
        ValueBuilders.oneOfValueBuilder("minecraft:default".fcId(),
            ValueBuilders.autoValueBuilder(::fontFamily),
            ValueBuilders.keyedIdentValueBuilder("minecraft:default".fcId(), "default"),
            ValueBuilders.keyedIdentValueBuilder("minecraft:uniform".fcId(), "uniform"),
            ValueBuilders.keyedIdentValueBuilder("minecraft:alt".fcId(), "alt"),
            ValueBuilders.keyedIdentValueBuilder("minecraft:illageralt".fcId(), "illager"),
            ValueBuilders.identifierValueBuilder())).register("font-family")


    private fun height(): DeclarationKey<LengthValue, *> {
        return HEIGHT
    }

    private fun width(): DeclarationKey<LengthValue, *> {
        return WIDTH
    }

    private fun fontFamily(): DeclarationKey<Identifier, *> {
        return FONT_FAMILY
    }

    fun <F: DeclarationKey<*, *>> F.register(declaration: String, vararg alias: String): F {
        DeclarationKey.register(declaration, this, *alias)
        return this
    }
}