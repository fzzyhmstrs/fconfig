/*
* Copyright (c) 2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */
package me.fzzyhmstrs.fzzy_config.util.error

import com.google.common.collect.HashMultimap
import me.fzzyhmstrs.fzzy_config.annotations.Action

import java.util.function.BiConsumer
import java.util.function.Consumer

class Error() {

    private val content: MultiMap<Type<*>, Entry<*>> = HashMultimap.create()
    private var critical: Boolean = false
    
    open fun isError(flags: Int): Boolean {
        return critical || content.keys().any { it.isError }
    }

    fun <C: Any> addError(type: Type<C>, content: C, e: Throwable? = null, msg: String? = null) {
        if (e != null)
            critical = true
        content.put(type, Entry.of(type, content, e, msg))
    }

    fun addError(other: Error) {
        critical = critical || other.critical
        content.putAll(other.content)
    }
    
    open fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
        for (entry in content.get(t)) {
            c.accept(entry)
        }
    }

    open fun <C: Any> consumeAll(c: BiConsumer<Type<*>, Entry<*>>) {
        for ((type, entry) in content.entries()) {
            c.accept(type, entry)
        }
    }


    ////////////////////////////

    sealed class Entry<C: Any>(val content: C, val e: Throwable, protected val name: String) {
        
        open fun print(c: BiConsumer<String, Throwable?>) {
            val crit = if (e != null) "Critical " else ""
            c.accept("$crit${name}: $content", e)
        }

        companion object {
            fun <C: Any> of(type: Type<C>, content: C, e: Throwable? = null, msg: String? = null): Entry<C> {
                return if (msg == null) {
                    BasicEntry(content, e, type.name)
                } else {
                    MsgEntry(content, e, type.name, msg)
                }
            }
        }
    }

    class BasicEntry<C: Any>(content: C, e: Throwable?, name: String): Entry<C>(content, exception, name)
    
    class MsgEntry<C: Any>(content: C, e: Throwable?, name: String, private val msg: String): Entry<C>(content, e, name) {
        override fun message(): String {
            val crit = if (e != null) "Critical " else ""
            c.accept("$crit${name}: $msg", e)
        }
    }

    ////////////////////////////

    class Single<C: Any>(private val type: Type<C>, content: C, e: Throwable? = null, msg: String? = null): Error() {
        private val entry
    }

    ////////////////////////////

    class Type<C: Any>(val name: String, val isError: Boolean = true)

    companion object {
        //flags:
        //0: critical only (flags will fail)
        //1: error
        //2: action
        val BASIC = Type<String>("Basic Error")
        val SERIALIZATION = Type<String>("Serialization Error")
        val DESERIALIZATION = Type<String>("Deserialization Error")
        val OUT_OF_BOUNDS = Type<String>("Value Out of Bounds")
        val FILE_STRUCTURE = Type<String>("File Structure Problem")
        val RESTART = Type<Action>("Restart Required", false)
        val ACTION = Type<Action>("Action Required", false)
    }
}
