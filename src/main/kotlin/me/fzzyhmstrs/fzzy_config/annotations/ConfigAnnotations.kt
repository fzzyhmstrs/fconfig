/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * A config marked with this annotation will attempt to ignore field visibility when de/serializing
 *
 * This can also be used to widen the access for Sections or other inner classes like classes wrapped by a [ValidatedAny][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny]
 *
 * Without this annotation, only PUBLIC fields/properties are considered, and there will be a crash if you try to pass a less-than-public inner class as a Section or Any
 * @author fzzyhmstrs
 * @since 0.3.0
 */
@Target(AnnotationTarget.CLASS)
annotation class IgnoreVisibility

/**
 * Marks the annotated config entry as modifiable by any player, not just Ops. Has higher priority than [WithPerms]
 *
 * Typically, the opposite of using [WithPerms]. Should be used with caution, as this will enable any player, moderator, admin, or otherwise to modify a server-synced config setting. By default, all settings need Op Level 2.
 *
 * Not applicable if [NonSync] is used, these are treated as ClientModifiable automatically because they don't affect the server.
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class ClientModifiable

/**
 * Applies permission restrictions to a config setting. Overriden by [ClientModifiable]
 *
 * Defines the Operator Permission Level needed to make modifications to a config entry. By default, all entries need Op Level 2, hence this default starts at 3.
 *
 * @param opLevel the Operator Level required for modification. 1 = moderator, 2 = gamemaster, 3 = admin, 4 = owner
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class WithPerms(val opLevel: Int = 3)

/**
 * Excludes an element of a [Config][me.fzzyhmstrs.fzzy_config.config.Config] from synchronization.
 *
 * A NonSync element won't be synced back and forth between server and client, and [WithPerms] and [ClientModifiable] won't have any affect. This is primarily useful for values that are client-specific: The color of something, the position of a GUI element. Things you don't want the server to overwrite every time the player joins, rather their local config that is loaded on launch takes precedent in those cases.
 *
 * NonSync elements will automatically act like [ClientModifiable], as they won't be synced anyway.
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
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
 * Properties or fields marked with RequiresRestart will prompt the user that changes will require a restart of the server/client
 *
 * Classes marked with RequiresRestart will prompt the user that changes will require a restart of the server/client if any of their containing properties/fields are changed
 *
 * On sync, if a property doesn't match synced data <-> loaded data, a screen will pop up prompting
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class RequiresRestart

/**
 * Provides the path to an old config file used before updating to FzzyConfig. FzzyConfig will attempt to read the file and scrape as much data as possible from it into the new config class and format (TOML).
 *
 * The old config file will be deleted after scraping
 *
 * Currently valid file types for old configs:
 * - .json
 * - .json5
 * - .jsonc
 * - .toml
 *
 * File searching is keyed off of the standard config folder fabric provides. If your config is in the config folder root, leave `folder` and `subfolder` blank.
 * @param fileName String - the file name of the old config, including file extension. Ex: `old_config_file.json5`
 * @param folder String, optional - the folder within the `./config/` directory your config is stored in
 * @param subfolder String, optional - the subfolder within the `./config/folder/` directory your config is stored in
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.CLASS)
annotation class ConvertFrom (val fileName: String, val folder: String = "", val subfolder: String = "")

/**
 * Adds a [Comment] to the [Version] key of a config file.
 *
 * TomlComment can't attach to a Class annotation target. This fills that hole, and is applied as comment annotations to the "version" key added to the written config file. Can be applied multiple times (Repeatable annotation)
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