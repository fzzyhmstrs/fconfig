package me.fzzyhmstrs.fzzy_config.screen.decoration

import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import net.minecraft.util.Identifier

class SmallSpriteDecoration: SpriteDecoration{

    constructor(tex: TextureSet): super(tex, 10, 10)
    constructor(id: Identifier): super(id, 10, 10)
}