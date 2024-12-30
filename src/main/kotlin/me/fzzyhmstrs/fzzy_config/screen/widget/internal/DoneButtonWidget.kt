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

import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import net.minecraft.client.gui.screen.Screen
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import java.util.function.Consumer

internal class DoneButtonWidget(onPress: Consumer<CustomButtonWidget>): CustomButtonWidget(0, 0, 78, 20, ScreenTexts.DONE, onPress, DEFAULT_NARRATION_SUPPLIER) {

    override fun getMessage(): Text {
        return if (Screen.hasShiftDown()) {
            ScreenTexts.DONE
        } else {
            super.getMessage()
        }
    }

}