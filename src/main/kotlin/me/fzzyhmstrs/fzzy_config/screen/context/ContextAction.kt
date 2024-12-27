package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.text.Text
import java.util.function.Consumer
import java.util.function.Supplier

class ContextAction(val texts: Translatable.Result, val active: Supplier<Boolean>, val forMenu: Boolean, val icon: Decorated?, val action: Consumer<Position>) {

    class Builder(private val name: Text, private val action: Consumer<Position>) {
        private var narration: Text? = null
        private var active: Supplier<Boolean> = Supplier { true }
        private var icon: Decorated? = null

        fun narration(narration: Text): Builder {
            this.narration = narration
            return this
        }

        fun active(active: Supplier<Boolean>): Builder {
            this.active = active
            return this
        }

        fun icon(icon: Decorated): Builder {
            this.icon = icon
            return this
        }

        fun build(): ContextAction {
            return ContextAction(Translatable.Result(name, narration), active, false, icon, action)
        }

        fun buildMenu(): ContextAction {
            return ContextAction(Translatable.Result(name, narration), active, true, icon, action)
        }
    }
}