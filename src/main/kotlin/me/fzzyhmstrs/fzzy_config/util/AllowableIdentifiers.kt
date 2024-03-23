package me.fzzyhmstrs.fzzy_config.util

import net.minecraft.util.Identifier
import java.util.function.Predicate
import java.util.function.Supplier

class AllowableIdentifiers(private val predicate: Predicate<Identifier>, private val supplier: Supplier<List<Identifier>>) {
    fun test(identifier: Identifier): Boolean{
        return predicate.test(identifier)
    }
    fun get(): List<Identifier>{
        return supplier.get()
    }
}