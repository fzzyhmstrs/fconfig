package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.description
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.FcText.translation
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.FcText.appended
import me.fzzyhmstrs.fzzy_config.util.FcText.empty
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.literal
import me.fzzyhmstrs.fzzy_config.util.FcText.stringified
import me.fzzyhmstrs.fzzy_config.util.FcText.text
import me.fzzyhmstrs.fzzy_config.util.FcText.translatable
import me.fzzyhmstrs.fzzy_config.util.FcText.translatableWithFallback
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.*

object ExampleTexts{

    fun texts() {
        //FcText has wrappers for the standard Text methods, historically used for porting
        val standardText = FcText.literal("Normal text")
        val translateText = FcText.translatable("my.translatable.text")
        val fallbackText = FcText.translatableWithFallback("my.translatable.text","My Fallback")
        val stringifiedText = FcText.stringified("my.stringified.text", TagKey.of(RegistryKeys.ITEM,"arg_requiring_stringification".fcId()))
        val emptyText = FcText.empty()
        val appendedText = FcText.appended(standardText, fallbackText)

        //several extension functions for converting common MC and Java objects into text
        val idText = Identifier("stick").text()
        val uuidText = UUID.fromString("732bf411-5bb5-4f5d-8ef0-feb45d6032ee").text()
        //val dateText = TODO()
        //val messageText = TODO()
        //val chunkPosText = TODO()

        //string extension functions for simple text-ification of string literals
        val stringLit = "My Cool String".lit()
        val stringTranslate = "my.cool.string".translate() // can add args too

        // simple example class that implements Translatable
        class TranslatableExample: Translatable {
            override fun translationKey(): String{
                return "example.translatable.translation"
            }
            override fun descriptionKey(): String{
                return "example.translatable.translation.desc"
            }
        }

        //provide translations and descriptions for anything, particularly hooking into Translatable
        val myTranslatableThing = TranslatableExample()

        // translates anything, first checking if the thing is Translatable and using that translation if found, otherwise it translates the fallback key
        val anyTranslate = myTranslatableThing.translation("my.fallback.translation")
        //translate anything, first checking if the thing is Translatable and using that translation if found, otherwise it provides the fallback string literally
        val anyTransLit = myTranslatableThing.transLit("Fallback Message")

        // describes anything, first checking if the thing is Translatable and using that description if found, otherwise it translates the fallback key
        val anyDescription = myTranslatableThing.description("my.fallback.translation.desc")
        //describes anything, first checking if the thing is Translatable and using that description if found, otherwise it provides the fallback string literally
        val anyDescLit = myTranslatableThing.descLit("Fallback Description")


    }


    fun lang() {
        //fields and sections have lang keys based on their "location" in the Config class graph.
        //Lange key composition is as follows
        //1. the namespace of the config id: (my_mod)
        //2. the path of the config id: (my_mod.my_config)
        //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
        //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
        val lang = """
            {
                "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
                "my_mod.my_config.subSection.fieldName": "Very Important Setting",
                "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way.",
                
                "_comment2": "this is the lang for the corresponding subSection",
                "my_mod.my_config.subSection": "Important Settings",
                "my_mod.my_config.subSection.desc": "Important settings related to important things are in this section."
                
                "_comment2": "the lang for the base config itself",
                "my_mod.my_config": "My Mod's Config",
                "my_mod.my_config.desc": "Lots of really cool config settings live in this config."
            }
            """
    }

    fun fieldLang() {
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