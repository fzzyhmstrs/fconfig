/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.*
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.block.Block
import net.minecraft.entity.EntityType
import net.minecraft.fluid.Fluid
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.awt.Color
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

@JvmDefaultWithCompatibility
internal interface BasicValidationProvider {

    fun basicValidationStrategy(input: Any?, inputType: KType, annotations: List<Annotation>): ValidatedField<*>? {

        fun complexStrategy(input: Any?, type: KType, annotations: List<Annotation>): ValidatedField<*>? {
            try {
                val clazz = type.jvmErasure
                if (clazz.javaObjectType.isEnum) {
                    return (input as? Enum<*>)?.let { ValidatedEnum(it) }
                } else if (clazz.isSubclassOf(List::class)) {
                    val argument = type.arguments[0]
                    val argumentType = argument.type ?: return null
                    val projectionValidation = basicValidationStrategy(null, argumentType, annotations) ?: return null
                    return ValidatedList.tryMake(if (input != null) input as List<*> else listOf(), projectionValidation)
                }else if (clazz.isSubclassOf(Set::class)) {
                    val argument = type.arguments[0]
                    val argumentType = argument.type ?: return null
                    val projectionValidation = basicValidationStrategy(null, argumentType, annotations) ?: return null
                    return ValidatedSet.tryMake(if (input != null) input as Set<*> else setOf(), projectionValidation)
                } else if (clazz.isSubclassOf(Map::class)) {
                    val keyArgument = type.arguments[0]
                    val keyArgumentType = keyArgument.type ?: return null
                    val keyProjectionValidation = basicValidationStrategy(null, keyArgumentType, annotations) ?: return null
                    val valueArgument = type.arguments[1]
                    val valueArgumentType = valueArgument.type ?: return null
                    val valueProjectionValidation = basicValidationStrategy(null, valueArgumentType, annotations) ?: return null
                    return if(keyArgumentType.jvmErasure.javaObjectType.isEnum) {
                        ValidatedEnumMap.tryMake(if (input != null) input as Map<Enum<*>, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else if (keyArgumentType.jvmErasure.javaObjectType.isInstance("")) {
                        ValidatedStringMap.tryMake(if (input != null)input as Map<String, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else if (keyArgumentType.jvmErasure.javaObjectType.isInstance("i".fcId())) {
                        ValidatedIdentifierMap.tryMake(if (input != null)input as Map<Identifier, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else {
                        ValidatedMap.tryMake(if (input != null)input as Map<*, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    }
                } else {
                    return null
                }
            } catch (e: Throwable) {
                return null
            }
        }

        fun getIntRestrict(annotations: List<Annotation>): ValidatedInt.Restrict? {
            return annotations.firstOrNull { it is ValidatedInt.Restrict } as? ValidatedInt.Restrict
        }
        fun getByteRestrict(annotations: List<Annotation>): ValidatedByte.Restrict? {
            return annotations.firstOrNull { it is ValidatedByte.Restrict } as? ValidatedByte.Restrict
        }
        fun getShortRestrict(annotations: List<Annotation>): ValidatedShort.Restrict? {
            return annotations.firstOrNull { it is ValidatedShort.Restrict } as? ValidatedShort.Restrict
        }
        fun getLongRestrict(annotations: List<Annotation>): ValidatedLong.Restrict? {
            return annotations.firstOrNull { it is ValidatedLong.Restrict } as? ValidatedLong.Restrict
        }
        fun getDoubleRestrict(annotations: List<Annotation>): ValidatedDouble.Restrict? {
            return annotations.firstOrNull { it is ValidatedDouble.Restrict } as? ValidatedDouble.Restrict
        }
        fun getFloatRestrict(annotations: List<Annotation>): ValidatedFloat.Restrict? {
            return annotations.firstOrNull { it is ValidatedFloat.Restrict } as? ValidatedFloat.Restrict
        }

        try {
            return if (input != null) {
                if (input is ValidatedField<*>) {
                    return input
                } else if (input is Walkable && input !is ConfigSection) {
                    return ValidatedAny(input)
                } else {
                    when (val jot = inputType.jvmErasure.javaObjectType) {
                        java.lang.Integer::class.java -> getIntRestrict(annotations)?.let { ValidatedInt(input as Int, it.max, it.min) } ?: ValidatedInt(input as Int)
                        java.lang.Short::class.java -> getShortRestrict(annotations)?.let { ValidatedShort(input as Short, it.max, it.min) } ?: ValidatedShort(input as Short)
                        java.lang.Byte::class.java -> getByteRestrict(annotations)?.let { ValidatedByte(input as Byte, it.max, it.min) } ?: ValidatedByte(input as Byte)
                        java.lang.Long::class.java -> getLongRestrict(annotations)?.let { ValidatedLong(input as Long, it.max, it.min) } ?: ValidatedLong(input as Long)
                        java.lang.Double::class.java -> getDoubleRestrict(annotations)?.let { ValidatedDouble(input as Double, it.max, it.min) } ?: ValidatedDouble(input as Double)
                        java.lang.Float::class.java -> getFloatRestrict(annotations)?.let { ValidatedFloat(input as Float, it.max, it.min) } ?: ValidatedFloat(input as Float)
                        java.lang.Boolean::class.java -> ValidatedBoolean(input as Boolean)
                        java.awt.Color::class.java -> ValidatedColor(input as Color)
                        Identifier::class.java -> ValidatedIdentifier(input as Identifier)
                        java.lang.String::class.java -> ValidatedString(input as String)
                        TagKey::class.java -> ValidatedTagKey(input as TagKey<*>)
                        else -> {
                            if (Item::class.java.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as Item, Registries.ITEM)
                            } else if (Block::class.java.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as Block, Registries.BLOCK)
                            } else if (EntityType::class.java.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as EntityType<*>, Registries.ENTITY_TYPE)
                            } else if (Fluid::class.java.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as Fluid, Registries.FLUID)
                            } else {
                                complexStrategy(input, inputType, annotations)
                            }
                        }
                    }
                }
            } else {
                when (val jot = inputType.jvmErasure.javaObjectType) {
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
                    Item::class.java -> ValidatedRegistryType.of(Registries.ITEM)
                    Block::class.java -> ValidatedRegistryType.of(Registries.BLOCK)
                    EntityType::class.java -> ValidatedRegistryType.of(Registries.ENTITY_TYPE)
                    Fluid::class.java -> ValidatedRegistryType.of(Registries.FLUID)
                    else -> {
                        if (Item::class.java.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.ITEM)
                        } else if (Block::class.java.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.BLOCK)
                        } else if (EntityType::class.java.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.ENTITY_TYPE)
                        } else if (Fluid::class.java.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.FLUID)
                        } else {
                            complexStrategy(null, inputType, annotations)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            return null
        }
    }

}