package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import java.util.Comparator;
import java.util.function.Predicate;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class BuildingStockRequirement extends ItemStockRequirement {

   @Getter
    private final ItemStack displayItem;

    public BuildingStockRequirement(String key, Predicate<ItemStack> filter, Comparator<ItemStack> preference, int minimumAcceptableAmount, int idealAmount, ItemStack displayItem) {
      super(key, filter, preference, minimumAcceptableAmount, idealAmount);
        this.displayItem = displayItem;
    }

   public BuildingStockRequirement(String key, Predicate<ItemStack> filter, int minimumAcceptableAmount, int idealAmount, ItemStack displayItem) {
      this(key, filter, ContainerHelper.NO_PREFERENCE, minimumAcceptableAmount, idealAmount, displayItem);
   }

   @Override
   public @NotNull Container getHaulInventory(CivilizedVillager villager) {
      return villager.getLogisticsInventory();
   }
}
