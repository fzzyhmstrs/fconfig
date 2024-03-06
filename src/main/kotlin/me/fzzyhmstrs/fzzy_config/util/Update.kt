package me.fzzyhmstrs.fzzy_config.util

import net.minecraft.text.Text
import java.util.concurrent.Callable

data class Update(val desc: Text, val undo: Callable<Text>, val redo: Callable<Text>)