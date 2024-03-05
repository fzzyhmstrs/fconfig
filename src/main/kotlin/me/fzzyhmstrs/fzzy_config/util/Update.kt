package me.fzzyhmstrs.fzzy_config.util

import java.util.concurent.Callable
import net.minecraft.text.Text

data class Update(val desc: Text, val undo: Callable<Text>, val redo: Callable<Text>)
