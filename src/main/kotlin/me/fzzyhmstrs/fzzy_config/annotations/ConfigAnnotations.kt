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
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import kotlin.reflect.KClass

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
 * Marks the annotated config entry as modifiable by any player, not just Ops.
 *
 * Typically, the opposite of using [WithPerms]. Should be used with caution, as this will enable any player, moderator, admin, or otherwise to modify a server-synced config setting. By default, all settings need Op Level 2.
 *
 * Not applicable if [NonSync] is used, these are treated as ClientModifiable automatically because they don't affect the server.
 * Order of precedence:
 * 1. [NonSync]
 * 2. [ClientModifiable] (Annotating config class itself)
 * 3. [ClientModifiable] (specific setting annotation)
 * 4. [WithCustomPerms] (specific setting annotation)
 * 5. [WithPerms] (specific setting annotation)
 * 6. [WithCustomPerms] (Annotating config class itself)
 * 7. [WithPerms] (Annotating config class itself)
 * 8. [Config.defaultPermLevel][me.fzzyhmstrs.fzzy_config.config.Config.defaultPermLevel]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class ClientModifiable

/**
 * Applies permission restrictions to a config setting.
 *
 * Defines the Operator Permission Level needed to make modifications to a config entry. By default, all entries need Op Level 2, hence this default starts at 3.
 *
 * Order of precedence:
 * 1. [NonSync]
 * 2. [ClientModifiable] (Annotating config class itself)
 * 3. [ClientModifiable] (specific setting annotation)
 * 4. [WithCustomPerms] (specific setting annotation)
 * 5. [WithPerms] (specific setting annotation)
 * 6. [WithCustomPerms] (Annotating config class itself)
 * 7. [WithPerms] (Annotating config class itself)
 * 8. [Config.defaultPermLevel][me.fzzyhmstrs.fzzy_config.config.Config.defaultPermLevel]
 * @param opLevel the Operator Level required for modification. 1 = moderator, 2 = gamemaster, 3 = admin, 4 = owner
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class WithPerms(val opLevel: Int = 3)

/**
 * Paired with [AdminAccess] Applies custom permission restrictions to a config setting. Overridden by [ClientModifiable]
 *
 * If this annotation is used in a Config, it should also have [AdminAccess] defined for the config class
 *
 * Uses permissions from LuckPerms, or any permissions mod that integrates with `fabric-permissions-api`.
 *
 * Order of precedence:
 * 1. [NonSync]
 * 2. [ClientModifiable] (Annotating config class itself)
 * 3. [ClientModifiable] (specific setting annotation)
 * 4. [WithCustomPerms] (specific setting annotation)
 * 5. [WithPerms] (specific setting annotation)
 * 6. [WithCustomPerms] (Annotating config class itself)
 * 7. [WithPerms] (Annotating config class itself)
 * 8. [Config.defaultPermLevel][me.fzzyhmstrs.fzzy_config.config.Config.defaultPermLevel]
 * @param perms Array&lt;String&gt; - permission groups allowed to access this setting. Groups need to be compatible with LuckPerms or similar.
 * @param fallback Int - Default -1 = no custom fallback behavior; it will use the default permissions of the class. If provided, uses vanilla logic: 1 = moderator, 2 = gamemaster, 3 = admin, 4 = owner
 * @author fzzyhmstrs
 * @since 0.3.8
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class WithCustomPerms(val perms: Array<String>, val fallback: Int = -1)

/**
 * Paired with [WithCustomPerms]. Defines the permissions needed for "admin" access to the config.
 *
 * Admin access will mark that a player can handle access violations (potential cheating of a config update) and other server-level issues, and will be notified in-game if such an error occurs while they are online.
 *
 * This annotation is outside of the chain of precedence of the others, it is solely responsible for determining admin access.
 *
 * If [WithCustomPerms] is used in the config class, this should be paired with it; otherwise the system will consider any server admin or owner (level 3+ perms) as an admin.
 * @param perms Array&lt;String&gt; - permission groups allowed to access this setting. Groups need to be compatible with LuckPerms or similar.
 * @param fallback Int - Default -1 = no custom fallback behavior; it will check for permission level of 3+. If provided, uses vanilla logic: 1 = moderator, 2 = gamemaster, 3 = admin, 4 = owner
 * @author fzzyhmstrs
 * @since 0.4.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class AdminAccess(val perms: Array<String>, val fallback: Int = -1)

/**
 * Excludes an element of a [Config][me.fzzyhmstrs.fzzy_config.config.Config] from synchronization.
 *
 * A NonSync element won't be synced back and forth between server and client, and [WithPerms] and [ClientModifiable] won't have any affect. This is primarily useful for values that are client-specific: The color of something, the position of a GUI element. Things you don't want the server to overwrite every time the player joins, rather their local config that is loaded on launch takes precedent in those cases.
 *
 * NonSync elements will automatically act like [ClientModifiable], as they won't be synced anyway.
 *
 * Order of precedence:
 * 1. [NonSync]
 * 2. [ClientModifiable] (Annotating config class itself)
 * 3. [ClientModifiable] (specific setting annotation)
 * 4. [WithCustomPerms] (specific setting annotation)
 * 5. [WithPerms] (specific setting annotation)
 * 6. [WithCustomPerms] (Annotating config class itself)
 * 7. [WithPerms] (Annotating config class itself)
 * 8. [Config.defaultPermLevel][me.fzzyhmstrs.fzzy_config.config.Config.defaultPermLevel]
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
 * Properties or fields marked with [RequiresRestart] will prompt the user that changes will require a restart of the server/client
 *
 * Classes marked with RequiresRestart will prompt the user that changes will require a restart of the server/client if any of their containing properties/fields are changed
 *
 * On local sync (loading into the world), if a property doesn't match between synced data and locally loaded data, a screen will pop up prompting a restart
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Deprecated("Use RequiresInfo instead. If your setting or config doesn't require a full restart, consider one of the other Action options.", replaceWith = ReplaceWith("@RequiresInfo(ConfigInfo.RESTART)", "me.fzzyhmstrs.fzzy_config.config.ConfigInfo"))
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class RequiresRestart

/**
 * Properties fields marked with [RequiresAction] will prompt the user that changes will require a certain action as defined by the [Action] enum selected
 *
 * Classes marked with [RequiresAction] will prompt the user that changes will require a certain action as defined by the [Action] enum selected, if any of their containing properties/fields are changed
 *
 * Actions have a priority based on their enum ordinal ([Action.RESTART] as the highest priority). If a property/field has an action of higher priority than the config class top level [RequiresAction], the settings action will take priority for that setting.
 *
 * On local sync (loading into the world), if a property doesn't match between synced data and locally loaded data, and a [Action.RESTART] is present in the changes, a screen will pop up prompting a restart
 * @param action [Action] the action the user needs to take if this setting or a setting in this config is changed
 * @author fzzyhmstrs
 * @since 0.4.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class RequiresAction(val action: Action)

/**
 * Defines an action the user or server owner needs to take if a setting is changed. Actions are sorted in priority by their ordinal ([RESTART] is the highest priority action and so on).
 *
 * The priority of the action defines which action will be displayed if both a setting and a config have a defined action. Only the higher priority will be displayed on an individual setting entry.
 * @author fzzyhmstrs
 * @since 0.4.0
 */
