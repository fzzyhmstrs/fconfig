/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.context

import java.util.function.UnaryOperator

class ContextResultBuilder(private var position: Position) {
    private val actions: LinkedHashMap<String, MutableMap<ContextHandler.ContextType, ContextAction.Builder>> = linkedMapOf()

    fun add(group: String, type: ContextHandler.ContextType, builder: ContextAction.Builder) {
        actions.computeIfAbsent(group) { _-> mutableMapOf() }[type] = builder
    }

    fun addAll(group: String, builders: Map<ContextHandler.ContextType, ContextAction.Builder>) {
        actions.computeIfAbsent(group) { _-> mutableMapOf() }.putAll(builders)
    }

    fun apply(group: String, type: ContextHandler.ContextType, operator: UnaryOperator<ContextAction.Builder>) {
        val result = actions[group]?.get(type)?.let { operator.apply(it) }
        if (result != null) {
            add(group, type, result)
        }
    }

    fun apply(group: String, operator: UnaryOperator<MutableMap<ContextHandler.ContextType, ContextAction.Builder>>) {
        val result = actions[group]?.let { operator.apply(it) }
        if (result != null) {
            actions[group] = result
        }
    }

    fun move(operator: UnaryOperator<Position>) {
        position = operator.apply(position)
    }

    internal fun isNotEmpty(): Boolean {
        return actions.isNotEmpty()
    }

    internal fun apply(): Map<String, Map<ContextHandler.ContextType, ContextAction>> {
        return actions.mapValues { m -> m.value.mapValues { m2 -> m2.value.build() } }
    }

    internal fun position(): Position {
        return position
    }
}