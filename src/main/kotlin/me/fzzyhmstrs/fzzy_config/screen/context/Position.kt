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

data class Position(val contextInput: ContextInput,
                    val mX: Int, val mY: Int, //mouse xy
                    val x: Int, val y: Int, // element xy
                    val width: Int, val height: Int, //element wh
                    val screenWidth: Int, val screenHeight: Int) //screen wh