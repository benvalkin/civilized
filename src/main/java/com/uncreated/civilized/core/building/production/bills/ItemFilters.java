package com.uncreated.civilized.core.building.production.bills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class ItemFilters {

   /** Nothing has this many recipe slots; the cap is only here so that a bad packet cannot ask for a huge list. */
   private static final int MAX_FILTERS = 16;

   @Getter
   private final List<ItemFilter> filters;

   public ItemFilters(List<ItemFilter> filters) {
      this.filters = filters;
   }

   public ItemFilters(ItemFilter... filters) {
      // note: filters list should be mutable so that adding and removing items works.
      this.filters = Arrays.stream(filters).collect(Collectors.toCollection(ArrayList::new));
   }

   public static ItemFilters emptyItemFilters(int numberOfRecipeSlots) {
      List<ItemFilter> filters = new LinkedList<>();
      for (int i = 0; i < numberOfRecipeSlots; i++) {
         filters.add(ItemFilter.allowAllItems());
      }
      return new ItemFilters(filters);
   }

   /**
    * Gets the {@link ItemFilter} corresponding to one "recipe slot" of a recipe. For example, Crafting recipes only
    * ever one single recipe slot index (0), whereas Furnace recipe have two indices: 0 for the item to smelt, and 1 for
    * the fuel item.
    * 
    * @param recipeSlot
    *           Which "recipe slot" to get the corresponding {@link ItemFilter} for.
    */
   public @NotNull ItemFilter getFilter(int recipeSlot) {
      if (recipeSlot < 0 || recipeSlot >= this.filters.size())
         throw new IndexOutOfBoundsException(
               String.format(
                     "Cannot get item filter. No item filter corresponds to recipe slot index %s (%s filters)",
                     recipeSlot,
                     filters.size()));

      return filters.get(recipeSlot);
   }

   public void setFilter(int recipeSlot, ItemFilter filter) {
      filters.set(recipeSlot, filter);
   }

   public int size() {
      return filters.size();
   }

   public ItemFilters copy() {
      return new ItemFilters(new ArrayList<>(filters));
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

   public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilters> STREAM_CODEC =
         ItemFilter.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FILTERS))
               .map(filters -> new ItemFilters(new ArrayList<>(filters)), ItemFilters::getFilters);
}
