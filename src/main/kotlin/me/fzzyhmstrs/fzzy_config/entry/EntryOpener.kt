package me.fzzyhmstrs.fzzy_config.entry

/**
 * An entry that implements EntryActor is expected to perform some sort of "opening" action when requested by the screen manager or other system in Fzzy Config. For example, a [ValidatedAny][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny] will open it's popup when requested.
 * @author fzzyhmstrs
 * @since 0.6.6
 */
@FunctionalInterface
fun interface EntryOpener {
    /**
     * Opens something. This is called by the screen manager when opening a screen and handling remaining sub-scope, and potentially other systems in the future.
     * @author fzzyhmstrs
     * @since 0.6.6
     */
    fun open()
}