package me.fzzyhmstrs.fzzy_config.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo


/**
 * Marks the annotated config entry as modifiable by any player, not just Ops.
 *
 * Typically, the opposite of using [WithPerms]. Should be used with caution, as this will enable any player, moderator, admin, or otherwise to modify a server-synced config setting. By default, all settings need Op Level 2.
 *
 * Not applicable if [NonSync] is used, these are treated as ClientModifiable automatically because they don't affect the server.
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.CLASS)
annotation class ClientModifiable

/**
 * Applies permission restrictions to a config setting.
 *
 * Defines the Operator Permission Level needed to make modifications to a config entry. By default, all entries need Op Level 2, hence the default at 3.
 *
 * @param opLevel the Operator Level required for modification. 1 = moderator, 2 = gamemaster, 3 = admin, 4 = owner
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.CLASS)
annotation class WithPerms(val opLevel: Int = 3)

/**
 * Excludes an element of a [Config][me.fzzyhmstrs.fzzy_config.config.Config] from syncronization.
 *
 * A NonSync element won't be synced back and forth between server and client. This is primarily useful for values that are client-specific: The color of something, the position of a GUI element. Things you don't want the server to overwrite every time the player joins, rather their local config that is loaded on launch takes precedent in those cases.
 *
 * NonSync elements will automatically act like [ClientModifiable], as they won't be synced anyway.
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY)
annotation class NonSync()

/**
 * Defines the version of the config file.
 *
 * Config serialization will prepend a 'version' key with the integer version of a config. defaults to 0, even if a Version annotation is not used. This number is passed to [Config]
 *
 * @param version the version number of the config. 0-indexed.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.CLASS)
annotation class Version(val version: Int)

/**
 * Adds a [Comment] to the [Version] key of a config file.
 *
 * TomlComment can't attach to a Class Annotation target. This fills that hole, and is applied as comment annotations to the "version" key added to the written config file. Can be applied multiple times (Repeatable annotation)
 *
 * @param text The comment text to write to file. Supports multi-line comments with triple-quotes.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class TomlHeaderComment(val text: String)