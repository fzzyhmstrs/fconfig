/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import java.util.function.Consumer
import java.util.function.Supplier

open class CustomButtonWidget protected constructor(x: Int, y: Int, width: Int, height: Int, message: Text, private val pressAction: Consumer<CustomButtonWidget>, private val narrationSupplier: ButtonWidget.NarrationSupplier) : CustomPressableWidget(x, y, width, height, message) {

    override fun onPress() {
        pressAction.accept(this)
    }

    override fun getNarrationMessage(): MutableText {
        return narrationSupplier.createNarrationMessage { super.getNarrationMessage() }
    }

    public override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        this.appendDefaultNarrations(builder)
    }

    companion object {
        @JvmStatic
        protected val DEFAULT_NARRATION_SUPPLIER: ButtonWidget.NarrationSupplier = ButtonWidget.NarrationSupplier { textSupplier: Supplier<MutableText?> -> textSupplier.get() }
    }
}