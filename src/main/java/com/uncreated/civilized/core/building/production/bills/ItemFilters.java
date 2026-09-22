package com.uncreated.civilized.core.building.production.bills;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class ItemFilters {
   private final List<ItemFilter> filters;

   public ItemFilters(List<ItemFilter> filters) {
      this.filters = filters;
   }

   public static ItemFilters defaultFilters(int numberOfRecipeSlots) {
      List<ItemFilter> filters = new LinkedList<>();
      for (int i = 0; i < numberOfRecipeSlots; i++) {
         filters.add(ItemFilter.allowAllItems());
      }
      return new ItemFilters(filters);
   }

   /**
    * Gets the {@link ItemFilter} corresponding to one "recipe slot" of a recipe. For example, Crafting recipes only ever
    * one single recipe slot index (0), whereas Furnace recipe have two indices: 0 for the item to smelt, and 1 for the
    * fuel item.
    * 
    * @param recipeSlotsIndex
    *           Which "recipe slot" of a recipe. Can also be thought as a "block side" in many cases.
    */
   public ItemFilter getFilter(int recipeSlotsIndex) {
      return filters.get(recipeSlotsIndex);
   }

   public static ItemFilters fromNbt(ListTag itemFiltersTag, HolderLookup.Provider registryAccess) {
      List<ItemFilter> filters = new ArrayList<>();
      for (Tag tag : itemFiltersTag) {
         if (!(tag instanceof CompoundTag itemFilterTag))
            continue;

         ItemFilterMode itemFilterMode = ItemFilterMode.valueOf(itemFilterTag.getString("mode"));
         List<ItemStack> filterItems =
               NbtHelper.readItemList(itemFilterTag.getList("items", Tag.TAG_COMPOUND), registryAccess);
         filters.add(new ItemFilter(itemFilterMode, filterItems));
      }
      return new ItemFilters(filters);
   }

   public ListTag toNbt(HolderLookup.Provider registryAccess) {
      ListTag list = new ListTag();
      for (ItemFilter filter : filters) {
         CompoundTag itemFilterTag = new CompoundTag();
         itemFilterTag.putString("mode", filter.mode().name());
         itemFilterTag.put("items", NbtHelper.writeItemList(filter.items(), registryAccess));
         list.add(itemFilterTag);
      }
      return list;
   }

   public static final Supplier<ItemFilters> DEFAULT_CRAFTING_FILTERS = () -> ItemFilters.defaultFilters(1);
   public static final Supplier<ItemFilters> DEFAULT_COOKING_WITH_FUEL_FILTERS = () -> ItemFilters.defaultFilters(2);
}
