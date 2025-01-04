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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl.IGNORE_VISIBILITY
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl.drill
import me.fzzyhmstrs.fzzy_config.result.ResultArg
import me.fzzyhmstrs.fzzy_config.result.ResultProvider
import me.fzzyhmstrs.fzzy_config.result.ResultProviderSupplier
import me.fzzyhmstrs.fzzy_config.result.api.ResultApi
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import java.lang.ref.SoftReference
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmErasure

object ResultApiImpl: ResultApi {

    internal val resultProviders: MutableSet<SoftReference<CachedConfigValueProvider<*>>> = mutableSetOf()

    internal fun invalidateProviderCaches() {
        for (provider in resultProviders) {
            provider.get()?.invalidateResults()
        }
    }

    override fun <T : Any> createResultProvider(fallback: Supplier<T>, clazz: KClass<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope -> computeDefaultResultSupplier(scope, fallback, clazz) }
    }

    override fun <T : Any> createResultProvider(fallback: Supplier<T>, drillFunction: ResultProviderSupplier<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope ->
            computeResultSupplier(scope, fallback, drillFunction)
        }
    }

    override fun <T : Any> createSimpleResultProvider(fallback: T, clazz: KClass<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope -> computeDefaultResultSupplier(scope, { fallback }, clazz) }
    }

    override fun <T : Any> createSimpleResultProvider(fallback: T, drillFunction: ResultProviderSupplier<T>): ResultProvider<T> {
        return CachedConfigValueProvider { scope ->
            computeResultSupplier(scope, { fallback }, drillFunction)
        }
    }

    internal fun <T: Any> computeDefaultResultSupplier(
        scope: String,
        fallback: Supplier<T>,
        clazz: KClass<T>,
        drillFunction: ResultProviderSupplier<T> = ResultProviderSupplier { s, config, thing, thingProp ->
            if (thing is ValidatedField<*> && thing.argumentType()?.jvmErasure?.isSuperclassOf(clazz) == true) {
                Supplier { @Suppress("UNCHECKED_CAST") (thing.get() as T) }
            }
            else if (clazz.isInstance(thing)) {

                Supplier { @Suppress("UNCHECKED_CAST") (thingProp.call(config) as T) }
            }
            else {
                FC.LOGGER.error("Error encountered while reading value for $s. Value is not a number! Default value $fallback used.")
                fallback
            }
        }
    ): Supplier<T> {
        return computeResultSupplier(scope, fallback, drillFunction)
    }

    internal fun <T: Any> computeResultSupplier(
        scope: String,
        fallback: Supplier<T>,
        drillFunction: ResultProviderSupplier<T>
    ): Supplier<T> {
        try {
            var startIndex = 0
            while (startIndex < scope.length) {
                val nextStartIndex = scope.indexOf(".", startIndex)
                if (nextStartIndex == -1) {
                    FC.LOGGER.error("Invalid scope $scope provided. Config not found! Default value $fallback used.")
                    return fallback
                }
                startIndex = nextStartIndex + 1
                val testScope = scope.substring(0, nextStartIndex)
                val config = ConfigApiImpl.getConfig(testScope) ?: continue
                if (testScope == scope) {
                    FC.LOGGER.error("Invalid scope $scope provided. No setting scope provided! Default value $fallback used.")
                    FC.LOGGER.error("Found: '$scope'")
                    FC.LOGGER.error("Need '$scope[.subScopes].settingName'")
                    return fallback
                }
                var supplier = fallback
                val target = ResultArg.stripArgs(scope.removePrefix("$testScope."))
                drill(config, target, '.', IGNORE_VISIBILITY)  { _, _, _, thing, thingProp, _, _, _ ->
                    if (thing == null) {
                        FC.LOGGER.error("Error encountered while reading value for $scope. Value was null! Default value $fallback used.")
                    } else {
                        supplier = drillFunction.supplier(ResultArg.stripArgs(scope), config, thing, thingProp)
                    }
                }
                return supplier
            }
            return fallback
        } catch (e: Throwable) {
            FC.LOGGER.error("Critical exception encountered while computing config result supplier for $scope. Default value $fallback used")
            return fallback
        }
    }

}