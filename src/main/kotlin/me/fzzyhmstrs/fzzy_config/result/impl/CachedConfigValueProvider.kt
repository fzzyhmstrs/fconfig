/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.result.impl

import me.fzzyhmstrs.fzzy_config.result.ResultProvider
import java.lang.ref.SoftReference
import java.util.function.Function
import java.util.function.Supplier

internal class CachedConfigValueProvider<T: Any>(private val delegate: Function<String, Supplier<T>>): ResultProvider<T> {

    init {
        ResultApiImpl.resultProviders.add(SoftReference(this))
    }

    private var cachedResults: MutableMap<String, Supplier<T>> = mutableMapOf()

    internal fun invalidateResults() {
        cachedResults = mutableMapOf()
    }

    override fun getResult(scope: String): T {
        return cachedResults.computeIfAbsent(scope) { s -> delegate.apply(s) }.get()
    }
}