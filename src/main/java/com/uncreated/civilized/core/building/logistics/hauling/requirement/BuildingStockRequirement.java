package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import java.util.function.Predicate;

import com.uncreated.civilized.entity.CivilizedVillager;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BuildingStockRequirement extends ItemStockRequirement {

   public BuildingStockRequirement(String key, Predicate<ItemStack> test, int minimumAcceptableAmount, int idealAmount) {
      super(key, test, minimumAcceptableAmount, idealAmount);
   }

   @Override
   public @NotNull Container getHaulInventory(CivilizedVillager villager) {
      return villager.getLogisticsInventory();
   }
}
