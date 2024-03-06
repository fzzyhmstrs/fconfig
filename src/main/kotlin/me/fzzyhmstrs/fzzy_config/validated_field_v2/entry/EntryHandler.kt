package me.fzzyhmstrs.fzzy_config.validated_field_v2.entry

interface EntryHandler<T>: EntrySerializer<T>, EntryDeserializer<T>, EntryValidator<T>, EntryCorrector<T>
