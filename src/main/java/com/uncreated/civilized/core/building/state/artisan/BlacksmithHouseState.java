package com.uncreated.civilized.core.building.state.artisan;

import java.util.Collections;
import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.ItemFilters;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.common.Tags;

public class BlacksmithHouseState extends ArtisanHouseState {
   public BlacksmithHouseState(Building building) {
      super(building);
   }

   @Override
   protected List<ProductionType> getSupportedProductionTypes() {
      return List.of(ProductionTypes.CRAFTING, ProductionTypes.BLASTING);
   }

   @Override
   protected List<ProductionBill> getDefaultProductionBills(ProductionType productionType) {
      if (productionType.is(ProductionTypes.CRAFTING))
         return List.of(
               createDefaultBill(
                     "minecraft:iron_ingot_from_nuggets",
                     ProductionTypes.CRAFTING,
                     Collections.nCopies(9, new ItemStack(Items.IRON_NUGGET)),
                     new ItemStack(Items.IRON_INGOT)));
      if (productionType.is(ProductionTypes.BLASTING))
         return List.of(
               createDefaultBill(
                     "minecraft:iron_ingot_from_blasting_raw_iron",
                     ProductionTypes.BLASTING,
                     List.of(new ItemStack(Items.RAW_IRON)),
                     new ItemStack(Items.IRON_INGOT)),
               createDefaultBill(
                     "minecraft:copper_ingot_from_blasting_raw_copper",
                     ProductionTypes.BLASTING,
                     List.of(new ItemStack(Items.RAW_COPPER)),
                     new ItemStack(Items.COPPER_INGOT)),
               createDefaultBill(
                     "minecraft:gold_ingot_from_blasting_raw_gold",
                     ProductionTypes.BLASTING,
                     List.of(new ItemStack(Items.RAW_GOLD)),
                     new ItemStack(Items.GOLD_INGOT)));
      return List.of();
   }

   private static final List<TagKey<Item>> ALLOWED_INGREDIENT_TAGS =
         List.of(Tags.Items.RAW_MATERIALS, Tags.Items.ORES, Tags.Items.INGOTS);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {
      return recipeHasAtLeastOneIngredientWithMatchingTag(recipeInput, ALLOWED_INGREDIENT_TAGS);
   }

   @Override
   public Component getAllowedRecipeHelp() {
      return Component.translatable(
            "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.blacksmith");
   }
}
