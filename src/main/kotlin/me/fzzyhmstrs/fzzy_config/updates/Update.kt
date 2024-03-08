package me.fzzyhmstrs.fzzy_config.updates

import net.minecraft.text.Text
import java.util.concurrent.Callable

data class Update(val desc: Text, val undo: Callable<Text>, val redo: Callable<Text>)