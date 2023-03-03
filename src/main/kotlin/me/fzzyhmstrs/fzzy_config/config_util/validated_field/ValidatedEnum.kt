package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import net.minecraft.network.PacketByteBuf

class ValidatedEnum<T:Enum<T>>(defaultValue: T, enum: Class<T>): ValidatedField<T>(defaultValue) {

    private val valuesMap: Map<String,T>
    init{
        val map: MutableMap<String,T> = mutableMapOf()
        enum.enumConstants?.forEach {
            map[it.name] = it
        }
        valuesMap = map
    }

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<T> {
        return try{
            val string = gson.fromJson(json, String::class.java)
            val chkEnum = valuesMap[string]
            if(chkEnum == null){
                ValidationResult.error(storedValue,"Entered value isn't a valid selection from the possible values. Possible values are: ${valuesMap.keys}")
            } else {
                ValidationResult.success(chkEnum)
            }
        } catch (e: Exception){
            ValidationResult.error(storedValue,"json [$json] at key $fieldName is not a properly formatted string")
        }
    }

    override fun serializeHeldValue(): JsonElement {
        return JsonPrimitive(storedValue.name)
    }

    override fun validateAndCorrectInputs(input: T): ValidationResult<T> {
        return ValidationResult.success(input)
    }

    override fun readmeText(): String{
        return "Choose from the following options: ${valuesMap.keys}"
    }

    override fun toBuf(buf: PacketByteBuf) {
        buf.writeString(storedValue.name)
    }

    override fun fromBuf(buf: PacketByteBuf): T {
        return valuesMap[buf.readString()]?:storedValue
    }
}