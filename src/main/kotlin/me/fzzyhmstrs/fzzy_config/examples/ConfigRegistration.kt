package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

internal object ConfigRegistration {

    //instance of your config loaded from file and automatically registered to the SyncedConfigRegistry and ClientConfigRegistry using the getId() method
    var myConfig = ConfigApi.registerAndLoadConfig( { MyConfig() } )

    //adding the registerType, you can register a config as client-only. No syncing will occur. Useful for client-only mods.
    var myClientOnlyConfig = ConfigApi.registerAndLoadConfig( { MyConfig() }, RegisterType.CLIENT )

    //adding the registerType, you can register a config as sync-only. Their won't be any client-side GUI functionality, so the config will only be editable from the file itself, but it will auto-sync with clients.
    var mySyncedOnlyConfig = ConfigApi.registerAndLoadConfig( { MyConfig() }, RegisterType.SYNC )

    //Init function would be called in ModInitializer or some other entrypoint. Not strictly necessary if loading on-reference is ok.
    fun init(){}
}