package me.fzzyhmstrs.fzzy_config.screen.decoration

import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import net.minecraft.util.Identifier

/**
 * A [SpriteDecoration] that uses a 10x10 footprint
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class SmallSpriteDecoration: SpriteDecoration {

    /**
     * A small decoration using a texture set
     * @param tex [TextureSet] texture set for normal/disabled/highlighted textures
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    constructor(tex: TextureSet): super(tex, 10, 10)

    /**
     * A small decoration with one texture used in all circumstances
     * @param id [Identifier] sprite id for the decoration texture
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    constructor(id: Identifier): super(id, 10, 10)
}