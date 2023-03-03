package me.fzzyhmstrs.fzzy_config.config_util

@Target(AnnotationTarget.PROPERTY,AnnotationTarget.CLASS)
annotation class Lockable()

@Target(AnnotationTarget.PROPERTY,AnnotationTarget.CLASS)
annotation class ClientModifiable()

@Target(AnnotationTarget.PROPERTY,AnnotationTarget.CLASS)
annotation class ReadMeText(val translationKey: String = "",val description: String = "", val header: Array<String> = [])