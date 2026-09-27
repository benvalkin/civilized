package com.uncreated.civilized.core.building.state.artisan;

import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.common.Tags;

public class ArtistHouseState extends ArtisanHouseState {
   public ArtistHouseState(Building building) {
      super(building);
   }

   @Override
   protected List<ProductionType> getSupportedProductionTypes() {
      return List.of(ProductionTypes.CRAFTING);
   }

   @Override
   protected List<ProductionBill> getDefaultProductionBills(ProductionType productionType) {
      if (productionType.is(ProductionTypes.CRAFTING))
         return List.of(
               createDefaultBill(
                     "minecraft:painting",
                     ProductionTypes.CRAFTING,
                     List.of(
                           new ItemStack(Items.STICK),
                           new ItemStack(Items.STICK),
                           new ItemStack(Items.STICK),
                           new ItemStack(Items.STICK),
                           new ItemStack(Items.WHITE_WOOL),
                           new ItemStack(Items.STICK),
                           new ItemStack(Items.STICK),
                           new ItemStack(Items.STICK),
                           new ItemStack(Items.STICK)),
                     new ItemStack(Items.PAINTING)),
               createDefaultBill(
                     "minecraft:red_wool",
                     ProductionTypes.CRAFTING,
                     List.of(new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.RED_DYE)),
                     new ItemStack(Items.RED_WOOL)));
      return List.of();
   }

   private static final List<TagKey<Item>> ALLOWED_INGREDIENTS_TAGS =
         List.of(Tags.Items.DYES, ItemTags.DECORATED_POT_INGREDIENTS);
   private static final List<TagKey<Item>> ALLOWED_OUTPUT_TAGS = List.of(Tags.Items.DYED, ItemTags.CANDLES);
   private static final List<Item> ALLOWED_RESULT_ITEMS = List.of(Items.PAINTING, Items.FLOWER_POT);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {
      return recipeHasAtLeastOneIngredientWithMatchingTag(recipeInput, ALLOWED_INGREDIENTS_TAGS)
            || itemHasMatchingTag(resultItem, ALLOWED_OUTPUT_TAGS) || itemIsOneOf(resultItem, ALLOWED_RESULT_ITEMS);
   }

   @Override
   public Component getAllowedRecipeHelp() {
      return Component.translatable(
                  "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.artist_house");
   }
}
