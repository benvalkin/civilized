package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import java.util.Comparator;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class BuildingStockRequirement extends ItemStockRequirement {

   public BuildingStockRequirement(String key, Predicate<ItemStack> filter, Comparator<ItemStack> preference, int minimumAcceptableAmount, int idealAmount) {
      super(key, filter, preference, minimumAcceptableAmount, idealAmount);
   }

   public BuildingStockRequirement(String key, Predicate<ItemStack> filter, int minimumAcceptableAmount, int idealAmount) {
      this(key, filter, ContainerHelper.NO_PREFERENCE, minimumAcceptableAmount, idealAmount);
   }

   @Override
   public @NotNull Container getHaulInventory(CivilizedVillager villager) {
      return villager.getLogisticsInventory();
   }
}
