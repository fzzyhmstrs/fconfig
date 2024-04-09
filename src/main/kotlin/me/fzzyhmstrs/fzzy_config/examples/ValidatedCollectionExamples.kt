package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.util.Identifier

object ValidatedCollectionExamples {

    fun lists() {
        //validated int list, with validation on entries restricting inputs to 1 to 16 (inclusive)
        val validatedList = ValidatedList(listOf(1,2,4,8), ValidatedInt(1..16))
    
        //wraps the vararg valued provided with a blank validated field (identifiers in this case). validation with actual bounds and logic can of course be used too
        val listFromFieldVararg = ValidatedIdentifier().toList(Identifier("stick"), Identifier("blaze_rod"))
    
        //wraps the collection provided with a blank validated field (identifiers in this case). validation with actual bounds and logic can of course be used too
        val listFromFieldCollection = ValidatedIdentifier().toList(listOf(Identifier("wooden_sword"), Identifier("stone_sword")))

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }
    
    enum class KeyEnum: EnumTranslatable {
        KEY_1,
        KEY_2,
        KEY_3;
        override fun prefix(): String{
            return "my.config.key_enum"
        }
    }

    fun sets() {
        //validated set, based on TestEnum, validation limiting to those keys
        val validatedSet = ValidatedList(listOf(KeyEnum.KEY_1), KeyEnum::class.java.validated())

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun maps() {
        //Example ValidatedMap. NOTE: this is not a ValidatedEnumMap, but that can be used too
        val validatedMap = ValidatedMap(mapOf(KeyEnum.KEY_1 to true),KeyEnum.KEY_1.validated(),ValidatedBoolean())

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun enumMaps() {
        //Example ValidatedEnumMap with basic validation providers
        val validatedEnumMap = ValidatedEnumMap(mapOf(KeyEnum.KEY_1 to true),KeyEnum.KEY_1.validated(),ValidatedBoolean())

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun identifierMaps() {
        //Example ValidatedIdentifierMap with identifiers restricted to all registered enchantments
        val validatedIdentifierMap = ValidatedIdentiferMap(mapOf(Identifier("sharpness") to true),ValidatedIdentifier.ofRegistry(Registries.ENCHANTMENT),ValidatedBoolean())

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

    fun stringMaps() {
        //Example ValidatedStringMap with basic validation
        val validatedStringMap = ValidatedStringMap(mapOf("a" to 1),ValidatedString(),ValidatedInt())

        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val fieldLang = """
        {
            "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
            "my_mod.my_config.subSection.fieldName": "Very Important Setting",
            "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
        }
        """
    }

}
