package me.fzzyhmstrs.fzzy_config.entry

/**
 * wrapper interface for 4 basic handling interfaces
 * - serialize value
 * - deserialize inputs
 * - validate updates
 * - correct errors
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
interface EntryHandler<T: Any>: EntrySerializer<T>, EntryDeserializer<T>, EntryValidator<T>, EntryCorrector<T>