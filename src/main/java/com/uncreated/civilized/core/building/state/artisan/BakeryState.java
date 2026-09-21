package com.uncreated.civilized.core.building.state.artisan;

import java.util.Collections;
import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.tag.CommonTags;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.common.Tags;

public class BakeryState extends ArtisanHouseState {
   public BakeryState(Building building) {
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
                     "minecraft:bread",
                     ProductionTypes.CRAFTING,
                     Collections.nCopies(3, new ItemStack(Items.WHEAT)),
                     new ItemStack(Items.BREAD)),
               createDefaultBill(
                     "minecraft:sugar",
                     ProductionTypes.CRAFTING,
                     List.of(new ItemStack(Items.SUGAR_CANE)),
                     new ItemStack(Items.SUGAR)));
      if (productionType.is(ProductionTypes.SMELTING))
         return List.of(
               createDefaultBill(
                     "minecraft:baked_potato",
                     ProductionTypes.SMELTING,
                     List.of(new ItemStack(Items.POTATO)),
                     new ItemStack(Items.BAKED_POTATO)));
      return List.of();
   }

   private static final List<TagKey<Item>> ALLOWED_CRAFTING_INGREDIENT_TAGS =
         List.of(
               Tags.Items.CROPS_WHEAT,
               Tags.Items.FOODS_BREAD,
               CommonTags.CROPS_GRAIN,
               CommonTags.FOODS_DOUGH,
               CommonTags.FOODS_DOUGH_WHEAT,
               CommonTags.FOODS_PASTA,
               CommonTags.FLOURS,
               CommonTags.FLOURS_WHEAT);

   private static final List<TagKey<Item>> ALLOWED_SMELTING_INPUT_TAGS = List.of(Tags.Items.FOODS);

   private static final List<TagKey<Item>> FORBIDDEN_SMELTING_OUTPUT_TAGS =
         List.of(Tags.Items.FOODS_RAW_MEAT, Tags.Items.FOODS_RAW_FISH);

   private static final List<Item> ALLOWED_CRAFTNG_OUTPUT_ITEMS = List.of(Items.SUGAR, Items.BAKED_POTATO);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {
      if (productionType.is(ProductionTypes.CRAFTING))
         return recipeHasAtLeastOneIngredientWithMatchingTag(recipeInput, ALLOWED_CRAFTING_INGREDIENT_TAGS)
               || itemIsOneOf(resultItem, ALLOWED_CRAFTNG_OUTPUT_ITEMS);

      if (productionType.is(ProductionTypes.SMELTING))
         return recipeHasAtLeastOneIngredientWithMatchingTag(recipeInput, ALLOWED_SMELTING_INPUT_TAGS)
               && recipeHasNoIngredientsWithMatchingTag(recipeInput, FORBIDDEN_SMELTING_OUTPUT_TAGS);

      return false;
   }

   @Override
   public Tooltip getAllowedRecipeHelpTooltip() {
      return Tooltip.create(
            Component.translatable(
                  "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.baker"));
   }
}
