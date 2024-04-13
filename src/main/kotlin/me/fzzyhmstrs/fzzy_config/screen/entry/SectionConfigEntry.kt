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