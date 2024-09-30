/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.theme.impl.ThemeReloader
import net.minecraft.util.Identifier

object ThemeKeys {

    private fun int(id: String, default: Int): ThemeKey<Int> {
        return ConfigApi.theme().createAndRegisterInt(Identifier.of(FC.MOD_ID, id), default)
    }

    private fun str(id: String, default: String): ThemeKey<String> {
        return ConfigApi.theme().createAndRegisterString(Identifier.of(FC.MOD_ID, id), default)
    }

    private fun id(id: String, default: Identifier): ThemeKey<Identifier> {
        return ConfigApi.theme().createAndRegisterId(Identifier.of(FC.MOD_ID, id), default)
    }

    val CONFIG_STACK = Identifier.of(FC.MOD_ID, "config_stack")

    val HEADER_ALIGNMENT = ConfigApi.theme().createAndRegister(Identifier.of(FC.MOD_ID, "header_alignment"), PopupWidget.Builder.PositionGlobalAlignment.CODEC, PopupWidget.Builder.PositionGlobalAlignment.ALIGN_CENTER)
    val FOOTER_ALIGNMENT = ConfigApi.theme().createAndRegister(Identifier.of(FC.MOD_ID, "footer_alignment"), PopupWidget.Builder.PositionGlobalAlignment.CODEC, PopupWidget.Builder.PositionGlobalAlignment.ALIGN_CENTER)
    val LIST_ALIGNMENT = ConfigApi.theme().createAndRegister(Identifier.of(FC.MOD_ID, "list_alignment"), PopupWidget.Builder.PositionGlobalAlignment.CODEC, PopupWidget.Builder.PositionGlobalAlignment.ALIGN_CENTER)
    val LIST_ROW_WIDTH = int("list_row_width", 260)


    val WIDGET_HEIGHT = int("widget_height", 20)
    val WIDGET_WIDTH = int("widget_width", 110)
    val WIDGET_LIST_WIDTH = int("widget_list_width", 110)
    val WIDGET_MAP_KEY_WIDTH = int("widget_map_key_width", 110)
    val WIDGET_MAP_VALUE_WIDTH = int("widget_map_value_width", 110)

    val NUMBER_DECIMAL_FORMAT = str("number_decimal_format", "#.##")

    val BUTTON_BACKGROUND = id("button_background", Identifier.ofVanilla("widget/button"))
    val BUTTON_BACKGROUND_DISABLED = id("button_background_disabled", Identifier.ofVanilla("widget/button_disabled"))
    val BUTTON_BACKGROUND_HIGHLIGHTED = id("button_background_highlighted", Identifier.ofVanilla("widget/button_highlighted"))

    val SLIDER_BACKGROUND = id("slider_background", Identifier.ofVanilla("widget/slider"))
    val SLIDER_BACKGROUND_HIGHLIGHTED = id("slider_background_highlighted", Identifier.ofVanilla("widget/slider_highlighted"))
    val SLIDER_HANDLE = id("slider_handle", Identifier.ofVanilla("widget/slider_handle"))
    val SLIDER_HANDLE_HIGHLIGHTED = id("slider_handle_highlighted", Identifier.ofVanilla("widget/slider_handle_highlighted"))

    fun init() {
        ConfigApi.platform().registerClientReloader(Identifier.of(FC.MOD_ID, "theme_reloader"), ThemeReloader())
    }
}