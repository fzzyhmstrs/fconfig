/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.entry.Entry
import net.peanuuutz.tomlkt.TomlElement

/*
* Pushing to 0.7.0, setting up rn
*
* Process will be, roughly
* 1. modders add presets to their client resources
* 2. resource reloader creates a map of config string -> Raw
* 3. When ConfigScreenManager builds a screen for a config, map Raw -> ConfigPreset
* 3a. build total scope map for each config that uses config + root scope
* 4.
* */
/*
internal class ConfigPreset(val rootScope: String, val contents: Map<String, Entry<*, *>>) {
    internal class Raw(val rootScope: String, val translationKey: String, val content: TomlElement)
    fun interface RootScope {
        fun test(scope: String, annotations: List<Annotation>)
    }
}*/