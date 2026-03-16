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
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.Utf8String
import net.minecraft.network.VarInt
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.HolderLookup
import net.minecraft.core.HolderSet
import net.minecraft.tags.TagKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import java.util.*
import java.util.function.Predicate

object PortingUtils {

    fun getWhite(alpha: Float): Int {
        return ARGB.white(alpha)
    }

    fun fullAlpha(color: Int): Int {
        return ARGB.opaque(color)
    }

    fun Player.sendChat(message: Component) {
        this.sendSystemMessage(message)
    }

    fun getDynamicManager(player: ServerPlayer): HolderLookup.Provider {
        return player.level().server.reloadableRegistries().lookup().cast()
    }

    fun <T: Any> Registry<T>.optional(id: Identifier): Optional<T> {
        return this.getOptional(id)
    }

    fun HolderLookup.Provider.anyOptional(key: ResourceKey<out Registry<*>>):  Optional<out HolderLookup.RegistryLookup<*>> {
        return this.lookup(key)
    }

    fun <T: Any> HolderLookup.Provider.optional(key: ResourceKey<out Registry<T>>):  Optional<out HolderLookup.RegistryLookup<T>> {
        return this.lookup(key)
    }

    fun <T: Any> Registry<T>.tagIdList(): List<Identifier> {
        return this.listTags().map { it.key().location }.toList()
    }

    fun <T: Any> Registry<T>.tagIdList(predicate: Predicate<Identifier>? = null): List<Identifier> {
        return if(predicate == null)
            this.listTags().map { it.key().location }.toList()
        else
            this.listTags().map { it.key().location }.filter(predicate).toList()
    }

    fun <T: Any> HolderLookup.RegistryLookup<T>.tagIdList(predicate: Predicate<Identifier>? = null): List<Identifier> {
        return if(predicate == null)
            this.listTags().map { it.key().location }.toList()
        else
            this.listTags().map { it.key().location }.filter(predicate).toList()
    }

    fun <T: Any> Registry<T>.namedEntryList(tagKey: TagKey<T>): Optional<HolderSet.Named<T>> {
        return this.get(tagKey)
    }

    fun <T: Any> TagKey<T>.regRef(): ResourceKey<out Registry<T>> {
        return this.registry
    }

    fun TagKey<*>.regRefId(): Identifier {
        return this.registry.identifier()
    }

    fun emptyIngredient(id: String = ""): Ingredient {
        throw UnsupportedOperationException("Ingredients can't be empty; item ID [$id] not found in the Items Registry.")
    }

    fun itemIngredient(item: ItemLike): Ingredient {
        return Ingredient.of(item)
    }

    fun listIngredient(stacks: List<ItemLike>): Ingredient {
        return Ingredient.of(stacks.stream())
    }

    fun tagIngredient(tag: TagKey<Item>): Ingredient {
        return Ingredient.of(BuiltInRegistries.ITEM.namedEntryList(tag).orElseThrow { UnsupportedOperationException("Ingredients can't be empty; tag [$tag] wasn't found in the Items registry") })
    }

    fun isAltDown(): Boolean {
        return Minecraft.getInstance().hasAltDown()
    }

    fun isShiftDown(): Boolean {
        return Minecraft.getInstance().hasShiftDown()
    }

    fun isControlDown(): Boolean {
        return Minecraft.getInstance().hasControlDown()
    }

    object Codecs {

        val BOOL: StreamCodec<ByteBuf, Boolean> = object : StreamCodec<ByteBuf, Boolean> {
            override fun decode(byteBuf: ByteBuf): Boolean {
                return byteBuf.readBoolean()
            }

            override fun encode(byteBuf: ByteBuf, bl: Boolean) {
                byteBuf.writeBoolean(bl)
            }
        }

        val BYTE: StreamCodec<ByteBuf, Byte> = object : StreamCodec<ByteBuf, Byte> {
            override fun decode(byteBuf: ByteBuf): Byte {
                return byteBuf.readByte()
            }

            override fun encode(byteBuf: ByteBuf, b: Byte) {
                byteBuf.writeByte(b.toInt())
            }
        }

        val SHORT: StreamCodec<ByteBuf, Short> = object : StreamCodec<ByteBuf, Short> {
            override fun decode(byteBuf: ByteBuf): Short {
                return byteBuf.readShort()
            }

            override fun encode(byteBuf: ByteBuf, s: Short) {
                byteBuf.writeShort(s.toInt())
            }
        }

        val INT: StreamCodec<ByteBuf, Int> = object : StreamCodec<ByteBuf, Int> {
            override fun decode(byteBuf: ByteBuf): Int {
                return byteBuf.readInt()
            }

            override fun encode(byteBuf: ByteBuf, i: Int) {
                byteBuf.writeInt(i)
            }
        }

        val VAR_INT: StreamCodec<ByteBuf, Int> = object : StreamCodec<ByteBuf, Int> {
            override fun decode(byteBuf: ByteBuf): Int {
                return VarInt.read(byteBuf)
            }

            override fun encode(byteBuf: ByteBuf, i: Int) {
                VarInt.write(byteBuf, i)
            }
        }

        val LONG: StreamCodec<ByteBuf, Long> = object : StreamCodec<ByteBuf, Long> {
            override fun decode(byteBuf: ByteBuf): Long {
                return byteBuf.readLong()
            }

            override fun encode(byteBuf: ByteBuf, l: Long) {
                byteBuf.writeLong(l)
            }
        }

        val FLOAT: StreamCodec<ByteBuf, Float> = object : StreamCodec<ByteBuf, Float> {
            override fun decode(byteBuf: ByteBuf): Float {
                return byteBuf.readFloat()
            }

            override fun encode(byteBuf: ByteBuf, f: Float) {
                byteBuf.writeFloat(f)
            }
        }

        val DOUBLE: StreamCodec<ByteBuf, Double> = object : StreamCodec<ByteBuf, Double> {
            override fun decode(byteBuf: ByteBuf): Double {
                return byteBuf.readDouble()
            }

            override fun encode(byteBuf: ByteBuf, d: Double) {
                byteBuf.writeDouble(d)
            }
        }

        val STRING: StreamCodec<ByteBuf, String> = object : StreamCodec<ByteBuf, String> {
            override fun decode(byteBuf: ByteBuf): String {
                return Utf8String.read(byteBuf, 32767)
            }

            override fun encode(byteBuf: ByteBuf, s: String) {
                Utf8String.write(byteBuf, s, 32767)
            }
        }
    }

}