/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.result.api

import me.fzzyhmstrs.fzzy_config.result.ResultProvider
import me.fzzyhmstrs.fzzy_config.result.ResultProviderSupplierJava
import java.util.function.Supplier

interface ResultApiJava {

    fun <T: Any> createResultProvider(fallback: Supplier<T>, clazz: Class<T>): ResultProvider<T>

    fun <T: Any> createResultProvider(fallback: Supplier<T>, drillFunction: ResultProviderSupplierJava<T>): ResultProvider<T>

    fun <T: Any> createSimpleResultProvider(fallback: T, clazz: Class<T>): ResultProvider<T>

    fun <T: Any> createSimpleResultProvider(fallback: T, drillFunction: ResultProviderSupplierJava<T>): ResultProvider<T>

}