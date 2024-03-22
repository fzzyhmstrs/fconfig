package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validated_field.map.ValidatedMap
import me.fzzyhmstrs.fzzy_config.validated_field.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validated_field.misc.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validated_field.number.ValidatedDouble
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags

internal class MyConfig: Config("my_config","fzzy_config") {

    var bareDouble = 5.0 //this won't have most of the features of the lib

    var validatedDouble = ValidatedDouble(5.0,10.0,0.0) //this has automatic validation, error correction, and will auto-generate a widget in the GUI for user selection

    var mySection = MySection() // a section of the config with its own validated fields and other sections as applicable
    internal class MySection: ConfigSection(){ // a Config Section. Self-serializable, and will add a "layer" to the GUI.

        var sectionBoolean = ValidatedBoolean(true)

        var sectionMap = ValidatedMap(
            mapOf(),
            ValidatedIdentifier.fromTag(Registries.ITEM.getId(Items.IRON_AXE),ItemTags.AXES),
            ValidatedDouble(1.0,1.0,0.0)
        )

    }


}