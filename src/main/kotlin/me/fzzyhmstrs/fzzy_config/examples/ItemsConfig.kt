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
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import net.minecraft.util.Identifier

class ItemsConfig: Config(Identifier(FC.MOD_ID, "items_config")) {

    //settings that apply to all items can go in the parent class

    var overallItemSetting = true
    var overallItemWeight = 10

    // category-specific settings can go into sections

    var axes = AxesSection() // axe settings are stored here
    class AxesSection: ConfigSection() { // a Config Section. Self-serializable, and will add a "layer" to the GUI.
        /* Axe-specific settings go here */
    }

    var swords = SwordsSection() // axe settings are stored here
    class SwordsSection: ConfigSection() { // a Config Section. Self-serializable, and will add a "layer" to the GUI.
        /* Sword-specific settings go here */
    }

    var tridents = TridentsSection() // axe settings are stored here
    class TridentsSection: ConfigSection() { // a Config Section. Self-serializable, and will add a "layer" to the GUI.
        /* Trident-specific settings go here */
    }
}