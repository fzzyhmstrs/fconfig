package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.api.Translatable

class ExampleTranslatable: Translatable {
    override fun translationKey(): String {
        return "my.config.cool.translation"
    }
    override fun descriptionKey(): String {
        return "my.config.cool.translation.desc"
    }

    /* the lang would look like so:
    {
        "my.config.cool.translation": "Cool Translation"
        "my.config.cool.translation.desc": "A very cool translation description"
    }
     */
}