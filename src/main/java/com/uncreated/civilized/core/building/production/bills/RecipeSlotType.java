package com.uncreated.civilized.core.building.production.bills;

import java.util.function.BiPredicate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Represents a single part of a recipe. For example, crafting recipes have only one recipe slot, whereas item cooking
 * have two: one for the cooking ingredient, and one for burnable fuel.
 * 
 * @param translationKey
 *           the translation of what one would commonly refer to this slot as, e.g. "Crafting", "Fuel"
 * @param isItemAllowed
 *           whether the specified item is allowed in this recipe slot.
 */
public record RecipeSlotType(String translationKey, BiPredicate<ItemStack, Level> isItemAllowed) {

   public static final RecipeSlotType INGREDIENTS =
         new RecipeSlotType("production_bill.recipe_slot.ingredients", (stack, level) -> true);

   public static RecipeSlotType fuel(RecipeType<?> recipeType) {
      return new RecipeSlotType(
            "production_bill.recipe_slot.fuel",
            (stack, level) -> stack.getBurnTime(recipeType, level.fuelValues()) > 0);
   }

   public MutableComponent name() {
      return Component.translatable(translationKey);
   }

   public boolean isItemAllowed(ItemStack stack, Level level) {
      return isItemAllowed.test(stack, level);
   }
}
