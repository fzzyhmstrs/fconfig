/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny
import net.minecraft.util.Identifier

class BoisConfig: Config(Identifier(FC.MOD_ID, "bois_config")) {

    // If there are common clusters of settings you want to use in many places, such as mob stats,
    // you can use ValidatedAny to implement arrangements of settings from one common source
    class BoiStats(hp: Double, dmg: Double, spd: Double) { // a Config Section. Self-serializable, and will add a "layer" to the GUI.

        constructor(): this(20.0, 5.0, 0.3) // empty constructor for serialization and validation

        var health = hp
        var damage = dmg
        var speed = spd
    }

    //settings built from your generic boi stats object

    var bigBoi = ValidatedAny(BoiStats(40.0, 8.0, 0.15))

    var littleBoi = ValidatedAny(BoiStats(10.0, 1.0, 0.4))

    var regularBoi = ValidatedAny(BoiStats())

    // you don't need to use ValidatedAny! FzzyConfig knows to wrap objects with one internally
    var plainBoi = BoiStats()
}