package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.text.Text

class ContextAction(val texts: Translatable.Result, val active: Boolean, val icon: Decorated?, val action: Runnable) {

    class Builder(private val name: Text, private val action: Runnable) {
        private var narration: Text? = null
        private var active: Boolean = true
        private var icon: Decorated? = null

        fun narration(narration: Text): Builder {
            this.narration = narration
            return this
        }

        fun active(active: Boolean): Builder {
            this.active = active
            return this
        }

        fun icon(icon: Decorated): Builder {
            this.icon = icon
            return this
        }

        fun build(): ContextAction {
            return ContextAction(Translatable.Result(name, narration), active, icon, action)
        }
    }
}