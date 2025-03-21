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

import me.fzzyhmstrs.fzzy_config.fcId
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.SinglePreparationResourceReloader
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profiler


/*
* Saving for 0.7.0
* */
/*
internal object ConfigPresetsLoader: SinglePreparationResourceReloader<Map<String, ConfigPreset.Raw>>(), IdentifiableResourceReloadListener {

    override fun prepare(manager: ResourceManager, profiler: Profiler): Map<String, ConfigPreset.Raw> {
        TODO("Not yet implemented")
    }

    override fun apply(prepared: Map<String, ConfigPreset.Raw>?, manager: ResourceManager, profiler: Profiler) {
        TODO("Not yet implemented")
    }

    override fun getFabricId(): Identifier {
        return "presets_loader".fcId()
    }
}*/