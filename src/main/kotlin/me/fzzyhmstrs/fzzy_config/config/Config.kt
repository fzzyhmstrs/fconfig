/*
* Copyright (c) 2024-5 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.entry.EntryAnchor
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus.Internal
import java.io.File

/**
 * Base Config class for use with FzzyConfig
 *
 * Implement a subclass of this base class, and include the config options you want as public non-final properties ("vars"). Register your config in the [ConfigApi] to benefit from auto-syncing to clients, auto-GUI generation, automatic update saving, and more. Recommended registration pattern is
 * - Kotlin:`var myConfig = ConfigApi.registerAndLoadConfig { MyConfigClass() }`
 * - Java: `MyConfig myConfig = ConfigApi.registerAndLoadConfig(() -> new MyConfig())`
 *
 * For version control support, annotate your config classes with a [Version][me.fzzyhmstrs.fzzy_config.annotations.Version]. As you update the config, update this version number and implement any needed manual adjustments to [update]
 *
 * FzzyConfig can interact with three types of vars
 * 1) {Not Recommended} "Bare" properties. Your standard property (ex: `var myOption = 5`). These properties won't be validated, corrected, and won't show up in Config Guis. For an internal server-only setting, this may be an appropriate choice, but generally strongly recommended to use a Validation wrapper.
 * 2) {Recommended} [ConfigSection]. Inner "sections" of a Config. Can be used to organize related topics of a larger config. Interfaces with the auto-GUI system as a "sub-layer". A button will allow the user to drill down into the section, showing only the options for that section. Sections automatically de/serialize updated entries back to the server/clients like #3 below.
 * 3) {Recommended} [ValidatedField][me.fzzyhmstrs.fzzy_config.validation.ValidatedField] properties, or custom [Entry][me.fzzyhmstrs.fzzy_config.validation.entry.Entry] implementations. These properties offer a wide array of powerful benefits to the user and the implementer
 * - Validation. Inputs are automatically validated
 * - Correction. Invalid inputs are automatically corrected, reverted, or skipped.
 * - Gui Support. FzzyConfigs auto-generated GUIs only take Entry's into account when building their GUI layers. The internal validation is used to build meaningful and helpful widgets with helpful tooltips and auto-suggestions, where possible. Entry's are auto-synced back to the server and other listening clients based on user permission level
 * @param id Identifier - The identifier of this config. Common groups of namespace will be the first "layer" of the Config GUI where applicable (all configs of the same namespace in one group), so it's recommended to use one unified namespace (modid, generally)
 * @param name String, optional -  the name of the config, this will be the file name (sans file extension). By default this is defined from the path of the config identifier. NOTE: Do not add a file type to this name. That is done automatically where needed
 * @param folder String, optional - the subfolder inside the root config folder the file will be saved in. By default this is defined from the namespace of the config identifier. Can be "", which will put the file in the root config folder.
 * @param subfolder String, optional - puts the config into a sub-subfolder inside the subfolder specified in [folder]. Does not affect ID or GUI layout
 * @see ConfigApi
 * @see ConfigSection
 * @see ConfigAction
 * @see ConfigGroup
 * @see getId
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Suppress("unused")
open class Config @JvmOverloads constructor(protected val identifier: Identifier, val subfolder: String = "", val folder: String = identifier.namespace, val name: String = identifier.path): Walkable, Translatable, EntryAnchor {

    /**
     * The identifier of this Config.
     *
     * Used by the internal registries to store and group this registry with like registries. When the Config GUI is opened, all configs of a common namespace will be grouped together.
     * @return Identifier of this config
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun getId(): Identifier {
        return identifier
    }

    /**
     * Creates and returns a [File] corresponding to this config file directory. Does not include the file name
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    fun getDir(): File {
        return if (subfolder != "") {
            File(File(PlatformUtils.configDir(), folder), subfolder)
        } else {
            if (folder != "") {
                File(PlatformUtils.configDir(), folder)
            } else {
                PlatformUtils.configDir()
            }
        }
    }

    /**
     * Saves the config to file.
     *
     * Called by FzzyConfig every time a config update is pushed from a client. Use if you have some custom method for altering configurations and need to save the changes to file. Only recommended to use this on the client for client-only settings
     *
     * Only automatically saves on the client-side if [NonSync][me.fzzyhmstrs.fzzy_config.annotations.NonSync] fields were altered.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun save() {
        ConfigApi.save(this)
    }

    /**
     * The default vanilla permission level of entries in this config. Users will need to have at least this permission level to modify entries for synced configs except for entries that are [me.fzzyhmstrs.fzzy_config.annotations.ClientModifiable] or [me.fzzyhmstrs.fzzy_config.annotations.NonSync]
     *
     * Override specific setting permission levels with [me.fzzyhmstrs.fzzy_config.annotations.WithPerms]
     *
     * 1 = moderator, 2 = gamemaster, 3 = admin, 4 = owner
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    open fun defaultPermLevel(): Int {
        return 2
    }

    /**
     * The file type this config saves as. Default is .toml files.
     * @return [FileType] the output file conversion to use.
     * @author fzzyhmstrs
     * @since 0.6.7
     */
    open fun fileType(): FileType {
        return FileType.TOML
    }

    /**
     * The save type for updates received by a client. If the config is registered as SERVER and this returns [SaveType.SEPARATE], the config won't sync at all.
     * @return [SaveType] the save behavior when a client is updated.
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    open fun saveType(): SaveType {
        return SaveType.OVERWRITE
    }

    /**
     * Paired with [Version][me.fzzyhmstrs.fzzy_config.annotations.Version] annotations to perform needed manual updating of an outdated Config.
     *
     * If there are breaking changes with a new version of the API, `update` can be used to perform manual corrections on previous config data or user inputs. For example, if the config has a `scale` number that was previously a float between 0f-1f, but is now 0f-255f, `update` could apply a factor to correct the previous user input of 0.5f to 128f.
     *
     * @param deserializedVersion the version of the config read in from File.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    open fun update(deserializedVersion: Int){}

    /**
     * Runs on the logical client after a config is synced from the server. This occurs when the player is logging in and when datapacks are reloaded. This is distinct from [onUpdateClient], which fires when _changes_ are made to a config in-game, which are also synced. This is the initial sync of the entire config state.
     *
     * This should only perform client logic, and it is good practice to insulate client-only code by putting a method reference to a dedicated client-only class in this call.
     * @see onUpdateClient
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    open fun onSyncClient(){}

    /**
     * Runs on the logical server as config is about to be synced to a client. This occurs when the player is logging in and when datapacks are reloaded. This is distinct from [onUpdateServer], which fires when _changes_ are made to a config in-game, which are also synced. This is the initial sync of the entire config state.
     *
     * Client-only code shouldn't be run here.
     * @see onUpdateServer
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    open fun onSyncServer(){}

    /**
     * Runs on the logical client after the config is updated. Typically, this is when the user closes the config screen or applies changes, but also occurs after a connected client recieves a S2C update. This is distinct from [onSyncClient], which fires when the entire config state is synced on login/reload. This handles chnages made in-game.
     *
     * This should only perform client logic, and it is good practice to insulate client-only code by putting a method reference to a dedicated client-only class in this call.
     * @see onUpdateServer
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    open fun onUpdateClient(){}

    /**
     * Runs on the logical server after an updated config is prepared for saving. Typically, this will be after a config update is received from a connected client, and that update passes permission checks.
     *
     * Client-only code shouldn't be run here.
     * @param playerEntity [ServerPlayerEntity] - the player that provided the update.
     * @see onUpdateClient
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    open fun onUpdateServer(playerEntity: ServerPlayerEntity){}

    /**
     * Anchor modifier method for a config. By default, provides a folder icon decoration to the base anchor. You can provide a custom icon if you want a special icon for the config in the goto menu. If your config has a long name, you may also want to create and provide a shortened "summary" name for a goto link.
     * @param anchor [EntryAnchor.Anchor] automatically generated input Anchor for modification.
     * @return Anchor with any desired modifications. If you still want the folder deco, call super
     * @see [TextureDeco] for other built in icons
     * @see [me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecoration] for a simple class to build your own icon
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun anchorEntry(anchor: EntryAnchor.Anchor): EntryAnchor.Anchor {
        return anchor.decoration(TextureDeco.DECO_FOLDER)
    }

    @Internal
    final override fun anchorId(scope: String): String {
        return translationKey()
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return ConfigApi.serializeConfig(this, mutableListOf())
    }
    /**
     * @suppress
     */
    override fun translationKey(): String {
        return getId().toTranslationKey()
    }
    /**
     * @suppress
     */
    override fun descriptionKey(): String {
        return getId().toTranslationKey("", ".desc")
    }
    /**
     * @suppress
     */
    override fun translation(fallback: String?): MutableText {
        return Translatable.getScopedResult(translationKey())?.name?.nullCast() ?: super.translation(fallback)
    }
    /**
     * @suppress
     */
    override fun description(fallback: String?): MutableText {
        return Translatable.getScopedResult(translationKey())?.desc?.nullCast() ?: super.description(fallback)
    }
    /**
     * @suppress
     */
    override fun prefix(fallback: String?): MutableText {
        return Translatable.getScopedResult(translationKey())?.prefix?.nullCast() ?: super.prefix(fallback)
    }
}