package me.fzzyhmstrs.fzzy_config.entry

import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KClass


@ApiStatus.Internal
interface EntryDelegate {
    fun delegateClass(): KClass<*>
}