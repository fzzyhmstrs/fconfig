/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.internal

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DirectionalLayoutWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.widget.*
import net.minecraft.client.realms.gui.screen.RealmsMainScreen

//client
internal class RestartScreen: PopupWidgetScreen(FcText.empty()) {

    private val layout = ThreePartsLayoutWidget(this)

    override fun init() {
        super.init()
        initBody()
        initTabNavigation()
    }

    private fun initBody() {
        val directionalLayoutWidget = layout.addBody(DirectionalLayoutWidget.vertical().spacing(8))
        val textHeadingLayoutWidget = DirectionalLayoutWidget.horizontal().spacing(4)
        val textWidget = TextWidget(MinecraftClient.getInstance().textRenderer.getWidth("fc.config.restart".translate()), 20, "fc.config.restart".translate(), MinecraftClient.getInstance().textRenderer).alignCenter()
        textHeadingLayoutWidget.add(IconWidget.create(20, 20, "textures/gui/sprites/widget/entry_error.png".fcId()))
        textHeadingLayoutWidget.add(textWidget)
        textHeadingLayoutWidget.add(IconWidget.create(20, 20, "textures/gui/sprites/widget/entry_error.png".fcId()))
        directionalLayoutWidget.add(textHeadingLayoutWidget) { it.alignHorizontalCenter() }
        directionalLayoutWidget.add(MultilineTextWidget("fc.config.restart.sync".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(180)) { it.alignHorizontalCenter() }
        directionalLayoutWidget.add(ButtonWidget.builder("menu.quit".translate()) { this.close(); this.client?.scheduleStop() }.dimensions(0, 0, 180, 20).build()) { it.alignHorizontalCenter() }
        directionalLayoutWidget.add(ButtonWidget.builder("fc.button.restart.cancel".translate()) { this.close(); disconnect() }.dimensions(0, 0, 180, 20).build()) { it.alignHorizontalCenter() }
        layout.forEachChild {
            addDrawableChild(it)
        }
    }

    private fun disconnect() {
        val c = client ?: return
        val sp = c.isInSingleplayer
        val serverInfo = c.currentServerEntry
        c.world?.disconnect()
        c.disconnect()
        val titleScreen = TitleScreen()
        if (sp) {
            c.setScreen(titleScreen)
        } else if (serverInfo != null && serverInfo.isRealm) {
            c.setScreen(RealmsMainScreen(titleScreen))
        } else {
            c.setScreen(MultiplayerScreen(titleScreen))
        }
    }

    override fun initTabNavigation() {
        layout.refreshPositions()
    }

    override fun shouldCloseOnEsc(): Boolean {
        return false
    }
}