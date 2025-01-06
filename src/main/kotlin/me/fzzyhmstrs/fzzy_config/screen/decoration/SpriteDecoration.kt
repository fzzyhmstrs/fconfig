/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.decoration

import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import net.minecraft.util.Identifier

/**
 * Implementation of a [SpriteDecorated] for rendering a [TextureSet] as a decoration
 * @param tex [TextureSet]
 * @param w Decoration height in pixels, default 16
 * @param h Decoration width in pixels, default 16
 * @author fzzyhmstrs
 * @since 0.6.0
 */
open class SpriteDecoration @JvmOverloads constructor(
    private val tex: TextureSet,
    override val w: Int = 16,
    override val h: Int = 16)
    : SpriteDecorated
{

    /**
     * [SpriteDecorated] that uses one texture in all circumstances
     * @param id [Identifier] Sprite id to render
     * @param w Decoration height in pixels, default 16
     * @param h Decoration width in pixels, default 16
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @JvmOverloads
    constructor(id: Identifier, w: Int = 16, h: Int = 16): this(TextureSet(id), w, h)

    override fun textureSet(): TextureSet {
        return tex
    }
}