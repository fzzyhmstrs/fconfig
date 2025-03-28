/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Searcher
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.Walkable
import java.util.function.Function

/**
 * Provides a function for searching sub-entries within this [Entry]
 *
 * SAM: [searchEntry] provides a search function that accepts a search string and returns search results
 * @author fzzyhmstrs
 * @since 0.6.8
 */
@FunctionalInterface
fun interface EntrySearcher {
    /**
     * Provides a search function, converting a search string into results
     * @param config The parent config object. If this is a child of a child, the config will be its parent's parent (always the main config object)
     * @param scope The scope of this entry
     * @param client Whether the config is client-only or not
     * @return [Function]&lt;String, [Translatable.Result]&gt; that converts search strings into a list of found results
     * @see SearchProvider
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    fun searchEntry(config: Any, scope: String, client: Boolean): Function<String, List<Translatable.Result>>

    /**
     * A built-in searcher function that builds a searcher by reflectively walking the provided content, mapping text [Translatable.Result] from each member within
     * @param config The parent config object. If this is a child of a child, the config will be its parent's parent (always the main config object)
     * @param content The entry to walk and search from. Typically, a Config, Section, or Walkable
     * @param prefix The scope of the content
     * @param client Whether the config is client-only or not
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    class SearchProvider(config: Any, content: Any, prefix: String, client: Boolean): Function<String, List<Translatable.Result>> {

        private val delegate: Function<String, List<Translatable.Result>> by lazy {
            val list: MutableList<Translatable.Result> = mutableListOf()
            val nestedSearchers: MutableList<Pair<Translatable.Result, Function<String, List<Translatable.Result>>>> = mutableListOf()
            ConfigApiImpl.walk(content, prefix, ConfigApiImpl.IGNORE_NON_SYNC) { _, _, new, thing, _, annotations, globalAnnotations, _ ->
                val flags = if(thing is EntryFlag) {
                    EntryFlag.Flag.entries.filter { thing.hasFlag(it) }
                } else {
                    EntryFlag.Flag.NONE
                }
                val permResult = ConfigApiImplClient.hasNeededPermLevel(
                    thing,
                    ConfigApiImplClient.getPlayerPermissionLevel(),
                    config,
                    config.nullCast<Config>()?.getId()?.toTranslationKey() ?: "",
                    new,
                    annotations,
                    client,
                    flags,
                    ConfigApiImplClient.getPerms())
                if (permResult.success && thing != null) {
                    val fieldName = new.substringAfterLast('.')
                    val texts = ConfigApiImplClient.getText(thing, new, fieldName, annotations, globalAnnotations)
                    if (thing is EntrySearcher) {
                        nestedSearchers.add(Pair(texts, thing.searchEntry(config, new, client)))
                    } else if (thing is Walkable) {
                        nestedSearchers.add(Pair(texts, SearchProvider(config, thing, prefix, client)))
                    }
                    list.add(texts)
                }
            }
            val searcher = Searcher(list)
            return@lazy Function { s ->
                val l = searcher.search(s).toMutableList()
                for (nested in nestedSearchers) {
                    l.addAll(nested.second.apply(s).map { Translatable.createResult(FcText.translatable("fc.search.child", nested.first.name, it.name)) })
                }
                return@Function l
            }
        }

        override fun apply(t: String): List<Translatable.Result> {
            return delegate.apply(t)
        }
    }
}