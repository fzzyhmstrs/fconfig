package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import net.minecraft.util.Identifier

open class Config(val name: String, val folder: String, val subfolder: String = ""): Walkable {

    open fun getId(): Identifier {
        return if (folder.isNotEmpty())
            Identifier(folder, name)
        else
            Identifier(FC.MOD_ID,name)
    }

    /**
     * Saves the config to file. Called by FzzyConfig every time a config update is pushed from a client.
     *
     * Only saves on the client-side if [NonSync] fields were altered.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun save(){
        ConfigApi.save(name,folder,subfolder,this)
    }

    /**
     * Paired with [Version] to perform needed manual updating of an outdated Config.
     *
     * If there are breaking changes with a new version of the API, `update` can be used to perform manual corrections on previous config data or user inputs. For example, if the config has a `scale` number that was previously a float between 0f-1f, but is now 0f-255f, `update` could apply a factor to correct the previous user input of 0.5f to 128f.
     *
     * @param deserializedVersion the version of the config read in from File.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    open fun update(deserializedVersion: Int){}

}