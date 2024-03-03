package me.fzzyhmstrs.fzzy_config.interfaces

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.NonSync
import me.fzzyhmstrs.fzzy_config.config.SyncedConfigHelperV1
import net.minecraft.network.PacketByteBuf
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Adds auto-syncing capabilities to an inheriting class, used by the SyncedConfigRegistry to automatically sync the config classes over the network to the client.
 *
 * An enclosing class that is ServerClientSynced will scrape any ServerClientSynced properties it contains and add their syncing results to itself.
 *
 * Mutable properties are also scraped and passed via string using GSON
 *
 * In general use, neither default method will need to be interacted with by the user. Fzzy Config will use them in the background to automatically perform syncing.
 */
interface ServerClientSynced{
    fun readFromServer(buf: PacketByteBuf){
        val nameMap = this.javaClass.kotlin.memberProperties.associate { it.name to it }
        val propCount = buf.readInt()
        for (i in 1..propCount){
            val name = buf.readString()
            if(name == "private_skipped") continue
            val prop = nameMap[name]?:throw IllegalStateException("PacketByteBuf reader had a problem resolving member name $name in the deserializing class ${this.javaClass.simpleName}")
            val propVal = prop.get(this)
            if (propVal is ServerClientSynced){ //ideal scenario is the properties are ValidatedFields or Sections
                propVal.readFromServer(buf)
            } else if(prop is KMutableProperty<*>){ //fallback is just gson serialization
                prop.setter.call(this, SyncedConfigHelperV1.gson.fromJson(buf.readString(),prop.returnType.javaClass))
            }
        }
    }

    fun writeToClient(buf: PacketByteBuf){
        buf.writeInt(this.javaClass.kotlin.memberProperties.size)
        for (it in this.javaClass.kotlin.memberProperties){
            if (it.visibility == KVisibility.PUBLIC) {
                val propVal = it.get(this)
                if (propVal is ServerClientSynced) {
                    val annotation = it.findAnnotation<NonSync>()
                    if (annotation != null){
                        buf.writeString("private_skipped")
                    } else {
                        buf.writeString(it.name)
                        propVal.writeToClient(buf)
                    }
                } else if (it is KMutableProperty<*> && propVal != null && it.visibility == KVisibility.PUBLIC) {
                    val annotation = it.findAnnotation<NonSync>()
                    if (annotation != null) {
                        buf.writeString("private_skipped")
                    } else {
                        try {
                            val str = SyncedConfigHelperV1.gson.toJson(propVal, it.returnType.javaClass)
                            buf.writeString(it.name)
                            buf.writeString(str)
                        } catch (e: Exception) {
                            FC.LOGGER.error(it.toString())
                            e.printStackTrace()
                            buf.writeString("private_skipped")
                        }
                    }
                } else {
                    buf.writeString("private_skipped")
                }
            } else {
                buf.writeString("private_skipped")
            }
        }
    }
}