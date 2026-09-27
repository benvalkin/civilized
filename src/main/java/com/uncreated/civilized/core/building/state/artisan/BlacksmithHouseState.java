package com.uncreated.civilized.core.building.state.artisan;

import java.util.Collections;
import java.util.List;

import com.uncreated.civilized.core.building.Building;
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
      return List.of(ProductionTypes.CRAFTING, ProductionTypes.SMELTING);
   }

   @Override
   protected List<ProductionBill> getDefaultProductionBills(ProductionType productionType) {
      if (productionType.is(ProductionTypes.CRAFTING))
         return List.of(
               createDefaultBill(
                     "minecraft:iron_ingot",
                     ProductionTypes.CRAFTING,
                     Collections.nCopies(9, new ItemStack(Items.IRON_NUGGET)),
                     new ItemStack(Items.IRON_INGOT)));
      if (productionType.is(ProductionTypes.SMELTING))
         return List.of(
               createDefaultBill(
                     "minecraft:charcoal",
                     ProductionTypes.SMELTING,
                     List.of(new ItemStack(Items.OAK_LOG)),
                     new ItemStack(Items.CHARCOAL)),
               createDefaultBill(
                     "minecraft:iron_ingot",
                     ProductionTypes.SMELTING,
                     List.of(new ItemStack(Items.IRON_ORE)),
                     new ItemStack(Items.IRON_INGOT)),
               createDefaultBill(
                     "minecraft:copper_ingot",
                     ProductionTypes.SMELTING,
                     List.of(new ItemStack(Items.COPPER_ORE)),
                     new ItemStack(Items.COPPER_INGOT)));
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
