package com.uncreated.civilized.entity.behaviour.worker.artisan.furnace;

import com.uncreated.civilized.core.building.production.RecipeProductionSystem;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.BlastingMachine;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.CookingMachine;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;

import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;

public class BlastItems extends CookItemsWithFuel {

   public BlastItems() {
      super(WorkStates.BLASTING_ITEMS);
   }

   @Override
   protected CookingMachine getProductionMachine(RecipeProductionSystem recipeProductionSystem) {
      return recipeProductionSystem.getMachine(BlastingMachine.class);
   }

   @Override
   protected RecipeType<? extends AbstractCookingRecipe> getRecipeType() {
      return RecipeType.BLASTING;
   }

   @Override
   protected boolean isCorrectFurnaceBlock(AbstractFurnaceBlockEntity furnaceBlockEntity) {
      return furnaceBlockEntity instanceof BlastFurnaceBlockEntity;
   }
}
