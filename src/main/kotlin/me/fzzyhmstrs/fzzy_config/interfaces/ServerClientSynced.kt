package me.fzzyhmstrs.fzzy_config.interfaces

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import net.minecraft.network.PacketByteBuf
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

interface ServerClientSynced{
    fun readFromServer(buf: PacketByteBuf){
        val nameMap = this.javaClass.kotlin.declaredMemberProperties.associate { it.name to it }
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
        buf.writeInt(this.javaClass.kotlin.declaredMemberProperties.size)
        for (it in this.javaClass.kotlin.declaredMemberProperties){
            if (it.visibility == KVisibility.PUBLIC) {
                val propVal = it.get(this)
                if (propVal is ServerClientSynced) {
                    buf.writeString(it.name)
                    propVal.writeToClient(buf)
                } else if (it is KMutableProperty<*> && propVal != null) {
                    buf.writeString(it.name)
                    buf.writeString(SyncedConfigHelperV1.gson.toJson(propVal, it.returnType.javaClass))
                } else {
                    buf.writeString("private_skipped")
                }
            } else {
                buf.writeString("private_skipped")
            }
        }
    }
}
