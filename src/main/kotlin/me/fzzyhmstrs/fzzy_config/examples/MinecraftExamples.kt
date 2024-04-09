package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedTagKey
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier

object MinecraftExamples {

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

    // A validated Ingredient for a single item
    val validatedIngredientItem = ValidatedIngredient(Identifier("oak_log"))

    // A validated ingredient accepting a set of items
    val validatedIngredientList = ValidatedIngredient(setOf(Identifier("oak_log"),Identifier("dark_oak_log")))

    // A validated ingredient utilizing a tag
    val validatedIngredientTag = ValidatedIngredient(ItemTags.LOGS_THAT_BURN)

    //get the ingredient from the holder for use in Materials etc
    val validatedIngredientIngredient: Ingredient = validatedIngredientItem.toIngredient()

}