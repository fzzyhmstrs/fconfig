/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier

internal class MyConfig: Config(Identifier(FC.MOD_ID, "my_config")) {

    var bareDouble = 5.0 // this won't have most of the features of the lib

    var booleanThing = false //yay, booleanssssss

    var validatedDouble = ValidatedDouble(5.0, 10.0, 0.0) //this has automatic validation, error correction, and will auto-generate a widget in the GUI for user selection

    var mySection = MySection() // a section of the config with its own validated fields and other sections as applicable
    internal class MySection: ConfigSection(){ // a Config Section. Self-serializable, and will add a "layer" to the GUI.

        var sectionBoolean = ValidatedBoolean(true)

        var sectionMap = ValidatedIdentifierMap(
            mapOf(),
            ValidatedIdentifier.ofTag(Registries.ITEM.getId(Items.IRON_AXE), ItemTags.AXES),
            ValidatedDouble(1.0, 1.0, 0.0)
        )
    }

    fun saveMe() {
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
    }


}