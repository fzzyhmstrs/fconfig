package me.fzzyhmstrs.fzzy_config.config_util.gui

interface ConfigGuiEntry{
    fun widgets(theme: Theme): List<ThemedWidget>
}
