package me.fzzyhmstrs.fzzy_config.validated_field_v2.entry

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget

interface Entry<T>: EntryHandler<T>{
    fun instanceEntry(): Entry<T>

    @Environment(EnvType.CLIENT)
    fun widgetEntry(): ClickableWidget
}