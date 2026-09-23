package com.uncreated.civilized.core.building.production.bills;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

@Accessors(fluent = true)
@Getter
public class ItemFilter {
   private final ItemFilterMode mode;
   private final List<ItemStack> items;

   public ItemFilter(ItemFilterMode mode, List<ItemStack> items) {
      this.mode = mode;
      this.items = new ArrayList<>(items);
   }

   public boolean acceptsItem(ItemStack stack) {
      if (items.isEmpty()) // everything is allowed unless the filter specifies an item
         return true;

      return switch (mode) {
      case WHITELIST -> items.stream().anyMatch(i -> ItemStack.isSameItem(i, stack));
      case BLACKLIST -> items.stream().noneMatch(i -> ItemStack.isSameItem(i, stack));
      };
   }

   public static final int MAX_ITEMS = 8;

   public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilter> STREAM_CODEC =
         StreamCodec.composite(
               NeoForgeStreamCodecs.enumCodec(ItemFilterMode.class),
               ItemFilter::mode,
               ItemStack.OPTIONAL_LIST_STREAM_CODEC,
               ItemFilter::items,
               ItemFilter::new);

   public ItemFilter sanitized() {
      List<ItemStack> sanitizedItems =
            items.stream().filter(i -> !i.isEmpty()).limit(MAX_ITEMS).map(item -> item.copyWithCount(1)).toList();

      return new ItemFilter(mode, new LinkedList<>(sanitizedItems));
   }

   public static ItemFilter allowAllItems() {
      return new ItemFilter(ItemFilterMode.WHITELIST, new LinkedList<>());
   }
   public static ItemFilter defaultBurnableFuel() {
      return new ItemFilter(ItemFilterMode.WHITELIST, List.of(
              new ItemStack(Items.COAL),
              new ItemStack(Items.CHARCOAL)
      ));
   }
}
