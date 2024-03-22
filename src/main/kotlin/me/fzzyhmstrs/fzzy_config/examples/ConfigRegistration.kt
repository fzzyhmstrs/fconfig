package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType

internal object ConfigRegistration {

    //instance of your config loaded from file and automatically registered to the SyncedConfigRegistry and ClientConfigRegistry using the getId() method
    var myConfig = ConfigApi.registerAndLoadConfig( { MyConfig() } )

    //adding the registerType, you can register a config as client-only, or sync-only. This example is client only. No syncing will occur. Useful for client-only mods.
    var myClientOnlyConfig = ConfigApi.registerAndLoadConfig( { MyConfig() }, RegisterType.CLIENT )

    //Init function would be called in ModInitializer or some other entrypoint. Not strictly necessary if loading on-reference is ok.
    fun init(){}

}