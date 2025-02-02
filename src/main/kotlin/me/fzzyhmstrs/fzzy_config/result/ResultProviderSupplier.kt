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

/**
 * Builds a [Supplier] of values from a config
 * @param T non-null type to build a supplier of
 * @author fzzyhmstrs
 * @since 0.5.3
 */
@FunctionalInterface
fun interface ResultProviderSupplier<T: Any> {
    /**
     * Creates a result supplier from config values. Since configs are by design mutable in-game, the result should freshly inspect the value on every `get()`.
     * - If the `thing` is a `ValidatedField` or otherwise a supplier of values, it can be used directly to provide a supplier of values
     * - If the `thing` is a basic primitive or object, the `property` should be used to supply values from the properties getter
     *
     * Using this general strategy ensures that fresh values are supplied on every call.
     * @param scope String representation of a config setting. See [the translation wiki page](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/Translation) for an overview of how scopes work.
     * @param config [Config] the config instance providing results
     * @param thing [Any] the candidate result to potentially use to create the supplier with.
     * @param property [KMutableProperty] reflection property instance for the [thing]
     * @return [Supplier]&lt;[T]&gt; supplier instance for retrieving values from the config scope provided.
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    fun supplier(scope: String, config: Config, thing: Any, property: KMutableProperty<*>): Supplier<T>
}