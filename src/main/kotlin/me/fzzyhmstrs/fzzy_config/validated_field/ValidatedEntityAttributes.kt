package me.fzzyhmstrs.fzzy_config.validated_field

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config_util.ConfigSection
import me.fzzyhmstrs.fzzy_config.validated_field.map.ValidatedStringDoubleMap
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.BiFunction
import java.util.function.BiPredicate

class ValidatedEntityAttributes(
    attributes: Map<EntityAttribute,Double>,
    headerText: Header = Header(),
    decorator: LineDecorating = LineDecorator.DEFAULT)
    :
    ConfigSection(headerText,decorator)
{

    private val validator: BiPredicate<String, Double> = BiPredicate{ attribute, value -> validate(attribute, value) }
    private val corrector: BiFunction<String, Double, Double> = BiFunction{ attribute, value -> correct(attribute, value) }

    var storedMap = ValidatedStringDoubleMap(initializeMap(attributes),validator,corrector,"Keys are valid Identifiers for registered EntityAttributes, and values are within the clamped Attribute bounds.")

    fun buildAttributes(builder: () -> DefaultAttributeContainer.Builder): DefaultAttributeContainer.Builder{
        val builderInstance = builder()
        for (entry in storedMap){
            val id = Identifier.tryParse(entry.key) ?: continue
            val attribute = Registries.ATTRIBUTE.get(id) ?: continue
            builderInstance.add(attribute,entry.value)
        }
        return builderInstance
    }

    private fun initializeMap(attributes: Map<EntityAttribute, Double>): Map<String, Double>{
        val map: MutableMap<String,Double> = mutableMapOf()
        for (entry in attributes){
            val attribute = entry.key
            if (!Registries.ATTRIBUTE.contains(attribute)){
                FC.LOGGER.error("Attribute $attribute is not in the ATTRIBUTE registry.")
                continue
            }
            val id = Registries.ATTRIBUTE.getId(attribute) ?: continue
            map[id.toString()] = entry.value
        }
        return map
    }

    private fun validate(attribute: String, value: Double): Boolean{
        return Identifier.tryParse(attribute) != null
    }

    private fun correct(attribute: String, value: Double): Double{
        val entityAttribute = Registries.ATTRIBUTE.get(Identifier(attribute)) ?: return value
        return entityAttribute.clamp(value)
    }
}