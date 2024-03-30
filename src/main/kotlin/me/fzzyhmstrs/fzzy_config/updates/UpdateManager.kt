package me.fzzyhmstrs.fzzy_config.updates

import com.google.common.collect.ArrayListMultimap
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.EntrySerializer
import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.*
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.awt.Color
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class UpdateManager {
    companion object {
        internal val INSTANCE  = UpdateManager()
    }

    // Important Base Concept: SCOPE
    // basically a string mapping of the "location" of an element in a config layout, not disimilar to a file path
    //
    // Top level
    //   The namespace of the mod adding configs. The namespace of config.getId()
    //   ex. 'mymod'
    //
    // Config
    //   Next level is the config name, the path of the getId()
    //   ex. 'items'
    //
    // Subsection
    //   sections add a layer to the scope. stacks.
    //   ex. 'dropRates'
    //
    // Element
    //   finally the element terminates the scope
    //   ex. 'oceanChests'
    //
    // Built
    //   scopes are built into translation-key-like strings
    //   ex. 'mymod.items.dropRates.oceanChests'

    private val updateMap: MutableMap<String, Updatable> = mutableMapOf()
    private val changeHistory: MutableMap<String, ArrayListMultimap<Long, Text>> = mutableMapOf()

    fun flush(): List<String> {
        updateMap.clear()
        val updates = buildChangeHistoryLog()
        changeHistory.clear()
        return updates
    }

    fun buildChangeHistoryLog(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        val list: MutableList<String> = mutableListOf()
        for ((scope, updateLog) in changeHistory){
            for ((time, updates) in updateLog.entries()){
                list.add("Updated scope [$scope] at [${formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()))}]: [${updates.string}]")
            }
        }
        return list
    }

    internal fun update(updatable: Updatable, updateMessage: Text) {
        updateMap.computeIfAbsent(updatable.getEntryKey()) { updatable }
        addUpdateMessage(updatable.getEntryKey(),updateMessage)
    }

    internal fun hasUpdate(scope: String): Boolean{
        return updateMap[scope]?.popState() ?: false
    }

    fun changes(): Int {
        return updateMap.filter { it.value.peekState() }.size
    }

    fun revertAll() {
        for (update in updateMap.values){
            update.revert()
        }
    }

    fun restoreAll() {
        for (update in updateMap.values){
            update.restore()
        }
    }

    fun addUpdateMessage(key: String,text: Text) {
        changeHistory.computeIfAbsent(key){ArrayListMultimap.create()}.put(System.currentTimeMillis(),text)
    }

    fun<T: Config> applyKeys(config: T) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {_, str, v, _, _ -> if (v is EntryKeyed) v.setEntryKey(str)}
    }

    fun<T: Config> pushStates(config: T) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {_, _, v, _, _ -> if (v is Updatable) v.pushState()}
    }

    fun<T: Config> getSyncUpdates(config: T, ignoreNonSync: Boolean = false): Map<String, EntrySerializer<*>> {
        val map: MutableMap<String, EntrySerializer<*>> = mutableMapOf()
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(), ignoreNonSync) {_, str, v, _, _ -> if (v is Updatable && v is EntrySerializer<*>) { if (v.popState()) map[str] = v }}
        return map
    }

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
                return null
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
    /*internal fun<T: Config> getSyncUpdates(config: T, ignoreNonSync: Boolean = false): Map<String, EntrySerializer<*>> {
        val map: MutableMap<String, EntrySerializer<*>> = mutableMapOf()
        for ((updateScope, update) in getScopedUpdates(config.getId().toTranslationKey())){
            if (update.popState()){
                if (update is EntrySerializer<*>)
                   map[updateScope] = update
            }
        }
        return map
    }*/
}