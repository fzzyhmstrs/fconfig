package me.fzzyhmstrs.fzzy_config.validated_field.entry

interface Entry<T: Any>: EntryHandler<T>, EntryWidget {
    fun instanceEntry(): Entry<T>

}