package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Provided with updates that can
 *
 * Internal to FzzyConfig. Used by [ValidatedField] and [ConfigSection].
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Internal
internal interface Updatable: EntryKeyed {
    fun update(updateMessage: Text){ //pushes an update to the UpdateManager based on its key, so the manager can track individual change "threads"
        UpdateManager.update(this, updateMessage)
    }
    fun isDefault(): Boolean //checks if the Updatable is its default value or not
    fun restore() //forces the updatable to restore its default state
    fun revert() // reverts back to the pushedState, if any
    fun pushState() //pushes the state of the Updatable at the time of call for later checking
    fun peekState(): Boolean //peeks at the actual status of the update state. Used to determine if an update serialization is really needed, and to keep track of actual changes.
    fun popState(): Boolean // called at GUI closure or "SAVE" button selection, to determine "are you actually updated?"
}