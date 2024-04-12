package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

@Environment(EnvType.CLIENT)
internal open class ConfigConfigEntry(
    name: Text,
    description: Text,
    isRestartTriggering: Boolean,
    parent: ConfigListWidget,
    widget: ClickableWidget)
    :
    BaseConfigEntry(name, description, isRestartTriggering, parent, widget)
{
    override fun restartText(): MutableText {
        return "fc.config.restart.warning.config".translate()
    }

}