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

/**
 * Java-friendly API for reflectively providing results from a config using string scopes rather than direct access.
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/features/Result-Providers) for more details and examples.
 * @author fzzyhmstrs
 * @since 0.5.3
 */
interface ResultApiJava {

    /**
     * Creates a result provider that will return values of the given type from valid config scopes matching that type.
     *
     * This provider will inspect the registered config matching to the scope provided, utilizing the fallback instead if a valid scope isn't provided.
     *
     * Results are cached per scope provided. The cache is cleared automatically at the end of data pack reload.
     * @param T non-null provider type. Needs to be a type that can actually be provided by a config
     * @param fallback [Supplier]&lt;[T]&gt; - supplies a fallback value if the scope provided isn't valid. This fallback will be cached against any invalid scope, and an error logged, rather than an exception being thrown.
     * @param clazz [Class]&lt;[T]&gt; - class type to provide. Scopes provided can point to either a direct instance of the type, or `validatedField<T>`. So Boolean and ValidatedBoolean are both valid targets for a Boolean provider.
     * @return [ResultProvider] to supply results based on passed scopes. This should be created once per use-case (static, companion object, etc), otherwise caching won't be effective.
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    fun <T: Any> createResultProvider(fallback: Supplier<T>, clazz: Class<T>): ResultProvider<T>

    /**
     * Creates a result provider that will return values of the given type from valid config scopes matching that type, with a custom Supplier generator instance.
     *
     * This provider will inspect the registered config matching to the scope provided, utilizing the fallback instead if a valid scope isn't provided.
     *
     * Results are cached per scope provided. The cache is cleared automatically at the end of data pack reload.
     * @param T non-null provider type. Needs to be a type that can actually be provided by a config
     * @param fallback [Supplier]&lt;[T]&gt; - supplies a fallback value if the scope provided isn't valid. This fallback will be cached against any invalid scope, and an error logged, rather than an exception being thrown.
     * @param drillFunction [ResultProviderSupplierJava] - a custom creator of Suppliers for creating scoped results.
     * @return [ResultProvider] to supply results based on passed scopes. This should be created once per use-case (static, companion object, etc), otherwise caching won't be effective.
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    fun <T: Any> createResultProvider(fallback: Supplier<T>, drillFunction: ResultProviderSupplierJava<T>): ResultProvider<T>

    /**
     * Creates a result provider that will return values of the given type from valid config scopes matching that type.
     *
     * This provider will inspect the registered config matching to the scope provided, utilizing the fallback instead if a valid scope isn't provided.
     *
     * Results are cached per scope provided. The cache is cleared automatically at the end of data pack reload.
     * @param T non-null provider type. Needs to be a type that can actually be provided by a config
     * @param fallback [T] - a fallback value if the scope provided isn't valid. This fallback will be cached against any invalid scope, and an error logged, rather than an exception being thrown.
     * @param clazz [Class]&lt;[T]&gt; - class type to provide. Scopes provided can point to either a direct instance of the type, or `validatedField<T>`. So Boolean and ValidatedBoolean are both valid targets for a Boolean provider.
     * @return [ResultProvider] to supply results based on passed scopes. This should be created once per use-case (static, companion object, etc), otherwise caching won't be effective.
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    fun <T: Any> createSimpleResultProvider(fallback: T, clazz: Class<T>): ResultProvider<T>

    /**
     * Creates a result provider that will return values of the given type from valid config scopes matching that type, with a custom Supplier generator instance.
     *
     * This provider will inspect the registered config matching to the scope provided, utilizing the fallback instead if a valid scope isn't provided.
     *
     * Results are cached per scope provided. The cache is cleared automatically at the end of data pack reload.
     * @param T non-null provider type. Needs to be a type that can actually be provided by a config
     * @param fallback [T] - supplies a fallback value if the scope provided isn't valid. This fallback will be cached against any invalid scope, and an error logged, rather than an exception being thrown.
     * @param drillFunction [ResultProviderSupplierJava] - a custom creator of Suppliers for creating scoped results.
     * @return [ResultProvider] to supply results based on passed scopes. This should be created once per use-case (static, companion object, etc), otherwise caching won't be effective.
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    fun <T: Any> createSimpleResultProvider(fallback: T, drillFunction: ResultProviderSupplierJava<T>): ResultProvider<T>

}