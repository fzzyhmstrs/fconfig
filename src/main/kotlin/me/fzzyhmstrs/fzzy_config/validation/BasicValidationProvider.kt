package me.fzzyhmstrs.fzzy_config.validation

import me.fzzyhmstrs.fzzy_config.validation.collection.*
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.awt.Color
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

@JvmDefaultWithCompatibility
interface BasicValidationProvider {

    fun basicValidationStrategy(input: Any?, inputType: KType): ValidatedField<*>? {

        fun complexStrategy(input: Any?,type: KType): ValidatedField<*>? {
            try {
                val clazz = type.jvmErasure
                if (clazz.javaObjectType.isEnum) {
                    return (clazz.javaObjectType as? Class<Enum<*>>)?.let { ValidatedEnum(it.enumConstants[0]) }
                } else if (clazz.isSubclassOf(List::class)) {
                    val argument = type.arguments[0]
                    val argumentType = argument.type ?: return null
                    val projectionValidation = basicValidationStrategy(null, argumentType) ?: return null
                    return ValidatedList.tryMake(if (input != null) input as List<*> else listOf(), projectionValidation)
                }else if (clazz.isSubclassOf(Set::class)) {
                    val argument = type.arguments[0]
                    val argumentType = argument.type ?: return null
                    val projectionValidation = basicValidationStrategy(null, argumentType) ?: return null
                    return ValidatedSet.tryMake(if (input != null) input as Set<*> else setOf(), projectionValidation)
                } else if (clazz.isSubclassOf(Map::class)) {
                    val keyArgument = type.arguments[0]
                    val keyArgumentType = keyArgument.type ?: return null
                    val keyProjectionValidation = basicValidationStrategy(null, keyArgumentType) ?: return null
                    val valueArgument = type.arguments[1]
                    val valueArgumentType = valueArgument.type ?: return null
                    val valueProjectionValidation = basicValidationStrategy(null, valueArgumentType) ?: return null
                    return if(keyArgumentType.jvmErasure.javaObjectType.isEnum){
                        ValidatedEnumMap.tryMake(if (input != null) input as Map<Enum<*>,*> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else if (keyArgumentType.jvmErasure.javaObjectType.isInstance("")) {
                        ValidatedStringMap.tryMake(if (input != null)input as Map<String,*> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else if (keyArgumentType.jvmErasure.javaObjectType.isInstance(Identifier(""))) {
                        ValidatedIdentifierMap.tryMake(if (input != null)input as Map<Identifier,*> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else {
                        ValidatedMap.tryMake(if (input != null)input as Map<*,*> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    }
                } else {
                    return null
                }
            } catch (e: Exception){
                return null
            }
        }

        try {
            return if (input != null)
                if (input is ValidatedField<*>)
                    return input
                else
                    when (inputType.jvmErasure.javaObjectType) {
                        java.lang.Integer::class.java -> ValidatedInt(input as Int)
                        java.lang.Short::class.java -> ValidatedShort(input as Short)
                        java.lang.Byte::class.java -> ValidatedByte(input as Byte)
                        java.lang.Long::class.java -> ValidatedLong(input as Long)
                        java.lang.Double::class.java -> ValidatedDouble(input as Double)
                        java.lang.Float::class.java -> ValidatedFloat(input as Float)
                        java.lang.Boolean::class.java -> ValidatedBoolean(input as Boolean)
                        java.awt.Color::class.java -> ValidatedColor(input as Color)
                        Identifier::class.java -> ValidatedIdentifier(input as Identifier)
                        java.lang.String::class.java -> ValidatedString(input as String)
                        TagKey::class.java -> ValidatedTagKey(input as TagKey<*>)
                        else -> complexStrategy(input, inputType)
                    }
            else
                when (inputType.jvmErasure.javaObjectType) {
                    java.lang.Integer::class.java -> ValidatedInt()
                    java.lang.Short::class.java -> ValidatedShort()
                    java.lang.Byte::class.java -> ValidatedByte()
                    java.lang.Long::class.java -> ValidatedLong()
                    java.lang.Double::class.java -> ValidatedDouble()
                    java.lang.Float::class.java -> ValidatedFloat()
                    java.lang.Boolean::class.java -> ValidatedBoolean()
                    java.awt.Color::class.java -> ValidatedColor()
                    Identifier::class.java -> ValidatedIdentifier()
                    java.lang.String::class.java -> ValidatedString()
                    else -> complexStrategy(null, inputType)
                }
        } catch (e: Exception){
            return null
        }
    }

}