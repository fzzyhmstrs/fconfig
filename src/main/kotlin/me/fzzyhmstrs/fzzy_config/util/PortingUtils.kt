/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import io.netty.buffer.ByteBuf
import me.fzzyhmstrs.fzzy_config.cast
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.encoding.StringEncoding
import net.minecraft.network.encoding.VarInts
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.entry.RegistryEntryList
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.ColorHelper
import java.util.*
import java.util.function.Predicate

object PortingUtils {

    fun getWhite(alpha: Float): Int {
        return ColorHelper.Argb.fromFloats(alpha, 1f, 1f, 1f)
    }

    fun PlayerEntity.sendChat(message: Text) {
        this.sendMessage(message, false)
    }

    fun getDynamicManager(player: ServerPlayerEntity): RegistryWrapper.WrapperLookup {
        return player.server.reloadableRegistries.registryManager.cast()
    }

    fun <T> Registry<T>.optional(id: Identifier): Optional<T> {
        return this.getOrEmpty(id)
    }

    fun RegistryWrapper.WrapperLookup.anyOptional(key: RegistryKey<out Registry<*>>):  Optional<out RegistryWrapper.Impl<*>> {
        return this.getOptionalWrapper(key)
    }

    fun <T> RegistryWrapper.WrapperLookup.optional(key: RegistryKey<out Registry<T>>):  Optional<out RegistryWrapper.Impl<T>> {
        return this.getOptionalWrapper(key)
    }

    fun <T> Registry<T>.tagIdList(): List<Identifier> {
        return this.streamTags().map { it.id }.toList()
    }

    fun <T> Registry<T>.tagIdList(predicate: Predicate<Identifier>? = null): List<Identifier> {
        return if(predicate == null)
            this.streamTags().map { it.id }.toList()
        else
            this.streamTags().map { it.id }.filter(predicate).toList()
    }

    fun <T> RegistryWrapper.Impl<T>.tagIdList(predicate: Predicate<Identifier>? = null): List<Identifier> {
        return if(predicate == null)
            this.streamTags().map { it.tag.id }.toList()
        else
            this.streamTags().map { it.tag.id }.filter(predicate).toList()
    }

    fun <T> Registry<T>.namedEntryList(tagKey: TagKey<T>): Optional<RegistryEntryList.Named<T>> {
        return this.getEntryList(tagKey)
    }

    fun <T> TagKey<T>.regRef(): RegistryKey<out Registry<T>> {
        return this.registry
    }

    fun TagKey<*>.regRefId(): Identifier {
        return this.registry.value
    }

    object Codecs {

        val BOOL: PacketCodec<ByteBuf, Boolean> = object : PacketCodec<ByteBuf, Boolean> {
            override fun decode(byteBuf: ByteBuf): Boolean {
                return byteBuf.readBoolean()
            }

            override fun encode(byteBuf: ByteBuf, bl: Boolean) {
                byteBuf.writeBoolean(bl)
            }
        }

        val BYTE: PacketCodec<ByteBuf, Byte> = object : PacketCodec<ByteBuf, Byte> {
            override fun decode(byteBuf: ByteBuf): Byte {
                return byteBuf.readByte()
            }

            override fun encode(byteBuf: ByteBuf, b: Byte) {
                byteBuf.writeByte(b.toInt())
            }
        }

        val SHORT: PacketCodec<ByteBuf, Short> = object : PacketCodec<ByteBuf, Short> {
            override fun decode(byteBuf: ByteBuf): Short {
                return byteBuf.readShort()
            }

            override fun encode(byteBuf: ByteBuf, s: Short) {
                byteBuf.writeShort(s.toInt())
            }
        }

        val INT: PacketCodec<ByteBuf, Int> = object : PacketCodec<ByteBuf, Int> {
            override fun decode(byteBuf: ByteBuf): Int {
                return byteBuf.readInt()
            }

            override fun encode(byteBuf: ByteBuf, i: Int) {
                byteBuf.writeInt(i)
            }
        }

        val VAR_INT: PacketCodec<ByteBuf, Int> = object : PacketCodec<ByteBuf, Int> {
            override fun decode(byteBuf: ByteBuf): Int {
                return VarInts.read(byteBuf)
            }

            override fun encode(byteBuf: ByteBuf, i: Int) {
                VarInts.write(byteBuf, i)
            }
        }

        val LONG: PacketCodec<ByteBuf, Long> = object : PacketCodec<ByteBuf, Long> {
            override fun decode(byteBuf: ByteBuf): Long {
                return byteBuf.readLong()
            }

            override fun encode(byteBuf: ByteBuf, l: Long) {
                byteBuf.writeLong(l)
            }
        }

        val FLOAT: PacketCodec<ByteBuf, Float> = object : PacketCodec<ByteBuf, Float> {
            override fun decode(byteBuf: ByteBuf): Float {
                return byteBuf.readFloat()
            }

            override fun encode(byteBuf: ByteBuf, f: Float) {
                byteBuf.writeFloat(f)
            }
        }

        val DOUBLE: PacketCodec<ByteBuf, Double> = object : PacketCodec<ByteBuf, Double> {
            override fun decode(byteBuf: ByteBuf): Double {
                return byteBuf.readDouble()
            }

            override fun encode(byteBuf: ByteBuf, d: Double) {
                byteBuf.writeDouble(d)
            }
        }

        val STRING: PacketCodec<ByteBuf, String> = object : PacketCodec<ByteBuf, String> {
            override fun decode(byteBuf: ByteBuf): String {
                return StringEncoding.decode(byteBuf, 32767)
            }

            override fun encode(byteBuf: ByteBuf, s: String) {
                StringEncoding.encode(byteBuf, s, 32767)
            }
        }
    }

}