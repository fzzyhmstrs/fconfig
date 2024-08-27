/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.Walkable
import net.minecraft.util.Identifier

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
 * 3) {Recommended} [ValidatedField][me.fzzyhmstrs.fzzy_config.validation.ValidatedField] properties, or custom [Entry][me.fzzyhmstrs.fzzy_config.entry.Entry] implementations. These properties offer a wide array of powerful benefits to the user and the implementer
 * - Validation. Inputs are automatically validated
 * - Correction. Invalid inputs are automatically corrected, reverted, or skipped.
 * - Gui Support. FzzyConfigs auto-generated GUIs only take Entry's into account when building their GUI layers. The internal validation is used to build meaningful and helpful widgets with helpful tooltips and auto-suggestions, where possible. Entry's are auto-synced back to the server and other listening clients based on user permission level
 * @param identifier Identifier - The identifier of this config. Common groups of namespace will be the first "layer" of the Config GUI where applicable (all configs of the same namespace in one group), so it's recommended to use one unified namespace (modid, generally)
 * @param name String, optional -  the name of the config, this will be the file name (sans file extension). By default, this is defined from the path of the config identifier. NOTE: Do not add a file type to this name. That is done automatically where needed
 * @param folder String, optional - the subfolder inside the root config folder the file will be saved in. By default, this is defined from the namespace of the config identifier. Can be "", which will put the file in the root config folder.
 * @param subfolder String, optional - puts the config into a sub-subfolder inside the subfolder specified in [folder]. Does not affect ID or GUI layout
 * @see ConfigApi
 * @see ConfigSection
 * @see getId
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Suppress("unused")
open class Config @JvmOverloads constructor(protected val identifier: Identifier, val subfolder: String = "", val folder: String = identifier.namespace, val name: String = identifier.path): Walkable, Translatable {


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
     * Paired with [Version][me.fzzyhmstrs.fzzy_config.annotations.Version] annotations to perform needed manual updating of an outdated Config.
     *
     * If there are breaking changes with a new version of the API, `update` can be used to perform manual corrections on previous config data or user inputs. For example, if the config has a `scale` number that was previously a float between 0f-1f, but is now 0f-255f, `update` could apply a factor to correct the previous user input of 0.5f to 128f.
     *
     * @param deserializedVersion the version of the config read in from File.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    open fun update(deserializedVersion: Int) {}

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

}