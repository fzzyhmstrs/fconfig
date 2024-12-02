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

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.entry.Decorated
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer

//client
internal class ScreenOpenButtonWidget(private val buttonTitle: Text, private val pressAction: Consumer<ScreenOpenButtonWidget>) : CustomPressableWidget(0, 0, 110, 20, FcText.empty()), Decorated {

    override fun getNarrationMessage(): MutableText {
        return this.message.copy()
    }

    override fun drawScrollableText(context: DrawContext?, textRenderer: TextRenderer?, xMargin: Int, color: Int) {
        val i = x + xMargin
        val j = x + getWidth() - xMargin
        drawScrollableText(context, textRenderer, buttonTitle, i, y, j, y + getHeight(), color)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun onPress() {
        pressAction.accept(this)
    }

    override fun decorationId(): Identifier {
        return "widget/decoration/open_screen".fcId()
    }
}