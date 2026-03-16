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
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import com.mojang.realmsclient.RealmsMainScreen
import net.minecraft.client.gui.components.ImageWidget
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.multiplayer.ClientLevel

//client
internal class RestartScreen: PopupWidgetScreen(FcText.EMPTY) {

    private val layout = HeaderAndFooterLayout(this)

    override fun init() {
        super.init()
        initBody()
        initLayout()
    }

    private fun initBody() {
        val directionalLayoutWidget = layout.addToContents(LinearLayout.vertical().spacing(8))
        val textHeadingLayoutWidget = LinearLayout.horizontal().spacing(4)
        val textWidget = StringWidget("fc.config.restart".translate(), Minecraft.getInstance().font).also { it.height = 20 }
        textHeadingLayoutWidget.addChild(ImageWidget.sprite(20, 20, "widget/entry_error".fcId()))
        textHeadingLayoutWidget.addChild(textWidget)
        textHeadingLayoutWidget.addChild(ImageWidget.sprite(20, 20, "widget/entry_error".fcId()))
        directionalLayoutWidget.addChild(textHeadingLayoutWidget) { it.alignHorizontallyCenter() }
        directionalLayoutWidget.addChild(MultiLineTextWidget("fc.config.restart.sync".translate(), Minecraft.getInstance().font).setCentered(true).setMaxWidth(180)) { it.alignHorizontallyCenter() }
        directionalLayoutWidget.addChild(CustomButtonWidget.builder("menu.quit".translate()) { this.onClose(); this.minecraft?.stop() }.dimensions(0, 0, 180, 20).build()) { it.alignHorizontallyCenter() }
        directionalLayoutWidget.addChild(CustomButtonWidget.builder("fc.button.restart.cancel".translate()) { this.onClose(); disconnect() }.dimensions(0, 0, 180, 20).build()) { it.alignHorizontallyCenter() }
        layout.visitWidgets {
            addRenderableWidget(it)
        }
    }

    private fun disconnect() {
        val c = minecraft ?: return
        val sp = c.isLocalServer
        val serverInfo = c.currentServer
        c.level?.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE)
        val titleScreen = TitleScreen()
        if (sp) {
            c.disconnect(titleScreen, false, true)
        } else if (serverInfo != null && serverInfo.isRealm) {
            c.disconnect(RealmsMainScreen(titleScreen), false, true)
        } else {
            c.disconnect(JoinMultiplayerScreen(titleScreen), false, true)
        }
    }

    private fun initLayout() {
        layout.arrangeElements()
    }

    override fun shouldCloseOnEsc(): Boolean {
        return false
    }
}