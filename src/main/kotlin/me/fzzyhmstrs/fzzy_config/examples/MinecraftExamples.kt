/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier

object MinecraftExamples {

    fun tags() {
        //validated block tag that allows any tag in the Block Registry
        val validatedTag = ValidatedTagKey(BlockTags.ACACIA_LOGS)

        //validated Item TagKey with a predicate on the various tool types (this is optional)
        val validatedTagPredicated = ValidatedTagKey(ItemTags.AXES) { id ->
            listOf(
                ItemTags.AXES.id,
                ItemTags.SWORDS.id,
                ItemTags.SHOVELS.id,
                ItemTags.HOES.id,
                ItemTags.PICKAXES.id
            ).contains(id)
        }

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


    fun ingredients() {
        // A validated Ingredient for a single item
        val validatedIngredientItem = ValidatedIngredient(Identifier("oak_log"))

        // A validated ingredient accepting a set of items
        val validatedIngredientList = ValidatedIngredient(setOf(Identifier("oak_log"),Identifier("dark_oak_log")))

        // A validated ingredient utilizing a tag
        val validatedIngredientTag = ValidatedIngredient(ItemTags.LOGS_THAT_BURN)

        //get the ingredient from the holder for use in Materials etc
        val validatedIngredientIngredient: Ingredient = validatedIngredientItem.toIngredient()

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