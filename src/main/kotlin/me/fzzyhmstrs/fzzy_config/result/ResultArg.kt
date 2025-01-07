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

import com.mojang.brigadier.StringReader
import org.jetbrains.annotations.ApiStatus
import java.util.function.Consumer

/**
 * A key for handling a scope argument for a [ResultProvider]
 * @param V the value type received by this arg for processing
 * @param R the result type provided by processing
 * @param arg String key used in the scope passed to the provider. for example a boolean arg "test" would have a scope `my.example.scope?test=false`
 * @param fallback [R] a fallback value in case processing or argument parsing fails
 * @param noValueArg Optional boolean, if true the scope string will not need an `=value` clause: `my.example.scope?noValue?valueNeeded=true`
 * @author fzzyhmstrs
 * @since 0.5.3
 */
abstract class ResultArg<V, R> @JvmOverloads constructor(internal val arg: String, internal val fallback: R, noValueArg: Boolean = false) {

    init {
        if (noValueArg)
            noValueArgs.add(arg)
    }

    protected fun getFallback(): R {
        return fallback
    }

    /**
     * Applies the parsed arg value to a scoped config value and returns the processing result
     * @param scopeValue [V] - the valid config result acquired from [ResultProvider.getResult]
     * @param argValue String - parsed arg value for further parsing and application by this key
     * @return [R] result of processing the scope input
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    abstract fun applyArg(scopeValue: V, argValue: String): R

    /**
     * Converts this [ResultArg] into a [Processor] of the same type that calls the provided consumer.
     * @param consumer [Consumer]&lt;[R]&gt; called when this arg is successfully applied to an input. The result of the arg processing is passed into the consumer.
     * @return [Processor] wrapping this arg with the supplied consumer
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    fun to(consumer: Consumer<R>): Processor {
        return Processor(consumer)
    }

    /**
     * A [ResultArg] that calls a provided result consumer when the arg is applied. This is used in multi-arg result processing
     * @param V the value type received by this arg for processing
     * @param R the result type provided by processing
     * @param consumer [Consumer]&lt;[R]&gt; called when this arg is successfully applied to an input. The result of the arg processing is passed into the consumer.
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    inner class Processor internal constructor(private val consumer: Consumer<R>):
        ResultArg<V, R>(this@ResultArg.arg, this@ResultArg.fallback) {

        override fun applyArg(scopeValue: V, argValue: String): R {
            val result = this@ResultArg.applyArg(scopeValue, argValue)
            consumer.accept(result)
            return result
        }
    }

    companion object {

        private val noValueArgs: MutableSet<String> = mutableSetOf()

        /**
         * Returns a map of all arg keys and their associated values
         * @param scope with args included
         * @return [Map]&lt;String, String&gt; map of arg keys paired with their values or "" for a no-value-arg
         * @author fzzyhmstrs
         * @since 0.5.3
         */
        @JvmStatic
        fun getArgs(scope: String): Map<String, String> {
            if (!scope.contains('?')) return mapOf()
            val map: MutableMap<String, String> = mutableMapOf()
            val argScope = scope.substringAfter('?') + "?" // string should be argKey=value?argKey2=value2?. basically reverse the first ? to the end
            val reader = StringReader(argScope)
            while (reader.canRead()) {
                val keyIndex = reader.remaining.indexOf('?')
                val valueIndex = reader.remaining.indexOf('=')
                if (valueIndex < 0 || valueIndex > keyIndex) {
                    val noArgKey = reader.readStringUntil('?')
                    if(noValueArgs.contains(noArgKey)) {
                        map[noArgKey] = ""
                    } else {
                        return map
                    }
                } else {
                    val key = reader.readStringUntil('=')
                    val value = reader.readStringUntil('?')
                    map[key] = value
                }
            }
            return map
        }

        /**
         * Removes args from a scope string, leaving just the scope path. `my.scope?arg=true` to `my.scope`
         * @param scope scope with args to strip
         * @return scope without any args
         * @author fzzyhmstrs
         * @since 0.5.3
         */
        @JvmStatic
        fun stripArgs(scope: String): String {
            return scope.substringBefore('?')
        }
    }
}