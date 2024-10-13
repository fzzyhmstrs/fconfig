/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.result.ResultArg

class IndexArg<T>(fallback: T): ResultArg<Collection<T>, T>("index", fallback) {

    override fun applyArg(scopeValue: Collection<T>, argValue: String): T {
        try {
            val index = argValue.toInt()
            return if (index >= 0 && scopeValue.size > index)
                scopeValue.toList()[index]
            else
                getFallback()

        } catch (e: Throwable) {
            return this.getFallback()
        }
    }

}