package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.decoration.SmallSpriteDecoration
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.function.ConstSupplier
import net.minecraft.text.Text
import java.util.function.Function
import java.util.function.Supplier
import java.util.function.UnaryOperator

/**
 * An action linked to contextual input, be it a keybind press, mouse click, or other event. This is the heart of the Context handling system in Fzzy Config, providing the actual "doing" of actions.
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/features/Context-Actions) for a detailed overview of the ContextAction system in fzzy config.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class ContextAction private constructor(val texts: Translatable.ResultProvider<*>, val active: Supplier<Boolean>, val forMenu: Boolean, val icon: Decorated?, val action: Function<Position, Boolean>) {

    /**
     * A builder of [ContextAction]. This is used in [ContextHandler] and [ContextProvider] to incrementally build actions, with modifications applied on different layers of context handling/provision as needed.
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/features/Context-Actions) for a detailed overview of the ContextAction system in fzzy config.
     * @param name [Text]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    class Builder(private val name: Text, private val action: Function<Position, Boolean>) {
        private var narration: Text? = null
        private var active: Supplier<Boolean> = ConstSupplier(true)
        private var icon: Decorated? = null
        private var forMenu: Boolean = true

        /**
         * Whether this builder is for context menu use or not.
         * @return true if this action is suitable for a context menu, false otherwise.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun isForMenu(): Boolean {
            return forMenu
        }

        /**
         * Adds context menu narration to this action. Default narration will only announce the action name. This narration should be used to provide detailed information about the circumstances and consequences of using the action.
         * @param narration [Text]
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun narration(narration: Text): Builder {
            this.narration = narration
            return this
        }

        /**
         * Replaces this builders active state supplier with a new one.
         *
         * The active supplier defines when a context action is applicable. For example, if a context action empties or clears an object, it should only be active when that object is in a non-empty state.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun active(active: Supplier<Boolean>): Builder {
            this.active = active
            return this
        }

        /**
         * Applies modifications to the current active supplier of this builder. This can be used to "AND" or "OR" a previous supplier, for example.
         *
         * The active supplier defines when a context action is applicable. For example, if a context action empties or clears an object, it should only be active when that object is in a non-empty state.
         * @param operator [UnaryOperator]&lt;[Supplier]&gt; applies changes to the current supplier and returns the modified supplier.
         * @see [active]
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun withActive(operator: UnaryOperator<Supplier<Boolean>>): Builder {
            this.active = operator.apply(this.active)
            return this
        }

        /**
         * Applies an icon to this action for display in a context menu. Default icon is no icon at all, simply a blank space to line the text up with actions that do have an icon.
         * @param icon [Decorated] icon decoration to render. This rendering can't be offset, so supply an icon that fits in the defined 10x10 space.
         * @see [SmallSpriteDecoration] a decoration class that renders in the 10x space needed
         * @see [TextureDeco] for example context icons
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun icon(icon: Decorated): Builder {
            TextureDeco
            this.icon = icon
            return this
        }

        /**
         * Marks that this action should not appear in a context menu (keybind-only activation)
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun notForMenu(): Builder {
            this.forMenu = false
            return this
        }

        /**
         * Builds the [ContextAction] for deployment in a context handler setting. This is handled internally for Config GUI entries, but can be used for actions added to other contexts.
         * @return [ContextAction]
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun build(): ContextAction {
            return ContextAction(Translatable.createResult(name, narration), active, forMenu, icon, action)
        }
    }
}