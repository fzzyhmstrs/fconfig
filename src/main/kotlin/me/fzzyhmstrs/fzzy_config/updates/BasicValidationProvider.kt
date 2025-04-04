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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager.Base.basicValidationStrategy
import me.fzzyhmstrs.fzzy_config.util.TriState
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
import kotlin.reflect.KCallable
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

@JvmDefaultWithCompatibility
internal interface BasicValidationProvider {

    fun basicValidationStrategy(input: Any?, inputType: KCallable<*>, fieldName: String): ValidatedField<*>? {
        try {
            return if (input != null) {
                if (input is ValidatedField<*>) {
                    return input
                } else if (input is Walkable && input !is ConfigSection) {
                    return ValidatedAny(input)
                } else {
                    when (val jot = input::class.java) {
                        intClass -> getIntRestrict(inputType.annotations)?.let {
                            ValidatedInt(
                                input as Int,
                                it.max,
                                it.min,
                                if ((it.max == Int.MAX_VALUE || it.min == Int.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedInt(input as Int)
                        shortClass -> getShortRestrict(inputType.annotations)?.let {
                            ValidatedShort(
                                input as Short,
                                it.max,
                                it.min,
                                if ((it.max == Short.MAX_VALUE || it.min == Short.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedShort(input as Short)
                        byteClass -> getByteRestrict(inputType.annotations)?.let {
                            ValidatedByte(
                                input as Byte,
                                it.max,
                                it.min,
                                if ((it.max == Byte.MAX_VALUE || it.min == Byte.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedByte(input as Byte)
                        longClass -> getLongRestrict(inputType.annotations)?.let {
                            ValidatedLong(
                                input as Long,
                                it.max,
                                it.min,
                                if ((it.max == Long.MAX_VALUE || it.min == Long.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedLong(input as Long)
                        doubleClass -> getDoubleRestrict(inputType.annotations)?.let {
                            ValidatedDouble(
                                input as Double,
                                it.max,
                                it.min,
                                if ((it.max == Double.MAX_VALUE || it.min == -Double.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedDouble(input as Double)
                        floatClass -> getFloatRestrict(inputType.annotations)?.let {
                            ValidatedFloat(
                                input as Float,
                                it.max,
                                it.min,
                                if ((it.max == Float.MAX_VALUE || it.min == -Float.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedFloat(input as Float)
                        booleanClass -> ValidatedBoolean(input as Boolean)
                        triStateClass -> ValidatedTriState(TriState.DEFAULT)
                        colorClass -> ValidatedColor(input as Color)
                        Identifier::class.java -> ValidatedIdentifier(input as Identifier)
                        stringClass -> ValidatedString(input as String)
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
                                complexStrategy(input, inputType.returnType, fieldName, inputType.annotations)
                            }
                        }
                    }
                }
            } else {
                val ktype = inputType.returnType
                when (val jot = ktype.jvmErasure.javaObjectType) {
                    intClass -> ValidatedInt()
                    shortClass -> ValidatedShort()
                    byteClass -> ValidatedByte()
                    longClass -> ValidatedLong()
                    doubleClass -> ValidatedDouble()
                    floatClass -> ValidatedFloat()
                    booleanClass -> ValidatedBoolean()
                    triStateClass -> ValidatedTriState(TriState.DEFAULT)
                    colorClass -> ValidatedColor()
                    Identifier::class.java -> ValidatedIdentifier()
                    stringClass -> ValidatedString()
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
                            complexStrategy(null, ktype, fieldName, inputType.annotations)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            FC.DEVLOG.error("Basic Validation Failed:")
            decorateError(fieldName, inputType, inputType.annotations)
            FC.DEVLOG.error("   > Possible Cause: ${e.message}")
            return null
        }
    }

    fun basicValidationStrategy(input: Any?, inputType: KType, fieldName: String, annotations: List<Annotation>): ValidatedField<*>? {
        try {
            return if (input != null) {
                if (input is ValidatedField<*>) {
                    return input
                } else if (input is Walkable && input !is ConfigSection) {
                    return ValidatedAny(input)
                } else {
                    when (val jot = input::class.java) {
                        booleanClass -> ValidatedBoolean(input as Boolean)
                        intClass -> getIntRestrict(annotations)?.let {
                            ValidatedInt(
                                input as Int,
                                it.max,
                                it.min,
                                if ((it.max == Int.MAX_VALUE || it.min == Int.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedInt(input as Int)
                        doubleClass -> getDoubleRestrict(annotations)?.let {
                            ValidatedDouble(
                                input as Double,
                                it.max,
                                it.min,
                                if ((it.max == Double.MAX_VALUE || it.min == -Double.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedDouble(input as Double)
                        floatClass -> getFloatRestrict(annotations)?.let {
                            ValidatedFloat(
                                input as Float,
                                it.max,
                                it.min,
                                if ((it.max == Float.MAX_VALUE || it.min == -Float.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedFloat(input as Float)

                        triStateClass -> ValidatedTriState(TriState.DEFAULT)
                        idClass -> ValidatedIdentifier(input as Identifier)
                        stringClass -> ValidatedString(input as String)
                        tagClass -> ValidatedTagKey(input as TagKey<*>)
                        shortClass -> getShortRestrict(annotations)?.let {
                            ValidatedShort(
                                input as Short,
                                it.max,
                                it.min,
                                if ((it.max == Short.MAX_VALUE || it.min == Short.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedShort(input as Short)
                        byteClass -> getByteRestrict(annotations)?.let {
                            ValidatedByte(
                                input as Byte,
                                it.max,
                                it.min,
                                if ((it.max == Byte.MAX_VALUE || it.min == Byte.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedByte(input as Byte)
                        longClass -> getLongRestrict(annotations)?.let {
                            ValidatedLong(
                                input as Long,
                                it.max,
                                it.min,
                                if ((it.max == Long.MAX_VALUE || it.min == Long.MIN_VALUE) && it.type == ValidatedNumber.WidgetType.SLIDER) {
                                    ValidatedNumber.WidgetType.TEXTBOX
                                } else {
                                    it.type
                                })
                        } ?: ValidatedLong(input as Long)
                        colorClass -> ValidatedColor(input as Color)
                        else -> {
                            if (itemClass.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as Item, Registries.ITEM)
                            } else if (blockClass.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as Block, Registries.BLOCK)
                            } else if (entityClass.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as EntityType<*>, Registries.ENTITY_TYPE)
                            } else if (fluidClass.isAssignableFrom(jot)) {
                                ValidatedRegistryType.of(input as Fluid, Registries.FLUID)
                            } else {
                                complexStrategy(input, inputType, fieldName, annotations)
                            }
                        }
                    }
                }
            } else {
                when (val jot = inputType.jvmErasure.javaObjectType) {
                    booleanClass -> ValidatedBoolean()
                    intClass -> ValidatedInt()
                    doubleClass -> ValidatedDouble()
                    floatClass -> ValidatedFloat()
                    triStateClass -> ValidatedTriState(TriState.DEFAULT)
                    idClass -> ValidatedIdentifier()
                    stringClass -> ValidatedString()
                    longClass -> ValidatedLong()
                    shortClass -> ValidatedShort()
                    byteClass -> ValidatedByte()
                    colorClass -> ValidatedColor()
                    else -> {
                        if (itemClass.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.ITEM)
                        } else if (blockClass.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.BLOCK)
                        } else if (entityClass.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.ENTITY_TYPE)
                        } else if (fluidClass.isAssignableFrom(jot)) {
                            ValidatedRegistryType.of(Registries.FLUID)
                        } else {
                            complexStrategy(null, inputType, fieldName, annotations)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            FC.DEVLOG.error("Basic Validation Failed: ")
            decorateError(fieldName, inputType, annotations)
            FC.DEVLOG.error("   > Possible Cause: ${e.message} ")
            return null
        }
    }


    companion object {

        private fun decorateError(fieldName: String, inputType: Any, annotations: List<Annotation>) {
            FC.DEVLOG.error("   > (This error will only show inside development environments)")
            FC.DEVLOG.error("   > Field: $fieldName")
            FC.DEVLOG.error("   > Type: $inputType")
            FC.DEVLOG.error("   > Annotations: $annotations")
        }

        fun complexStrategy(input: Any?, type: KType, fieldName: String, annotations: List<Annotation>): ValidatedField<*>? {
            try {
                val clazz = type.jvmErasure
                if (clazz.javaObjectType.isEnum) {
                    return (input as? Enum<*>)?.let { ValidatedEnum(it) }
                } else if (clazz.isSubclassOf(List::class)) {
                    val argument = type.arguments[0]
                    val argumentType = argument.type ?: return null
                    val projectionValidation = basicValidationStrategy(null, argumentType, fieldName, annotations) ?: return null
                    return ValidatedList.tryMake(if (input != null) input as List<*> else emptyList(), projectionValidation)
                } else if (clazz.isSubclassOf(Set::class)) {
                    val argument = type.arguments[0]
                    val argumentType = argument.type ?: return null
                    val projectionValidation = basicValidationStrategy(null, argumentType, fieldName, annotations) ?: return null
                    return ValidatedSet.tryMake(if (input != null) input as Set<*> else setOf(), projectionValidation)
                } else if (clazz.isSubclassOf(Map::class)) {
                    val keyArgument = type.arguments[0]
                    val keyArgumentType = keyArgument.type ?: return null
                    val keyProjectionValidation = basicValidationStrategy(null, keyArgumentType, fieldName, annotations) ?: return null
                    val valueArgument = type.arguments[1]
                    val valueArgumentType = valueArgument.type ?: return null
                    val valueProjectionValidation = basicValidationStrategy(null, valueArgumentType, fieldName, annotations) ?: return null
                    return if(keyArgumentType.jvmErasure.javaObjectType.isEnum) {
                        @Suppress("UNCHECKED_CAST")
                        ValidatedEnumMap.tryMake(if (input != null) input as Map<Enum<*>, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else if (keyArgumentType.jvmErasure.javaObjectType.isInstance("")) {
                        @Suppress("UNCHECKED_CAST")
                        ValidatedStringMap.tryMake(if (input != null)input as Map<String, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else if (keyArgumentType.jvmErasure.javaObjectType.isInstance("i".fcId())) {
                        @Suppress("UNCHECKED_CAST")
                        ValidatedIdentifierMap.tryMake(if (input != null)input as Map<Identifier, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    } else {
                        ValidatedMap.tryMake(if (input != null)input as Map<*, *> else mapOf(), keyProjectionValidation, valueProjectionValidation)
                    }
                } else {
                    FC.DEVLOG.error("Setting isn't eligible for automatic validation")
                    decorateError(fieldName, type, annotations)
                    return null
                }
            } catch (e: Throwable) {
                throw ReflectiveOperationException("Error caught while performing complex validation creation", e)
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

        @JvmField
        val intClass = java.lang.Integer::class.java
        @JvmField
        val shortClass = java.lang.Short::class.java
        @JvmField
        val byteClass = java.lang.Byte::class.java
        @JvmField
        val longClass = java.lang.Long::class.java
        @JvmField
        val doubleClass = java.lang.Double::class.java
        @JvmField
        val floatClass = java.lang.Float::class.java
        @JvmField
        val booleanClass = java.lang.Boolean::class.java
        @JvmField
        val triStateClass = TriState::class.java
        @JvmField
        val colorClass = java.awt.Color::class.java
        @JvmField
        val stringClass = java.lang.String::class.java
        @JvmField
        val idClass = Identifier::class.java
        @JvmField
        val tagClass = TagKey::class.java
        @JvmField
        val itemClass = Item::class.java
        @JvmField
        val blockClass = Block::class.java
        @JvmField
        val entityClass = EntityType::class.java
        @JvmField
        val fluidClass = Fluid::class.java
    }

}