package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors
import org.lwjgl.glfw.GLFW
import java.util.function.Function

@Environment(EnvType.CLIENT)
internal class ConfigScreen() : PopupWidgetScreen(title) {

    private val layout = ThreePartsLayoutWidget(this)

    override fun init() {
        super.init()
        initBody()
        initTabNavigation()
    }

    private fun initBody() {
        val directionalLayoutWidget = layout.addBody(DirectionalLayoutWidget.vertical().spacing(8))
        val textHeadingLayoutWidget = DirectionalLayoutWidget.horizontal().spacing(4)
        textHeadingLayoutWidget.add(IconWidget.create(0,0,"widget/entry_error".fcId())
        textHeadingLayoutWidget.add(TextWidget(MinecraftClient.getInstance().textRenderer,"fc.config.restart".translate()))
        textHeadingLayoutWidget.add(IconWidget.create(0,0,"widget/entry_error".fcId())
        directionalLayoutWidget.add(textHeadingLayoutWidget)
        directionalLayoutWidget.add(DividerWidget(180))
        directionalLayoutWidget.add(MultiLineTextWidget(MinecraftClient.getInstance().textRenderer,"fc.config.restart.sync".translate()).setCentered(true).setMaxWidth(180))
        directionalLayoutWidget.add(ButtonWidget.builder("fc.button.restart.confirm".translate()) { this.close(); this.client?.scheduleStop() }.dimension(0,0,180,20).build())
        directionalLayoutWidget.add(ButtonWidget.builder("fc.button.restart.cancel".translate()) { this.close(); this.client?.disconnect() }.dimension(0,0,180,20).build())
    }

    override fun initTabNavigation() {
        layout.refreshPositions()
    }

    override shouldCloseOnEscape(): Boolean {
        return false
    }
}
