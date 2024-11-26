package me.fzzyhmstrs.fzzy_config.util.platform

import java.util.function.Supplier

interface Registrar<T> {

    fun register(name: String, entrySupplier: Supplier<T>): RegistrySupplier<T>
}