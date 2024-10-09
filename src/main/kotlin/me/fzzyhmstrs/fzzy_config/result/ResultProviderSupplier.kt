/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.result

import me.fzzyhmstrs.fzzy_config.config.Config
import java.util.function.Supplier
import kotlin.reflect.KMutableProperty

@FunctionalInterface
fun interface ResultProviderSupplier<T: Any> {
    fun supplier(scope: String, config: Config, thing: Any, property: KMutableProperty<*>): Supplier<T>
}