package me.fzzyhmstrs.fzzy_config.validated_field.entry

interface EntryHandler<T: Any>: EntrySerializer<T>, EntryDeserializer<T>, EntryValidator<T>, EntryCorrector<T>