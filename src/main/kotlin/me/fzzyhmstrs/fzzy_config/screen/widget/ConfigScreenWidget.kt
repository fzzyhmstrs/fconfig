/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier


//client
/**
 * A widget that will open a fzzy config screen directly on press.
 * @author fzzyhmstrs
 * @since 0.5.7, reimplemented with CustomPressableWidget 0.6.3
 */
class ConfigScreenWidget private constructor(
    private val scope: String,
    position: Position)
    :
    CustomPressableWidget(0, 0, 20, 20, FcText.EMPTY)
{
    init {
        setPosition(position.positionX(MinecraftClient.getInstance().window.scaledWidth), position.positionY(MinecraftClient.getInstance().window.scaledHeight))
    }

    private fun getTex(): Identifier {
        if(!active)
            return TextureIds.CONFIG_INACTIVE
        return if (hovered || isFocused)
            TextureIds.CONFIG_HIGHLIGHTED
        else
            TextureIds.CONFIG
    }

    override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = ClientConfigRegistry.hasScreen(scope)
        context.drawTex(getTex(), x, y, getWidth(), getHeight())
        setTooltip(if (this.active) {
            Tooltip.of(TextureIds.CONFIG_LANG)
        } else {
            Tooltip.of(TextureIds.CONFIG_INACTIVE_LANG)
        })
    }

    override fun getNarrationMessage(): MutableText {
        return if(active) TextureIds.CONFIG_LANG.copy() else TextureIds.CONFIG_INACTIVE_LANG.copy()
    }

    override fun onPress() {
        ClientConfigRegistry.openScreen(scope)
    }


    companion object {
        /**
         * Creates a 20x20 textless button widget that opens a config screen based on the mod_id provided
         * @param scope String representation of the config to open. Usually the mod_id for the config
         * @param x Int horizontal position of the widget
         * @param y Int vertical position of the widget
         * @author fzzyhmstrs
         * @since 0.5.7
         */
        @JvmStatic
        @JvmOverloads
        fun of(scope: String, x: Int = 0, y: Int = 0): ConfigScreenWidget {
            return ConfigScreenWidget(scope, Position(Position.Corner.ABSOLUTE, x, y))
        }

        /**
         * Creates a 20x20 textless button widget that opens a config screen based on the mod_id provided
         * @param scope String representation of the config to open. Usually the mod_id for the config
         * @param corner [Position.Corner] which corner of the screen to anchor the
         * @param xPadding Int - padding in scaled pixels the widget will be away from the specified left/right edge of the screen.
         * @param yPadding Int - padding in scaled pixels the widget will be away from the specified top/bottom edge of the screen.
         * @author fzzyhmstrs
         * @since 0.5.7
         */
        @JvmStatic
        @JvmOverloads
        fun of (scope: String, corner: Position.Corner, xPadding: Int = 4, yPadding: Int = xPadding): ConfigScreenWidget {
            return ConfigScreenWidget(scope, Position(corner, xPadding, yPadding))
        }
    }

    class Position(private val corner: Corner, private val xOffset: Int, private val yOffset: Int) {

        fun positionX(width: Int): Int {
            return corner.positionX(xOffset, width)
        }

        fun positionY(height: Int): Int {
            return corner.positionY(yOffset, height)
        }

        enum class Corner {
            TOP_LEFT {
                override fun positionX(x: Int, width: Int): Int {
                    return x
                }

                override fun positionY(y: Int, height: Int): Int {
                    return y
                }
            },
            TOP_RIGHT {
                override fun positionX(x: Int, width: Int): Int {
                    return width - 20 - x
                }

                override fun positionY(y: Int, height: Int): Int {
                    return y
                }
            },
            BOTTOM_LEFT {
                override fun positionX(x: Int, width: Int): Int {
                    return x
                }

                override fun positionY(y: Int, height: Int): Int {
                    return height - 20 - y
                }
            },
            BOTTOM_RIGHT {
                override fun positionX(x: Int, width: Int): Int {
                    return width - 20 - x
                }

                override fun positionY(y: Int, height: Int): Int {
                    return height - 20 - y
                }
            },
            ABSOLUTE {
                override fun positionX(x: Int, width: Int): Int {
                    return x
                }

                override fun positionY(y: Int, height: Int): Int {
                    return y
                }
            };

            abstract fun positionX(x: Int, width: Int): Int
            abstract fun positionY(y: Int, height: Int): Int
        }
    }
}