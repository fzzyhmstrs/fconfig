package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.text.Text
import java.util.function.Function
import java.util.function.Supplier
import java.util.function.UnaryOperator

//TODO
class ContextAction(val texts: Translatable.Result, val active: Supplier<Boolean>, val forMenu: Boolean, val icon: Decorated?, val action: Function<Position, Boolean>) {

    //TODO
    class Builder(private val name: Text, private val action: Function<Position, Boolean>) {
        private var narration: Text? = null
        private var active: Supplier<Boolean> = Supplier { true }
        private var icon: Decorated? = null
        private var forMenu: Boolean = true

        //TODO
        fun isForMenu(): Boolean {
            return forMenu
        }

        //TODO
        fun narration(narration: Text): Builder {
            this.narration = narration
            return this
        }

        //TODO
        fun active(active: Supplier<Boolean>): Builder {
            this.active = active
            return this
        }

        //TODO
        fun withActive(operator: UnaryOperator<Supplier<Boolean>>): Builder {
            this.active = operator.apply(this.active)
            return this
        }

        //TODO
        fun icon(icon: Decorated): Builder {
            this.icon = icon
            return this
        }

        //TODO
        fun notForMenu(): Builder {
            this.forMenu = false
            return this
        }

        //TODO
        fun build(): ContextAction {
            return ContextAction(Translatable.Result(name, narration), active, forMenu, icon, action)
        }
    }
}