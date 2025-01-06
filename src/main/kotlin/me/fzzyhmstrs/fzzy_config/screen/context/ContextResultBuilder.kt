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

/**
 * Builds an organized set of context actions for use by a context handler. This is typically built in an upstream direction; that is to say a parent will request context with an empty builder, and then building will start with the most downstream [ContextProvider] child, with intermediate layers then building on that result before ending up with a completely built-up builder back at the call site.
 *
 * Context builders organize context actions in two layers
 * - group: a string id used to cluster like actions together. In most context menus, these can be seen by the "divider lines" present between context action categories (think of the copy/cut/paste section for example)
 * - type: a [ContextType] key that marks which type of context this action is handling. This is mostly used for access to specific builders later, as the actual context event will simply be selecting an option from a context menu list.
 * - **NOTE:** this builder uses linked maps, so the entry order of groups and types will define their visual ordering.
 * @param position [Position] position context. This should start as the general parent context as applicable, often the screen position and dimensions. Children may update this position with more scoped context.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class ContextResultBuilder(private var position: Position) {
    private val actions: LinkedHashMap<String, MutableMap<ContextType, ContextAction.Builder>> = linkedMapOf()

    /**
     * Adds a context action builder to this builder
     * @param group [String] id of the group this context belongs to. See the predefined options below if you want to add to existing common groups.
     * @param type [ContextType] the type key for the action; this should generally be the same type that would result in this action if keyboard inputs were directly used (if this action has a keyboard input equivalent)
     * @param builder [ContextAction.Builder] the action builder
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun add(group: String, type: ContextType, builder: ContextAction.Builder) {
        actions.computeIfAbsent(group) { _-> mutableMapOf() }[type] = builder
    }

    /**
     * Adds a collection of context action builders to this builder
     * @param group [String] id of the group this context belongs to. See the predefined options below if you want to add to existing common groups.
     * @param builders Map&lt;[ContextType], [ContextAction.Builder]&gt; the action builders keyed to types
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun addAll(group: String, builders: Map<ContextType, ContextAction.Builder>) {
        actions.computeIfAbsent(group) { _-> mutableMapOf() }.putAll(builders)
    }

    /**
     * Applies changes to an existing [ContextAction.Builder] if it exists in this builder. Does nothing if it does not exist.
     * @param group [String] id of the group this context belongs to.
     * @param type [ContextType] the type key for the action.
     * @param operator [UnaryOperator]&lt;[ContextAction.Builder]&gt; modifier to apply changes to the existing builder, if present. Note that this can be used to completely replace a builder as needed.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun apply(group: String, type: ContextType, operator: UnaryOperator<ContextAction.Builder>) {
        val result = actions[group]?.get(type)?.let { operator.apply(it) }
        if (result != null) {
            add(group, type, result)
        }
    }

    /**
     * Applies changes to an existing group of [ContextAction.Builder] if any in the given group exist in this builder. Does nothing if they do not exist.
     * @param group [String] id of the group this context belongs to.
     * @param operator [UnaryOperator]&lt;Map&lt;[ContextType], [ContextAction.Builder]&gt;&gt; modifier to apply changes to the existing builders, if present. Note that this can be used to completely replace a builder group as needed.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun apply(group: String, operator: UnaryOperator<MutableMap<ContextType, ContextAction.Builder>>) {
        val result = actions[group]?.let { operator.apply(it) }
        if (result != null) {
            actions[group] = result
        }
    }

    /**
     * Modifies this builders [Position] context. Generally this would be done to "scope in" position, say to a parent elements position and dimensions.
     * @param operator [UnaryOperator]&lt;[Position]&gt; modifier for applying changes to the current position context.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun move(operator: UnaryOperator<Position>) {
        position = operator.apply(position)
    }

    internal fun isNotEmpty(): Boolean {
        return actions.isNotEmpty()
    }

    internal fun apply(): Map<String, Map<ContextType, ContextAction>> {
        return actions.mapValues { m -> m.value.mapValues { m2 -> m2.value.build() } }
    }

    internal fun position(): Position {
        return position
    }

    /**
     * Common predefined group ids used by fzzy config. Group actions to these if you want context to appear alongside defaults from these groups.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    companion object {
        /**
         * The primary group for config settings; context like copy, paste, and revert are put in this group.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        const val ENTRY = "entry"
        /**
         * Actions specific to collections like lists, maps, etc.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        const val COLLECTION = "collection"
        /**
         * Config-wide actions like save go in this group
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        const val CONFIG = "config"
    }
}