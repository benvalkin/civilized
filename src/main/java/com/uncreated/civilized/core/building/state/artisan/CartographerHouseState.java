package com.uncreated.civilized.core.building.state.artisan;

import java.util.Collections;
import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;

public class CartographerHouseState extends ArtisanHouseState {
   public CartographerHouseState(Building building) {
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
                     "minecraft:paper",
                     ProductionTypes.CRAFTING,
                     Collections.nCopies(3, new ItemStack(Items.SUGAR_CANE)),
                     new ItemStack(Items.PAPER)),
               createDefaultBill(
                     "minecraft:planks",
                     ProductionTypes.CRAFTING,
                     List.of(
                           new ItemStack(Items.PAPER),
                           new ItemStack(Items.PAPER),
                           new ItemStack(Items.PAPER),
                           new ItemStack(Items.PAPER),
                           new ItemStack(Items.COMPASS),
                           new ItemStack(Items.PAPER),
                           new ItemStack(Items.PAPER),
                           new ItemStack(Items.PAPER),
                           new ItemStack(Items.PAPER)),
                     new ItemStack(Items.MAP)));
      return List.of();
   }

   private static final List<Item> ALLOWED_INGREDIENT_ITEMS = List.of(Items.PAPER, Items.MAP);
   private static final List<Item> ALLOWED_OUTPUT_ITEMS = List.of(Items.PAPER, Items.MAP, Items.COMPASS);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {
      return recipeHasAtLeastOneIngredientThatIsOneOf(recipeInput, ALLOWED_INGREDIENT_ITEMS)
            || itemIsOneOf(resultItem, ALLOWED_OUTPUT_ITEMS);
   }

   @Override
   public Component getAllowedRecipeHelp() {
      return Component.translatable(
                  "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.cartographer");
   }
}
