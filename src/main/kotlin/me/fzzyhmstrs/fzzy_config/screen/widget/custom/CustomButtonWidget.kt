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

import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import oshi.driver.windows.perfmon.PerfmonDisabled
import java.util.function.Consumer
import java.util.function.Supplier

open class CustomButtonWidget protected constructor(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    private val pressAction: Consumer<CustomButtonWidget>,
    private val narrationSupplier: ButtonWidget.NarrationSupplier,
    override val textures: PressableTextures = DEFAULT_TEXTURES,
    private val child: TooltipChild? = null)
    :
    CustomPressableWidget(x, y, width, height, message)
{

    override fun onPress() {
        pressAction.accept(this)
    }

    override fun getNarrationMessage(): MutableText {
        return narrationSupplier.createNarrationMessage { super.getNarrationMessage() }
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
        return child?.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused) ?: super.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused)
    }

    override fun provideNarrationLines(): List<Text> {
        return child?.provideNarrationLines() ?: super.provideNarrationLines()
    }

    class Builder(private val message: Text, private val onPress: Consumer<CustomButtonWidget>) {

        private var tooltip: Tooltip? = null
        private var x = 0
        private var y = 0
        private var width = 150
        private var height = 20
        private var narrationSupplier: ButtonWidget.NarrationSupplier = DEFAULT_NARRATION_SUPPLIER
        private var active = true
        private var textures: PressableTextures = DEFAULT_TEXTURES
        private var child: TooltipChild? = null

        fun position(x: Int, y: Int): Builder {
            this.x = x
            this.y = y
            return this
        }

        fun width(width: Int): Builder {
            this.width = width
            return this
        }

        fun size(width: Int, height: Int): Builder {
            this.width = width
            this.height = height
            return this
        }

        fun dimensions(x: Int, y: Int, width: Int, height: Int): Builder {
            return position(x, y).size(width, height)
        }

        fun tooltip(tooltip: Tooltip?): Builder {
            this.tooltip = tooltip
            return this
        }

        fun narrationSupplier(narrationSupplier: ButtonWidget.NarrationSupplier): Builder {
            this.narrationSupplier = narrationSupplier
            return this
        }

        fun active(active: Boolean): Builder {
            this.active = active
            return this
        }

        fun child(child: TooltipChild): Builder {
            this.child = child
            return this
        }

        fun textures(tex: Identifier, disabled: Identifier, highlighted: Identifier): Builder {
            this.textures = PressableTextures(tex, disabled, highlighted)
            return this
        }

        fun build(): CustomButtonWidget {
            val widget = CustomButtonWidget(x, y, width, height, message, onPress, narrationSupplier, textures, child)
            widget.tooltip = tooltip
            widget.active = active
            return widget
        }

    }

    companion object {

        @JvmStatic
        fun builder(message: Text, onPress: Consumer<CustomButtonWidget>): Builder {
            return Builder(message, onPress)
        }

        @JvmStatic
        protected val DEFAULT_NARRATION_SUPPLIER: ButtonWidget.NarrationSupplier = ButtonWidget.NarrationSupplier { textSupplier: Supplier<MutableText?> -> textSupplier.get() }
    }
}