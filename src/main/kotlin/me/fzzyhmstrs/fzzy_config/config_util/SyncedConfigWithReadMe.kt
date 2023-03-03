package me.fzzyhmstrs.fzzy_config.config_util

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.interfaces.SyncedConfig
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry

abstract class SyncedConfigWithReadMe(
    private val configName: String,
    file: String,
    base: String = FC.MOD_ID,
    headerText: Header = Header.Builder().add("fc.config.$configName").space().build(),
    decorator: LineDecorator = LineDecorator.DEFAULT)
    :
    ReadMeBuilder(file,base,headerText, decorator),
    SyncedConfig
{
    override fun initConfig() {
        SyncedConfigRegistry.registerConfig(configName,this)
    }
}