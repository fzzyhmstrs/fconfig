/*
package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.fzzyhmstrs.fzzy_config.api.ConfigHelper
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import net.minecraft.network.PacketByteBuf

*/
/**
 * Collection of ValidatedFields for the primitive Numbers collection.
 *
 * This class is sealed as all primitive types are captured by its extensions. To validate a number, use one of the subclasses.
 *//*

sealed class ValidatedNumber<T>(private val numberClass: Class<T>, private val defaultValue: T, private val minValue: T, private val maxValue: T)  : ValidatedField<T>(defaultValue) where T: Number, T: Comparable<T>{

    init{
        if (minValue > maxValue){
            throw IllegalArgumentException("Min value [$minValue] greater than max value [$maxValue] in validated number [${this.javaClass.canonicalName}] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
        }
    }

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<T> {
        val i = try{
            ValidationResult.success(ConfigHelper.gson.fromJson(json, numberClass))
        } catch (e: Exception){
            ValidationResult.error(storedValue,"json [$json] at key [$fieldName] is not a properly formatted number")
        }
        return i
    }

    override fun serializeHeldValue(): JsonElement {
        return JsonPrimitive(storedValue)
    }

    override fun validateAndCorrectInputs(input: T): ValidationResult<T> {
        if (input < minValue) {
            val errorMessage = "Value {$input} is below the minimum bound of {$minValue}."
            return ValidationResult.error(minValue, errorMessage)
        }
        if (input > maxValue) {
            val errorMessage = "Value {$input} is above the maximum bound of {$maxValue}."
            return ValidationResult.error(maxValue, errorMessage)
        }
        return ValidationResult.success(input)
    }

    override fun readmeText(): String{
        return "Number with a default of $storedValue, a minimum of $minValue, and a maximum of $maxValue"
    }

    fun reset() {
        this.validateAndSet(defaultValue)
    }

}

*/
/**
 * A validated integer value
 *
 * Validation is performed between a min and max value (both inclusive).
 *
 * @param defaultValue Int. The default stored value.
 * @param maxValue Int. The maximum valid value this can contain.
 * @param minValue Int, optional. The minimum allowable value. If empty, this defaults to 0.
 *//*

class ValidatedInt(defaultValue: Int,maxValue: Int, minValue:Int = 0): ValidatedNumber<Int>(Int::class.java,defaultValue,minValue,maxValue){
    override fun toBuf(buf: PacketByteBuf) {
        buf.writeInt(storedValue)
    }
    override fun fromBuf(buf: PacketByteBuf): Int {
        return buf.readInt()
    }
}

*/
/**
 * A validated float value
 *
 * Validation is performed between a min and max value (both inclusive).
 *
 * @param defaultValue Float. The default stored value.
 * @param maxValue Float. The maximum valid value this can contain.
 * @param minValue Float, optional. The minimum allowable value. If empty, this defaults to 0f.
 *//*

class ValidatedFloat(defaultValue: Float,maxValue: Float, minValue:Float = 0f): ValidatedNumber<Float>(Float::class.java,defaultValue,minValue,maxValue){
    override fun toBuf(buf: PacketByteBuf) {
        buf.writeFloat(storedValue)
    }
    override fun fromBuf(buf: PacketByteBuf): Float {
        return buf.readFloat()
    }
}

*/
/**
 * A validated double value
 *
 * Validation is performed between a min and max value (both inclusive).
 *
 * @param defaultValue Double. The default stored value.
 * @param maxValue Double. The maximum valid value this can contain.
 * @param minValue Double, optional. The minimum allowable value. If empty, this defaults to 0.0.
 *//*

class ValidatedDouble(defaultValue: Double,maxValue: Double, minValue:Double = 0.0): ValidatedNumber<Double>(Double::class.java,defaultValue,minValue,maxValue){
    override fun toBuf(buf: PacketByteBuf) {
        buf.writeDouble(storedValue)
    }
    override fun fromBuf(buf: PacketByteBuf): Double {
        return buf.readDouble()
    }
}

*/
/**
 * A validated long value
 *
 * Validation is performed between a min and max value (both inclusive).
 *
 * @param defaultValue Long. The default stored value.
 * @param maxValue Long. The maximum valid value this can contain.
 * @param minValue Long, optional. The minimum allowable value. If empty, this defaults to 0L.
 *//*

class ValidatedLong(defaultValue: Long,maxValue: Long, minValue:Long = 0L): ValidatedNumber<Long>(Long::class.java,defaultValue,minValue,maxValue){
    override fun toBuf(buf: PacketByteBuf) {
        buf.writeLong(storedValue)
    }
    override fun fromBuf(buf: PacketByteBuf): Long {
        return buf.readLong()
    }
}

*/
/**
 * A validated short value
 *
 * Validation is performed between a min and max value (both inclusive).
 *
 * @param defaultValue Short. The default stored value.
 * @param maxValue Short. The maximum valid value this can contain.
 * @param minValue Short, optional. The minimum allowable value. If empty, this defaults to 0.
 *//*

class ValidatedShort(defaultValue: Short,maxValue: Short, minValue:Short = 0): ValidatedNumber<Short>(Short::class.java,defaultValue,minValue,maxValue){
    override fun toBuf(buf: PacketByteBuf) {
        buf.writeShort(storedValue.toInt())
    }
    override fun fromBuf(buf: PacketByteBuf): Short {
        return buf.readShort()
    }
}

*/
/**
 * A validated byte value
 *
 * Validation is performed between a min and max value (both inclusive).
 *
 * @param defaultValue Byte. The default stored value.
 * @param maxValue Byte. The maximum valid value this can contain.
 * @param minValue Byte, optional. The minimum allowable value. If empty, this defaults to 0.
 *//*

class ValidatedByte(defaultValue: Byte,maxValue: Byte, minValue:Byte = 0): ValidatedNumber<Byte>(Byte::class.java,defaultValue,minValue,maxValue){
    override fun toBuf(buf: PacketByteBuf) {
        buf.writeByte(storedValue.toInt())
    }
    override fun fromBuf(buf: PacketByteBuf): Byte {
        return buf.readByte()
    }
}*/