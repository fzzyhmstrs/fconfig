package me.fzzyhmstrs.fzzy_config.interfaces

import org.jetbrains.annotations.ApiStatus.Internal
import java.util.concurrent.Callable

/**
 * Implementing class can mark itself dirty, as well as pass that state along to listeners
 *
 * Internal to FzzyConfig. Used by [ValidatedField] and [ConfigSection].
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Internal
interface DirtyMarkable {
    fun markDirty()
    fun isDirty(): Boolean
    fun addDirtyListener(listener:DirtyMarkableContaining)
    fun updateListeners(update: Callable<ValidationResult<Boolean>>)
}
