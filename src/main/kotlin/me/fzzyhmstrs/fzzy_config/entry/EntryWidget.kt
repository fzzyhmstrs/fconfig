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

    /**
     * Builds a new [ClickableWidget] for use in a config GUI. If the widget is presenting options that could be filtered by the optional [ChoiceValidator], those possible selections should be filtered in some way by the validator before presenting to the user.
     * @param choicePredicate [ChoiceValidator], optional. Default allows any option.
     * @return [ClickableWidget] instance; should be a new instance on every call.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun widgetEntry(choicePredicate: ChoiceValidator<T> = ChoiceValidator.any()): ClickableWidget

    /**
     * Builds a new [ClickableWidget] and applies a tooltip to it. This shouldn't need to be overridden in most cases
     * @param choicePredicate [ChoiceValidator], optional. Default allows any option.
     * @return [ClickableWidget] instance; should be a new instance on every call.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun widgetAndTooltipEntry(choicePredicate: ChoiceValidator<T> = ChoiceValidator.any()): ClickableWidget {
        val widget = widgetEntry(choicePredicate)
        if (this is Translatable && this.hasDescription()) {
            widget.tooltip = Tooltip.of(this.description(""))
        }
        return widget
    }
}