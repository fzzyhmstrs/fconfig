/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.minecraft.util.Colors
import java.util.function.Consumer
import java.util.function.Supplier

//client
internal class ChangelogListWidget(changelog: List<String>, private val sWidth: Supplier<Int>) : AlwaysSelectedEntryListWidget<ChangelogListWidget.Entry>(MinecraftClient.getInstance(), sWidth.get() - 16, 180, 0, 11) {

    init {
        this.setRenderBackground(false)
    }

    override fun getWidth(): Int {
        return sWidth.get() - 16
    }
    override fun getHeight(): Int {
        return this.height
    }
    override fun getNavigationFocus(): ScreenRect {
        return super.getNavigationFocus()
    }
    override fun forEachChild(consumer: Consumer<ClickableWidget>) {
    }

    override fun getRowWidth(): Int {
        return sWidth.get() - 36 //16 padding, 20 slider width and padding
    }

    override fun getScrollbarPositionX(): Int {
        return this.x + this.width / 2 + this.rowWidth / 2 + 4
    }

    init {
        for (logEntry in changelog) {
            this.addEntry(Entry(client.textRenderer.trimToWidth(logEntry, rowWidth - 4), logEntry))
        }
    }



    inner class Entry(private val logEntry: String, private val narratedEntry: String): AlwaysSelectedEntryListWidget.Entry<Entry>() {
        override fun render(
            context: DrawContext,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            context.drawTextWithShadow(this@ChangelogListWidget.client.textRenderer, logEntry, x+2, y, if (index % 2 == 0) Colors.WHITE else Colors.GRAY)
        }

        override fun getNarration(): Text {
            return narratedEntry.lit()
        }

    }

}