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

import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.util.Translatable
import org.jetbrains.annotations.ApiStatus.Experimental
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.Function

/**
 * Handles creating ConfigListWidget Entries (from a Config entry), not to be confusing
 * @author fzzyhmstrs
 * @since 0.6.0
 */
@Internal
@Experimental
@JvmDefaultWithoutCompatibility
fun interface EntryCreator {

    fun createEntry(context: CreatorContext): List<Creator>

    fun prepare(scope: String, groups: LinkedList<String>, annotations: List<Annotation>, globalAnnotations: List<Annotation>) {}

    class CreatorContext(val scope: String,
                         val groups: LinkedList<String>,
                         val client: Boolean,
                         val texts: Translatable.Result,
                         val annotations: List<Annotation>,
                         val actions: Set<Action>,
                         val misc: CreatorContextMisc)

    class CreatorContextMisc {

        private val map: MutableMap<CreatorContextKey<*>, Any> = mutableMapOf()

        fun <T> get(key: CreatorContextKey<T>): T? {
            return map[key] as? T
        }

        fun <T: Any> put(key: CreatorContextKey<T>, value: T): CreatorContextMisc {
            map[key] = value
            return this
        }
    }

    open class CreatorContextKey<T>

    class Creator(val scope: String, val texts: Translatable.Result, val entry: Function<DynamicListWidget, out DynamicListWidget.Entry>)
}