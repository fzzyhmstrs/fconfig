/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.custom

import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Supplier

//TODO
open class CustomButtonWidget protected constructor(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    private val pressAction: Consumer<CustomButtonWidget>,
    private val narrationSupplier: ButtonWidget.NarrationSupplier,
    private val narrationAppender: Consumer<NarrationMessageBuilder> = Consumer { _-> },
    override val textures: TextureSet = DEFAULT_TEXTURES,
    private val child: TooltipChild? = null,
    private val renderMessage: Boolean = true)
    :
    CustomPressableWidget(x, y, width, height, message)
{

    override fun onPress() {
        pressAction.accept(this)
    }

    override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        if (renderMessage)
            super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
    }

    override fun getNarrationMessage(): MutableText {
        return narrationSupplier.createNarrationMessage { super.getNarrationMessage() }
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
        return child?.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused) ?: super.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        super.appendClickableNarrations(builder)
        if (builder != null)
            narrationAppender.accept(builder)
    }

    //TODO
    class Builder(private val message: Text, private val onPress: Consumer<CustomButtonWidget>) {

        private var tooltip: Tooltip? = null
        private var x = 0
        private var y = 0
        private var w = 150
        private var h = 20
        private var narrationSupplier: ButtonWidget.NarrationSupplier = DEFAULT_NARRATION_SUPPLIER
        private var narrationAppender: Consumer<NarrationMessageBuilder> = Consumer { _-> }
        private var active = true
        private var textures: TextureSet = DEFAULT_TEXTURES
        private var child: TooltipChild? = null
        private var renderMessage: Boolean = true

        //TODO
        fun position(x: Int, y: Int): Builder {
            this.x = x
            this.y = y
            return this
        }

        //TODO
        fun width(width: Int): Builder {
            this.w = width
            return this
        }

        //TODO
        fun size(width: Int, height: Int): Builder {
            this.w = width
            this.h = height
            return this
        }

        //TODO
        fun dimensions(x: Int, y: Int, width: Int, height: Int): Builder {
            return position(x, y).size(width, height)
        }

        //TODO
        fun tooltip(tooltip: Tooltip?): Builder {
            this.tooltip = tooltip
            return this
        }

        //TODO
        fun narrationSupplier(narrationSupplier: ButtonWidget.NarrationSupplier): Builder {
            this.narrationSupplier = narrationSupplier
            return this
        }

        //TODO
        fun narrationAppender(narrationAppender: Consumer<NarrationMessageBuilder>): Builder {
            this.narrationAppender = narrationAppender
            return this
        }

        //TODO
        fun active(active: Boolean): Builder {
            this.active = active
            return this
        }

        //TODO
        fun child(child: TooltipChild): Builder {
            this.child = child
            return this
        }

        //TODO
        fun noMessage(): Builder {
            this.renderMessage = false
            return this
        }

        //TODO
        fun textures(tex: Identifier, disabled: Identifier, highlighted: Identifier): Builder {
            this.textures = TextureSet(tex, disabled, highlighted)
            return this
        }

        //TODO
        fun texture(tex: Identifier): Builder {
            this.textures = TextureSet(tex)
            return this
        }

        //TODO
        fun build(): CustomButtonWidget {
            val widget = CustomButtonWidget(x, y, w, h, message, onPress, narrationSupplier, narrationAppender, textures, child, renderMessage)
            widget.tooltip = tooltip
            widget.active = active
            return widget
        }

    }

    companion object {
        //TODO
        @JvmStatic
        fun builder(message: Text, onPress: Consumer<CustomButtonWidget>): Builder {
            return Builder(message, onPress)
        }

        //TODO
        @JvmStatic
        protected val DEFAULT_NARRATION_SUPPLIER: ButtonWidget.NarrationSupplier = ButtonWidget.NarrationSupplier { textSupplier: Supplier<MutableText?> -> textSupplier.get() }
    }
}