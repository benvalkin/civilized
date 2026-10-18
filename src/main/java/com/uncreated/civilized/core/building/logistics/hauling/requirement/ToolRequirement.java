package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import java.util.function.Predicate;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.item.ItemStack;

public class ToolRequirement extends InventoryStockRequirement {
   public ToolRequirement(String key, Predicate<ItemStack> test) {
      super(key, test, 1, 1, CivilizedVillager::getWorkInputInventory);
   }
}
