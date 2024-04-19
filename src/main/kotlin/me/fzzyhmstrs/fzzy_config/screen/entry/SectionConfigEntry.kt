/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text

@Environment(EnvType.CLIENT)
internal open class SectionConfigEntry(
    name: Text,
    description: Text,
    isRestartTriggering: Boolean,
    parent: ConfigListWidget,
    widget: ClickableWidget)
    :
    BaseConfigEntry(name, description, isRestartTriggering, parent, widget)
{
    override fun restartText(): MutableText{
        return "fc.config.restart.warning.section".translate()
    }

}