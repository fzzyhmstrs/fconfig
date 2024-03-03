package me.fzzyhmstrs.fzzy_config.interfaces

/**
 * OldClass defines an old revision of a ConfigClass for use in [me.fzzyhmstrs.fzzy_config.config.SyncedConfigHelperV1.readOrCreateUpdatedAndValidate]
 *
 * SAM: [generateNewClass], returns the post-updated version of the new class revision.
 */
interface OldClass<T>{
    /**
     * Typical implementation of this method involves:
     * 1. Instantiate a default instance of the new_class that is being updated to
     * 2. Update all properties in the new class that have a counterpart in the old class. For example, if the old class has `int1` and the new_class has `int1` and `int2`, you would update the new_classes int1 with the old classes version, leaving int2 as the new default.
     * 3. Pass back the modified new_class.
     *
     * @return T, the new class instance after modification
     */
    fun generateNewClass(): T
}