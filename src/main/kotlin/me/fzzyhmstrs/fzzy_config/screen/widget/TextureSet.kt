package me.fzzyhmstrs.fzzy_config.screen.widget

import net.minecraft.util.Identifier

data class TextureSet(private val tex: Identifier, private val disabled: Identifier, private val highlighted: Identifier) {

    constructor(id: Identifier): this(id, id, id)

    fun get(enabled: Boolean, focused: Boolean): Identifier {
        return if (enabled) {
            if (focused) highlighted else tex
        } else {
            disabled
        }
    }
}