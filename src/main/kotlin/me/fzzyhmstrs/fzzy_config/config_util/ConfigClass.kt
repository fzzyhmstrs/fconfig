package me.fzzyhmstrs.fzzy_config.config_util

import me.fzzyhmstrs.fzzy_config.interfaces.ServerClientSynced

open class ConfigClass(
    headerText: Header = Header(),
    decorator: LineDecorator = LineDecorator.DEFAULT)
    :
    ReadMeBuilder("default", headerText = headerText, decorator = decorator),
    ServerClientSynced
{
    constructor(configLabel: String): this(Header.Builder().literal().add(configLabel).build())
}