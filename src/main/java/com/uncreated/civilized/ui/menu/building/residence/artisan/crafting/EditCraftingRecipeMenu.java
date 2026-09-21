package com.uncreated.civilized.ui.menu.building.residence.artisan.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;
import com.uncreated.civilized.ui.menu.building.residence.artisan.EditRecipeMenu;
import com.uncreated.civilized.ui.menu.item.management.LegacyEyedropperSlot;
import com.uncreated.civilized.ui.menu.item.management.ReadonlySlot;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

public class EditCraftingRecipeMenu extends EditRecipeMenu<CraftingRecipe, CraftingInput> {

   public EditCraftingRecipeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraDataFromServer) {
      super(GuiRegistry.EDIT_CRAFTING_RECIPE_MENU.get(), containerId, playerInventory, extraDataFromServer);
   }

   public EditCraftingRecipeMenu(
         int containerId,
         Inventory playerInventory,
         Container craftingMenuContainer,
         Settlement settlement,
         Building building,
         int productionBillIndex,
         boolean isNewBill) {
      super(
            GuiRegistry.EDIT_CRAFTING_RECIPE_MENU.get(),
            containerId,
            playerInventory,
            craftingMenuContainer,
            settlement,
            building,
            productionBillIndex,
            isNewBill);
   }

   @Override
   protected List<LegacyEyedropperSlot> setupInputSlots(Container craftingMenuContainer) {
      List<LegacyEyedropperSlot> slots = new ArrayList<>();
      int slotIndex = 0;
      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 3; x++) {
            slots.add(new LegacyEyedropperSlot(craftingMenuContainer, slotIndex, 51 + 18 * x, 20 + 18 * y));
            slotIndex++;
         }
      }
      return slots;
   }

   @Override
   protected ReadonlySlot setupOutputSlot(Container resultContainer) {
      return new ReadonlySlot(resultContainer, 0, 141, 20 + 18);
   }

   @Override
   public ProductionType getProductionType() {
      return ProductionTypes.CRAFTING;
   }

   @Override
   protected Optional<Pair<RecipeHolder<CraftingRecipe>, CraftingInput>> getRecipeFromInputContainer(
         NonNullList<ItemStack> recipeInputItems,
         ServerLevel serverLevel,
         RecipeManager recipeManager) {
      List<ItemStack> input = recipeInputItems.subList(0, 9);

      CraftingInput craftingInput = CraftingInput.of(3, 3, input);

      Optional<RecipeHolder<CraftingRecipe>> recipe =
            recipeManager.getRecipeFor(RecipeType.CRAFTING, craftingInput, serverLevel);

      return recipe.map(craftingRecipeRecipeHolder -> Pair.of(craftingRecipeRecipeHolder, craftingInput));
   }
}
