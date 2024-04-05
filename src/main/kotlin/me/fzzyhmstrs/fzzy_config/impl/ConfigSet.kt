package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.config.Config

internal data class ConfigSet(val active: Config, val base: Config, val clientOnly: Boolean)