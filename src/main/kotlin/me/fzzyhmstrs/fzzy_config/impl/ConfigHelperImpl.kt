package me.fzzyhmstrs.fzzy_config.impl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.fzzyhmstrs.fzzy_config.annotations.*
import net.peanuuutz.tomlkt.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation

object ConfigHelperImpl {

    internal fun encodeToTomlElement(a: Any, clazz: KType): TomlElement?{
        return try {
            val strat = Toml.serializersModule.serializer(clazz)
            Toml. encodeToTomlElement(strat, a)
        } catch (e: Exception){
            null
        }
    }

    internal fun decodeFromTomlElement(element: TomlElement, clazz: KType): Any?{
        return try {
            val strat = Toml.serializersModule.serializer(clazz) as? KSerializer<*> ?: return null
            Toml.decodeFromTomlElement(strat, element)
        } catch (e: Exception){
            null
        }
    }

    internal fun isNonSync(property: KProperty<*>): Boolean{
        return property.annotations.firstOrNull { (it is NonSync) }?.let { true } ?: false
    }
    internal fun <T: Any> tomlAnnotations(property: KProperty1<T, *>): List<Annotation> {

        return property.annotations.map { mapJvmAnnotations(it) }.filter { it is TomlComment || it is TomlInline || it is TomlBlockArray || it is TomlMultilineString || it is TomlLiteralString || it is TomlInteger }
    }
    internal fun mapJvmAnnotations(input: Annotation): Annotation{
        return when(input) {
            is Comment -> TomlComment(input.value)
            is Inline -> TomlInline()
            is BlockArray -> TomlBlockArray(input.itemsPerLine)
            is MultilineString -> TomlMultilineString()
            is LiteralString -> TomlLiteralString()
            is Integer -> TomlInteger(input.base, input.group)
            else -> input
        }
    }
    internal fun <T: Any> tomlHeaderAnnotations(field: KClass<T>): List<TomlHeaderComment>{
        return field.annotations.mapNotNull { it as? TomlHeaderComment }
    }
    internal fun getVersion(clazz: KClass<*>): Int{
        val version = clazz.findAnnotation<Version>()
        return version?.version ?: 0
    }


}