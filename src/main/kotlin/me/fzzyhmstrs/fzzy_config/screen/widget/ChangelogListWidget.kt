package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors
import java.util.function.Supplier

@Environment(EnvType.CLIENT)
class ChangelogListWidget(changelog: List<String>, private val sWidth: Supplier<Int>) : AlwaysSelectedEntryListWidget<ChangelogListWidget.Entry>(MinecraftClient.getInstance(), sWidth.get() - 16, 180, 0, 11) {

    override fun drawHeaderAndFooterSeparators(context: DrawContext?) {
    }

    override fun drawMenuListBackground(context: DrawContext?) {
    }

    override fun getWidth(): Int {
        return sWidth.get() - 16
    }

    override fun getRowWidth(): Int {
        return sWidth.get() - 36 //16 padding, 20 slider width and padding
    }

    override fun getScrollbarX(): Int {
        return this.x + this.width / 2 + this.rowWidth / 2 + 4
    }

    init{
        for (logEntry in changelog){
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