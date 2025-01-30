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
import java.util.function.BiFunction

/**
 * Handles creating [DynamicListWidget.Entry], separate and distinct from a config-level Entry
 * @author fzzyhmstrs
 * @since 0.6.0
 */
@Experimental
@JvmDefaultWithoutCompatibility
fun interface EntryCreator {

    /**
     * Builds one or more [DynamicListWidget.Entry] from provided [CreatorContext]. These entries are used to build config GUIs, being the "building block" of the settings list.
     *
     * Unless you are making a fully new type of Entry (power to you!), usually you won't need to directly create a new Creator. Instead, you will probably deal with help methods in existing classes.
     * - [ValidatedField.entryDeco][me.fzzyhmstrs.fzzy_config.validation.ValidatedField.entryDeco]
     * - [ValidatedField.contentBuilder][me.fzzyhmstrs.fzzy_config.validation.ValidatedField.contentBuilder]
     * - [ValidatedField.contextActionBuilder][me.fzzyhmstrs.fzzy_config.validation.ValidatedField.contextActionBuilder]
     * @param context [CreatorContext]
     * @return List&lt;[Creator]&gt;
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun createEntry(context: CreatorContext): List<Creator>

    /**
     * Called in the prepare stage of screen building to perform any necessary pre-entry-creation tasks.
     * @param scope the settings scope
     * @param groups [LinkedList]&lt;String&gt; the current group stack
     * @param annotations List&lt;Annotation&gt; the annotations attached directly to this setting
     * @param globalAnnotations List&lt;Annotation&gt; the annotations attached to the entire config or relevant parent (section etc.)
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun prepare(scope: String, groups: LinkedList<String>, annotations: List<Annotation>, globalAnnotations: List<Annotation>) {}

    /**
     * Context provided by the screen manager for building a new [Creator]
     * @param scope String scope of the entry
     * @param groups [LinkedList]&lt;String&gt; the current group stack
     * @param client True if this setting is relevant to the client only (client-sided config etc.).
     * @param texts [Translatable.Result] translation result for the entry
     * @param annotations List&lt;Annotation&gt; annotations attached directly to this entry
     * @param actions Set&lt;[Action]&gt; the action(s) attached to this entry
     * @param misc [CreatorContextMisc] miscellaneous general information from the screen manager. This will be information that is common to the entire manager, such as a copy buffer reference.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    class CreatorContext(val scope: String,
                         val groups: LinkedList<String>,
                         val client: Boolean,
                         val texts: Translatable.Result,
                         val annotations: List<Annotation>,
                         val actions: Set<Action>,
                         val misc: CreatorContextMisc)

    class CreatorContextMisc internal constructor() {

        private val map: MutableMap<CreatorContextKey<*>, Any> = mutableMapOf()

        /**
         * Retrieves the information stored with the provided [CreatorContextKey], or null if none is present.
         * @param T data type of the key/value pair being retrieved
         * @param key [CreatorContextKey]
         * @return [T] instance stored in this map, if any, or null
         * @author fzzyhmstrs
         * @since
         */
        fun <T> get(key: CreatorContextKey<T>): T? {
            return map[key] as? T
        }
        internal fun <T: Any> put(key: CreatorContextKey<T>, value: T): CreatorContextMisc {
            map[key] = value
            return this
        }
    }

    /**
     * Key for data stored in [CreatorContextMisc]
     * @param T data type stored with this key
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open class CreatorContextKey<T>

    /**
     * Wrapper of information needed to create a new GUI entry.
     * @param scope The scope of the entry. Obtained from the [CreatorContext] during creation
     * @param texts [Translatable.Result]. Translation result for the entry. Obtained from the [CreatorContext] during creation
     * @param entry [BiFunction]&lt;[DynamicListWidget], Int, out [DynamicListWidget.Entry]&gt; the entry builder function. This should create new entries on every call.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    class Creator(val scope: String, val texts: Translatable.Result, val entry: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>)
}