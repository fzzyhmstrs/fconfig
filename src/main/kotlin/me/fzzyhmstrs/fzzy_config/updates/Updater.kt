package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Update
import net.minecraft.text.Text
import java.util.*

internal class Updater{
    private val undoStack: LinkedList<Update> = LinkedList()
    private val redoStack: LinkedList<Update> = LinkedList()

    internal fun update(update: Update){
        redoStack.clear() //invalidate the redo stack, as it may conflict with the new applied update
        undoStack.push(update)
    }

    internal fun canUndo(): Boolean{
        return undoStack.isNotEmpty()
    }

    internal fun canRedo(): Boolean{
        return redoStack.isNotEmpty()
    }

    internal fun getUndoDesc(): Text {
        return if(canUndo()) undoStack.peek().desc else FcText.empty()
    }

    internal fun getRedoText(): Text{
        return if(canRedo()) redoStack.peek().desc else FcText.empty()
    }

    internal fun undo(): Text{
        if (!canUndo()) return FcText.empty()
        val update = undoStack.pop()
        val undoDesc = update.undo.call()
        redoStack.push(update)
        return undoDesc
    }

    internal fun redo(): Text{
        if (!canRedo()) return FcText.empty()
        val update = redoStack.pop()
        val redoDesc = update.redo.call()
        undoStack.push(update)
        return redoDesc
    }

    internal fun revert(redo: Boolean = true){
        while (undoStack.isNotEmpty()){
            val update = undoStack.pop()
            update.undo.call()
            if (redo)
                redoStack.push(update)
        }
    }

    internal fun changeCount(): Int{
        return undoStack.size
    }


}
