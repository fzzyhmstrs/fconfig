/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.util.Translatable

class TranslatableExample {

    fun translatable() {
        class ExampleTranslatable: Translatable {
            override fun translationKey(): String {
                return "my.config.cool.translation"
            }

            override fun descriptionKey(): String {
                return "my.config.cool.translation.desc"
            }
        }
    }
}