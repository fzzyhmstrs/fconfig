package me.fzzyhmstrs.fzzy_config.validation.entry

/**
 * A base Entry for configs.
 *
 * Performs 6 basic functions
 * - serialize contents
 * - deserialize input
 * - validate updates
 * - correct errors
 * - provide widgets
 * - create instances
 *
 * SAM: [instanceEntry] creates an instance of this entry. Should be a copy of the original where possible
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
interface Entry<T: Any>: EntryHandler<T>, EntryWidget {
    fun instanceEntry(): Entry<T>

}