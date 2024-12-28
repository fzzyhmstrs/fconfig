package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.text.Text
import java.util.function.Function
import java.util.function.Supplier
import java.util.function.UnaryOperator

class ContextAction(val texts: Translatable.Result, val active: Supplier<Boolean>, val forMenu: Boolean, val icon: Decorated?, val action: Function<Position, Boolean>) {

    class Builder(private val name: Text, private val action: Function<Position, Boolean>) {
        private var narration: Text? = null
        private var active: Supplier<Boolean> = Supplier { true }
        private var icon: Decorated? = null
        private var forMenu: Boolean = true

        fun isForMenu(): Boolean {
            return forMenu
        }

        fun narration(narration: Text): Builder {
            this.narration = narration
            return this
        }

        fun active(active: Supplier<Boolean>): Builder {
            this.active = active
            return this
        }

        fun withActive(operator: UnaryOperator<Supplier<Boolean>>): Builder {
            this.active = operator.apply(this.active)
            return this
        }

        fun icon(icon: Decorated): Builder {
            this.icon = icon
            return this
        }

        fun notForMenu(): Builder {
            this.forMenu = false
            return this
        }

        fun build(): ContextAction {
            return ContextAction(Translatable.Result(name, narration), active, forMenu, icon, action)
        }
    }
}