package me.fzzyhmstrs.fzzy_config.interfaces

import me.fzzyhmstrs.fzzy_config.util.Update
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.concurrent.Callable

/**
 * Provided with updates that can 
 *
 * Internal to FzzyConfig. Used by [ValidatedField] and [ConfigSection].
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Internal
interface Updatable {
    fun updateKey(): String //returned by the Updatable element to denote its place in the heirarchy.
    fun update(update: Update){ //pushes an update to the UpdateManager based on its key, so the manager can track individual change "threads"
        UpdateManager.update(updateKey(), update)
    }
    fun pushState() //pushes the state of the Updatable at the time of call for later checking
    fun peekState() //peeks at the actual status of the update state. Used to determine if an update serialization is really needed, and to keep track of actual changes.
    fun popState(): Boolean // called at GUI closure or "SAVE" button selection, to determine "are you actually updated?"
}
