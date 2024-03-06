package me.fzzyhmstrs.fzzy_config.validated_field_v2.entry

fun interface EntryHandler<T>: EntrySerializer<T>, EntryDeserializer<T>, EntryValidator<T>, EntryCorrector<T>
