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
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

//client
@Deprecated("To Remove")
internal open class ConfigConfigEntry(
    name: Text,
    description: Text,
    actions: Set<Action>,
    parent: ConfigListWidget,
    widget: ClickableWidget)
    :
    BaseConfigEntry(name, description, actions, parent, widget)
{
    override fun restartText(action: Action): Text {
        return action.configTooltip
    }

}