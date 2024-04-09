package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import net.minecraft.text.Text

interface Updatable: EntryKeyed {
    fun getUpdateManager(): UpdateManager?
    fun setUpdateManager(manager: UpdateManager)
    fun update(updateMessage: Text) { //pushes an update to the UpdateManager based on its key, if one is present, so the manager can track changes
        getUpdateManager()?.update(this, updateMessage)
    }
    fun isDefault(): Boolean //checks if the Updatable is its default value or not
    fun restore() //forces the updatable to restore its default state
    fun revert() // reverts back to the pushedState, if any
    fun pushState() //pushes the state of the Updatable at the time of call for later checking
    fun peekState(): Boolean //peeks at the actual status of the update state. Used to determine if an update serialization is really needed, and to keep track of actual changes.
    fun popState(): Boolean // called at GUI closure or "SAVE" button selection, to determine "are you actually updated?"
}