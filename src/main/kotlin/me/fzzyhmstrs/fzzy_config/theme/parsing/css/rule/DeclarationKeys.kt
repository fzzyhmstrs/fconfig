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

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.ScrollBarDeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.SimpleValueKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder.SingleValueBuilders

object DeclarationKeys {
    val SCROLL_BAR = ScrollBarDeclarationKey
    val NARRATION_KEY = SimpleValueKey("narration-key", "fc.narrator.position.list", SingleValueBuilders.STRING)
    val TRANSLATION_KEY = SimpleValueKey("translation-key", "default.translation.key", SingleValueBuilders.STRING)
    val PADDING = Paddin
}