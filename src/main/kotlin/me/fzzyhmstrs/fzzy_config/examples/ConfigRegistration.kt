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

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType

internal object ConfigRegistration {

    fun registration() {
        //instance of your config loaded from file and automatically registered to the SyncedConfigRegistry and ClientConfigRegistry using the getId() method
        var myConfig = ConfigApi.registerAndLoadConfig({ MyConfig() })

        //adding the registerType, you can register a config as client-only. No syncing will occur. Useful for client-only mods.
        var myClientOnlyConfig = ConfigApi.registerAndLoadConfig({ MyConfig() }, RegisterType.CLIENT)

        //adding the registerType, you can register a config as sync-only. Their won't be any client-side GUI functionality, so the config will only be editable from the file itself, but it will auto-sync with clients.
        var mySyncedOnlyConfig = ConfigApi.registerAndLoadConfig({ MyConfig() }, RegisterType.SERVER)

        //Init function would be called in ModInitializer or some other entrypoint. Not strictly necessary if loading on-reference is ok.
        fun init() {}
    }
}