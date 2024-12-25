/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.context

import org.lwjgl.glfw.GLFW

interface ContextHandler {

    fun handleContext(contextType: ContextType): Boolean

    enum class ContextType {
        PAGE_UP {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_PAGE_UP
            }
        },
        PAGE_DOWN {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_PAGE_DOWN
            }
        },
        HOME {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_HOME
            }
        },
        END {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_END
            }
        },
        COPY {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_V && ctrl && !shift && !alt
            }
        },
        PASTE {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_V && ctrl && !shift && !alt
            }
        },
        CUT {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_X && ctrl && !shift && !alt
            }
        },
        FIND {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_F && ctrl && !shift && !alt
            }
        },
        SAVE {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_S && ctrl && !shift && !alt
            }
        },
        UNDO {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return keyCode == GLFW.GLFW_KEY_Z && ctrl && !shift && !alt
            }
        },
        CONTEXT {
            override fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
                return (keyCode == GLFW.GLFW_KEY_F10 && !ctrl && shift && !alt) || keyCode == GLFW.GLFW_KEY_MENU
            }
        };

        abstract fun relevant(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean
    }

    companion object {
        fun getRelevantContext(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): ContextType? {
            for (type in ContextType.entries) {
                if (type.relevant(keyCode, ctrl, shift, alt)) {
                    return type
                }
            }
            return null
        }
    }
}