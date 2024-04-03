package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable

enum class TestEnum: EnumTranslatable{
    ALPHA,
    BETA,
    GAMMA,
    DELTA,
    EPSILON;
    override fun prefix(): String {
        return "test_config.fzzy_config"
    }
}
