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
import me.fzzyhmstrs.fzzy_config.result.ResultProviderSupplierJava
import me.fzzyhmstrs.fzzy_config.result.api.ResultApiJava
import me.fzzyhmstrs.fzzy_config.result.impl.ResultApiImpl.computeDefaultResultSupplier
import me.fzzyhmstrs.fzzy_config.result.impl.ResultApiImpl.computeResultSupplier
import java.util.function.Supplier
import kotlin.reflect.jvm.javaField

object ResultApiJavaImpl: ResultApiJava {

    override fun <T : Any> createResultProvider(fallback: Supplier<T>, clazz: Class<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope -> computeDefaultResultSupplier(scope, fallback, clazz.kotlin) }
    }

    override fun <T : Any> createResultProvider(fallback: Supplier<T>, drillFunction: ResultProviderSupplierJava<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope ->
            computeResultSupplier(scope, fallback) { s, c, t, tp -> drillFunction.supplierJava(s, c, t, tp.javaField) }
        }
    }

    override fun <T : Any> createSimpleResultProvider(fallback: T, clazz: Class<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope -> computeDefaultResultSupplier(scope, { fallback }, clazz.kotlin) }
    }

    override fun <T : Any> createSimpleResultProvider(fallback: T, drillFunction: ResultProviderSupplierJava<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope ->
            computeResultSupplier(scope, { fallback }) { s, c, t, tp -> drillFunction.supplierJava(s, c, t, tp.javaField) }
        }
    }
}