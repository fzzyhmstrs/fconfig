package me.fzzyhmstrs.fzzy_config.validated_field.entry

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget

/**
 * Deserializes individual entries in a complex [ValidatedField]
 *
 * SAM: [deserialize] takes a TomlElement, returns a deserialized instance of T
 * @author fzzyhmstrs
 * @since 0.1.1
 */
@Environment(EnvType.CLIENT)
@FunctionalInterface
fun interface EntryWidget {
    @Environment(EnvType.CLIENT)
    fun widgetEntry(): ClickableWidget
}