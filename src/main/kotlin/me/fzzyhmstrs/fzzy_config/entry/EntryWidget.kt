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

import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget

/**
 * Interface handles an [Entry] creating a ClickableWidget
 *
 * Expectation is that a new widget is made on every call
 *
 * SAM: [widgetEntry] returns a ClickableWidget. This will be the widget shown in the config screen. For simple Entries, something like a TextFieldWidget, SliderWidget, or ButtonWidget may suffice. For more complex interactions, like Maps or Lists, a ButtonWidget that opens a PopupScreen to do further editing should be utilized.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
interface EntryWidget<T> {
    @Environment(EnvType.CLIENT)
    fun widgetEntry(choicePredicate: ChoiceValidator<T> = ChoiceValidator.any()): ClickableWidget
}