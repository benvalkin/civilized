package com.uncreated.civilized.core.building.state.artisan;

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

public class ButcheryState extends ArtisanHouseState {
   public ButcheryState(Building building) {
      super(building);
   }

   @Override
   protected List<ProductionType> getSupportedProductionTypes() {
      return List.of(ProductionTypes.CRAFTING, ProductionTypes.SMOKING);
   }

   @Override
   protected List<ProductionBill> getDefaultProductionBills(ProductionType productionType) {
      if (productionType.is(ProductionTypes.CRAFTING))
         return List.of(
               createDefaultBill(
                     "minecraft:rabbit_stew",
                     ProductionTypes.CRAFTING,
                     List.of(
                           new ItemStack(Items.COOKED_RABBIT),
                           new ItemStack(Items.BAKED_POTATO),
                           new ItemStack(Items.CARROT),
                           new ItemStack(Items.BROWN_MUSHROOM),
                           new ItemStack(Items.BOWL)),
                     new ItemStack(Items.RABBIT_STEW)));
      if (productionType.is(ProductionTypes.SMOKING))
         return List.of(
               createDefaultBill(
                     "minecraft:cooked_beef",
                     ProductionTypes.SMOKING,
                     List.of(new ItemStack(Items.BEEF)),
                     new ItemStack(Items.COOKED_BEEF)),
               createDefaultBill(
                     "minecraft:cooked_porkchop",
                     ProductionTypes.SMOKING,
                     List.of(new ItemStack(Items.PORKCHOP)),
                     new ItemStack(Items.COOKED_PORKCHOP)));
      return List.of();
   }

   private static final List<TagKey<Item>> ALLOWED_INGREDIENT_TAGS =
         List.of(Tags.Items.FOODS_RAW_MEAT, Tags.Items.FOODS_COOKED_MEAT);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {

      if (productionType == ProductionTypes.SMOKING)
         return true;

      return recipeHasAtLeastOneIngredientWithMatchingTag(recipeInput, ALLOWED_INGREDIENT_TAGS);
   }

   @Override
   public Component getAllowedRecipeHelp() {
      return Component.translatable(
                  "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.butcher");
   }
}