enum class Action(val restartPrompt: Boolean, val sprite: Identifier, val clientPrompt: Text, val clientUpdateMessage: Text, val serverUpdateMessage: Text, val settingTooltip: Text, val sectionTooltip: Text, val configTooltip: Text) {

    /**
     * Marks that a config or setting change will require a full restart if changed.
     *
     * Will prompt the user that a restart is required in the chat window
     *
     * Will prompt the server log that a restart is needed, and if a setting with this is found on client sync, a restart screen will pop up instead of game loading completing successfully.
     */
    RESTART(
        true,
        "widget/entry_error".fcId(),
        "fc.config.restart.update".translate(),
        "fc.config.restart.update.client".translate(),
        "fc.config.restart.update.server".translate(),
        "fc.config.restart.warning".translate().formatted(Formatting.RED),
        "fc.config.restart.warning.section".translate().formatted(Formatting.RED),
        "fc.config.restart.warning.config".translate().formatted(Formatting.RED)),

    /**
     * Marks that a config or setting change will require a user relog (disconnect -> reconnect from a server, leave -> reload single player world)
     *
     * Will prompt the user that a relog is needed in the chat window
     */
    RELOG(
        false,
        "widget/entry_error".fcId(),
        "fc.config.relog.update".translate(),
        "fc.config.relog.update.client".translate(),
        "fc.config.relog.update.server".translate(),
        "fc.config.relog.warning".translate().formatted(Formatting.RED),
        "fc.config.relog.warning.section".translate().formatted(Formatting.RED),
        "fc.config.relog.warning.config".translate().formatted(Formatting.RED)),

    /**
     * Marks that a config or setting change will need a data pack and resource pack reload to take effect (/reload and F3 + T)
     *
     * Will prompt the user that both resource types need to be reloaded
     */
    RELOAD_BOTH(
        false,
        "widget/entry_warning".fcId(),
        "fc.config.reload_both.update".translate(),
        "fc.config.reload_both.update.client".translate(),
        "fc.config.reload_both.update.server".translate(),
        "fc.config.reload_both.warning".translate().formatted(Formatting.GOLD),
        "fc.config.reload_both.warning.section".translate().formatted(Formatting.GOLD),
        "fc.config.reload_both.warning.config".translate().formatted(Formatting.GOLD)),

    /**
     * Marks that a config or setting change will need a data pack reload to take effect (/reload)
     *
     * Will prompt the user that data packs need to be reloaded
     */
    RELOAD_DATA(
        false,
        "widget/entry_warning".fcId(),
        "fc.config.reload_data.update".translate(),
        "fc.config.reload_data.update.client".translate(),
        "fc.config.reload_data.update.server".translate(),
        "fc.config.reload_data.warning".translate().formatted(Formatting.GOLD),
        "fc.config.reload_data.warning.section".translate().formatted(Formatting.GOLD),
        "fc.config.reload_data.warning.config".translate().formatted(Formatting.GOLD)),

    /**
     * Marks that a config or setting change will need a resource pack reload to take effect (F3 + T)
     *
     * Will prompt the user that resource packs need to be reloaded
     */
    RELOAD_RESOURCES(
        false,
        "widget/entry_warning".fcId(),
        "fc.config.reload_resources.update".translate(),
        "fc.config.reload_resources.update.client".translate(),
        "fc.config.reload_resources.update.server".translate(),
        "fc.config.reload_resources.warning".translate().formatted(Formatting.GOLD),
        "fc.config.reload_resources.warning.section".translate().formatted(Formatting.GOLD),
        "fc.config.reload_resources.warning.config".translate().formatted(Formatting.GOLD));


    fun priorityOf(other: Action): Action {
        return if (this == other) {
            this
        } else if(this.ordinal < other.ordinal) {
            this
        } else {
            other
        }
    }

    fun isPriority(other: Action): Boolean {
        return priorityOf(other) == this
    }
}



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