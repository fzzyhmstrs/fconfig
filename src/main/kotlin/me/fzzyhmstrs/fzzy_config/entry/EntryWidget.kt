/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.util.FcText.description
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget

/**
 * Interface handles an [Entry] creating a ClickableWidget
 *
 * Expectation is that a new widget is made on every call
 * @author fzzyhmstrs
 * @since 0.2.0
 */
//client
interface EntryWidget<T> {

    fun widgetEntry(choicePredicate: ChoiceValidator<T> = ChoiceValidator.any()): ClickableWidget

    fun widgetAndTooltipEntry(choicePredicate: ChoiceValidator<T> = ChoiceValidator.any()): ClickableWidget {
        val widget = widgetEntry(choicePredicate)
        if (this is Translatable && this.hasDescription()) {
            widget.tooltip = Tooltip.of(this.description(""))
        }
        return widget
    }
}