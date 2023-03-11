package me.fzzyhmstrs.fzzy_config.config_util

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.interfaces.ConfigSerializable
import me.fzzyhmstrs.fzzy_config.interfaces.ServerClientSynced

/**
 * A ConfigSection can be used to define a sub-section with a [ConfigClass].
 *
 * Unlike a Config Class, a section is auto-serializable and also indents the ReadMe by 1 tab
 *
 * @constructor headerText: An [ReadMeBuilder.Header] instance that builds a ReadMe header. Optional; if left blank, the header will be blank.
 *
 * decorator: A [ReadMeBuilder.LineDecorating] instance that defines the formatting of ReadMe lines built within this class. Optional; defaults to [ReadMeBuilder.LineDecorator.DEFAULT]
 *
 * @see ReadMeBuilder
 * @see ConfigSerializable
 * @see ServerClientSynced
 * @see ConfigClass
 */
open class ConfigSection(
    headerText: Header = Header(),
    decorator: LineDecorating = LineDecorator.DEFAULT)
    :
    ReadMeBuilder("","",headerText, decorator,1),
    ConfigSerializable,
    ServerClientSynced
{
    /**
     * Secondary constructor that builds a simple header using only sectionLabel
     */
    constructor(sectionLabel: String): this(Header.Builder().literal().add(sectionLabel).build())

    override fun serialize(): JsonElement {
        val str = SyncedConfigHelperV1.serializeConfig(this)
        return JsonParser.parseString(str)
    }

    override fun deserialize(json: JsonElement, fieldName: String): ValidationResult<Boolean> {
        val validatedSection = SyncedConfigHelperV1.deserializeConfig(this,json)
        return if (validatedSection.isError()){
            ValidationResult.error(true,validatedSection.getError())
        } else {
            ValidationResult.success(false)
        }
    }

    override fun toString(): String {
        return SyncedConfigHelperV1.serializeConfig(this)
    }
}