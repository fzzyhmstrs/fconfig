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

import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

@Environment(EnvType.CLIENT)
internal open class ValidatedAnyConfigEntry(
    name: Text,
    description: Text,
    actions: Set<Action>,
    parent: ConfigListWidget,
    widget: ClickableWidget,
    copyAction: Runnable?,
    pasteAction: Runnable?,
    rightClickAction: RightClickAction?)
    :
    SettingConfigEntry(name, description, actions, parent, widget, copyAction, pasteAction, rightClickAction)
{
    override fun restartText(action: Action): Text {
        return action.sectionTooltip
    }
}