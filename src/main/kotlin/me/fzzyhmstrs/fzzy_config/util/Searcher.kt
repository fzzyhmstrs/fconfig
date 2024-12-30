/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util

import me.fzzyhmstrs.fzzy_config.util.Searcher.SearchContent
import net.minecraft.client.search.SuffixArray
import net.minecraft.text.Text
import java.util.*

/**
 * Searches provided inputs by name and/or description with optional search decorators
 * @param C subclass of [SearchContent]
 * @param searchEntries List&lt;[C]&gt; list of [SearchContent] entries to search through.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class Searcher<C: Searcher.SearchContent>(private val searchEntries: List<C>) {

    private val search: SuffixArray<C> by lazy {
        val array = SuffixArray<C>()
        for (entry in searchEntries.filter{ !it.skip }) {
            array.add(entry, entry.name.string.lowercase(Locale.ROOT))
        }
        array.build()
        array
    }

    private val searchExact: Map<String, C> by lazy {
        val map: MutableMap<String, C> = mutableMapOf()
        for (entry in searchEntries.filter { !it.skip }) {
            map[entry.name.string.lowercase(Locale.ROOT)] = entry
        }
        map
    }

    private val searchDesc: SuffixArray<C> by lazy {
        val array = SuffixArray<C>()
        for (entry in searchEntries.filter{ it.desc != null && !it.skip }) {
            array.add(entry, entry.desc?.string?.lowercase(Locale.ROOT))
        }
        array.build()
        array
    }

    /**
     * Searches input entries with the provided input string. This search is case-insensitive.
     *
     * There is no guarantee on the order of returned results, so it may be prudent to use it to modify the based displayed list rather than directly.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun search(searchInput: String): List<C> {
        val type: SearchType
        val trimmedSearchInput = if (searchInput.startsWith('$')) {
            type = SearchType.DESCRIPTION
            if (searchInput.length == 1) {
                ""
            } else {
                searchInput.substring(1)
            }
        } else if (searchInput.startsWith('-')) {
            if (searchInput.startsWith("-$")) {
                type = SearchType.NEGATE_DESCRIPTION
                if (searchInput.length == 2) {
                    ""
                } else {
                    searchInput.substring(2)
                }
            } else if (searchInput.startsWith("-\"")) {
                if (searchInput.length > 2 && searchInput.endsWith('"')) {
                    type = SearchType.NEGATE_EXACT
                    if (searchInput.length == 3) {
                        ""
                    } else {
                        searchInput.substring(2, searchInput.lastIndex)
                    }
                } else {
                    type = SearchType.NEGATION
                    if (searchInput.length == 2) {
                        ""
                    } else {
                        searchInput.substring(2)
                    }
                }
            } else {
                type = SearchType.NEGATION
                if (searchInput.length == 1) {
                    ""
                } else {
                    searchInput.substring(1)
                }
            }
        } else if (searchInput.startsWith('"')) {
            if (searchInput.length > 1 && searchInput.endsWith('"')) {
                type = SearchType.EXACT
                if (searchInput.length == 2) {
                    ""
                } else {
                    searchInput.substring(1, searchInput.lastIndex)
                }
            } else {
                type = SearchType.NORMAL
                if (searchInput.length == 1) {
                    ""
                } else {
                    searchInput.substring(1)
                }
            }
        } else {
            type = SearchType.NORMAL
            searchInput
        }

        if (trimmedSearchInput == "") {
            return searchEntries
        }

        val list: List<C>
        when (type) {
            SearchType.DESCRIPTION -> {
                val results = searchDesc.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = results
            }
            SearchType.NEGATION -> {
                val results = search.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = searchEntries.filter { e -> !results.contains(e) }
            }
            SearchType.NEGATE_DESCRIPTION -> {
                val results = searchDesc.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = searchEntries.filter { e -> !results.contains(e) }
            }
            SearchType.EXACT -> {
                val result = searchExact[trimmedSearchInput.lowercase(Locale.ROOT)]
                list = if(result != null) listOf(result) else emptyList()
            }
            SearchType.NEGATE_EXACT -> {
                val result = searchExact[trimmedSearchInput.lowercase(Locale.ROOT)]
                list = searchEntries.filter { e -> e != result }
            }
            SearchType.NORMAL -> {
                val results = search.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = results
            }
        }
        return list
    }

    //TODO
    interface SearchContent {
        val name: Text
        val desc: Text?
        val skip: Boolean
    }

    private enum class SearchType {
        DESCRIPTION,
        NEGATION,
        NEGATE_DESCRIPTION,
        EXACT,
        NEGATE_EXACT,
        NORMAL
    }
}