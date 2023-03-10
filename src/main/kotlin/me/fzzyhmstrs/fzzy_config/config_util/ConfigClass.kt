package me.fzzyhmstrs.fzzy_config.config_util

import me.fzzyhmstrs.fzzy_config.interfaces.ServerClientSynced

/**
 *Superclass used to define an individual "section" of a configuration. Not to be confused with a [ConfigSection], which is a sub-section within a ConfigClass.
 *
 * A class used to store configuration properties does not necessarily have to be a ConfigClass, but this provides a configuration with an automatically generated ReadMe header, as well as automatic server-client syncing
 *
 * @constructor headerText: An [ReadMeBuilder.Header] instance that builds a ReadMe header. Optional; if left blank, the header will be blank.
 *
 * decorator: A [ReadMeBuilder.LineDecorator] instance that defines the formatting of ReadMe lines built within this class. Optional; defaults to [ReadMeBuilder.LineDecorator.DEFAULT]
 *
 * @see ReadMeBuilder
 * @see ServerClientSynced
 */
open class ConfigClass(
    headerText: Header = Header(),
    decorator: LineDecorator = LineDecorator.DEFAULT)
    :
    ReadMeBuilder("default", headerText = headerText, decorator = decorator),
    ServerClientSynced
{
    /**
     * Secondary constructor that builds a simple header using only configLabel
     */
    constructor(configLabel: String): this(Header.Builder().literal().add(configLabel).build())
}