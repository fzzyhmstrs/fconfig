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

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
interface EntryAnchor {

    fun anchorEntry(anchor: Anchor): Anchor {
        return anchor
    }

    fun anchorId(scope: String): String {
        return scope
    }

    class Anchor internal constructor(internal var layer: Int, internal var name: Text) {
        internal var type: AnchorType = AnchorType.CONFIG
        internal var decoration: Decorated? = null
        internal var offsetX = 0
        internal var offsetY = 0

        /**
         * Updates the [AnchorType] of this anchor. Generally this should be respected from the types automatically added in ConfigSection/Group, as the anchor type defines the functionality of the anchor itself.
         * @param type [AnchorType]
         * @return this anchor
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun type(type: AnchorType): Anchor {
            this.type = type
            return this
        }

        /**
         * Updates the name of this anchor. If the default name of the anchor (config, section, group) is long and/or tedious in some way, a shortened "link version" of the name can be passed here
         * @param name [Text]
         * @return this anchor
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun name(name: Text): Anchor {
            this.name = name
            return this
        }

        /**
         * Applies a decoration and andy needed x/y position offsets for rendering it. The goto menu assumes a 16x16 space for the decoration for reference.
         * @param decorated [Decorated] any decorated instance.
         * @param offsetX offset in pixels horizontally for rendering
         * @param offsetY offset in pixels vertically for rendering
         * @return this anchor
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun decoration(decorated: Decorated, offsetX: Int = 0, offsetY: Int = 0): Anchor {
            this.decoration = decorated
            this.offsetX = offsetX
            this.offsetY = offsetY
            return this
        }
    }

    /**
     * Avoid changing whatever type has been given to the anchor by the relevant superclass. These define the anchor functionality.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    enum class AnchorType {
        CONFIG {
            override fun action(scope: String, anchorId: String): Runnable {
                return Runnable { ConfigApi.openScreen(anchorId) }
            }
        },
        SECTION {
            override fun action(scope: String, anchorId: String): Runnable {
                return Runnable { ConfigApi.openScreen(anchorId) }
            }
        },
        INLINE {
            override fun action(scope: String, anchorId: String): Runnable {
                return Runnable {
                    ConfigApi.openScreen(scope)
                    MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.scrollToGroup(anchorId)
                }
            }
        };

        abstract fun action(scope: String, anchorId: String): Runnable
    }
}