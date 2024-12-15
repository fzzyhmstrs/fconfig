/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.screen.widget.NewConfigListWidget
import me.fzzyhmstrs.fzzy_config.util.Translatable
import org.jetbrains.annotations.ApiStatus.Experimental
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Function

/**
 * Handles creating ConfigListWidget Entries (from a Config entry), not to be confusing
 * @author fzzyhmstrs
 * @since 0.6.0
 */
@Internal
@Experimental
fun interface EntryCreator {

    fun createEntry(client: Boolean, texts: Translatable.Result, annotations: List<Annotation>): List<Function<NewConfigListWidget, out NewConfigListWidget.Entry>>


    class Creator(val scope: String, entries: List<Function<NewConfigListWidget, out NewConfigListWidget.Entry>>)

}