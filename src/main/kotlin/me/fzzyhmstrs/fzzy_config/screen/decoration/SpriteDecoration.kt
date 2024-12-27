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


open class SpriteDecoration @JvmOverloads constructor(
    private val tex: TextureSet,
    override val h: Int = 16,
    override val w: Int = 16)
    : SpriteDecorated
{

    @JvmOverloads
    constructor(id: Identifier, h: Int = 16, w: Int = 16): this(TextureSet(id), h, w)

    override fun decorationId(): TextureSet {
        return tex
    }
}