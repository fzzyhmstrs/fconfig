/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Experimental
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Handles marking an entry as a visitable "layer". The layer itself is defined by the builder, and the layers default name as defined by translation key or annotation is also passed in by default
 * @author fzzyhmstrs
 * @since 0.6.0
 */
@Internal
@Experimental
@JvmDefaultWithCompatibility
interface EntryAnchor {

    fun anchorEntry(anchor: Anchor): Anchor {
        return anchor
    }

    class Anchor internal constructor(internal val layer: Int, internal var name: Text) {
        internal var type: AnchorType = AnchorType.CONFIG
        internal var decoration: Decorated? = null
        internal var offsetX = 0
        internal var offsetY = 0

        fun type(type: AnchorType): Anchor {
            this.type = type
            return this
        }
        fun name(name: Text): Anchor {
            this.name = name
            return this
        }
        fun decoration(decorated: Decorated, offsetX: Int = 0, offsetY: Int = 0): Anchor {
            this.decoration = decorated
            this.offsetX = offsetX
            this.offsetY = offsetY
            return this
        }
    }

    enum class AnchorType {
        CONFIG,
        SECTION,
        INLINE
    }
}