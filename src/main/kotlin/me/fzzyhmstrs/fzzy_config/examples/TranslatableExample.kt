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