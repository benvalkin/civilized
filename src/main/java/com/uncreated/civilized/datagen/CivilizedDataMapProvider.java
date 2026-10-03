package com.uncreated.civilized.datagen;

import java.util.concurrent.CompletableFuture;

import com.uncreated.civilized.core.trading.MarketValue;
import com.uncreated.civilized.neoforge.registration.DataMapRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.DataMapProvider;

public class CivilizedDataMapProvider extends DataMapProvider {

   public CivilizedDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(packOutput, lookupProvider);
   }

   @Override
   protected void gather(HolderLookup.Provider provider) {
      MarketValueBuilder values = new MarketValueBuilder(builder(DataMapRegistry.MARKET_VALUE));

      // building materials
      values.add(Items.COBBLESTONE, 1, 4);
      values.add(Items.STONE, 1, 2);
      values.add(ItemTags.LOGS, 2);
      values.add(ItemTags.PLANKS, 1, 2);
      values.add(Items.STICK, 1, 4);
      values.add(Items.GLASS, 1);
      values.add(Items.BRICKS, 1);
      values.add(ItemTags.WOOL, 1);
      values.add(Items.TORCH, 1, 2);

      // crops and food
      values.add(Items.WHEAT, 1);
      values.add(Items.BREAD, 4);
      values.add(Items.CARROT, 2, 3);
      values.add(Items.POTATO, 2, 3);
      values.add(Items.BAKED_POTATO, 2);
      values.add(Items.BEETROOT, 2, 3);
      values.add(Items.APPLE, 2);
      values.add(Items.SUGAR_CANE, 1, 2);
      values.add(Items.EGG, 1);
      values.add(Items.BEEF, 4);
      values.add(Items.COOKED_BEEF, 8);
      values.add(Items.PORKCHOP, 4);
      values.add(Items.COOKED_PORKCHOP, 8);
      values.add(Items.CHICKEN, 2);
      values.add(Items.COOKED_CHICKEN, 4);
      values.add(Items.MUTTON, 2);
      values.add(Items.MUTTON, 2);
      values.add(Items.COOKED_MUTTON, 4);
      values.add(Items.RABBIT, 2);
      values.add(Items.COOKED_RABBIT, 4);

      // animal products
      values.add(Items.LEATHER, 8);
      values.add(Items.RABBIT_HIDE, 10);
      values.add(Items.RABBIT_FOOT, 24);
      values.add(Items.STRING, 1);
      values.add(Items.FEATHER, 1);

      // crafted goods
      values.add(Items.PAPER, 1);
      values.add(Items.BOOK, 8);
      values.add(Items.SUGAR, 5);

      // fuel, ores and gems
      values.add(Items.COAL, 2);
      values.add(Items.CHARCOAL, 2);
      values.add(Items.COPPER_INGOT, 2);
      values.add(Items.IRON_INGOT, 12);
      values.add(Items.GOLD_INGOT, 24);
      values.add(Items.REDSTONE, 1);
      values.add(Items.LAPIS_LAZULI, 2);
      values.add(Items.EMERALD, 60);
      values.add(Items.DIAMOND, 160);

      values.add(Items.GLOWSTONE, 3);
      values.add(Items.NETHER_WART, 5);
      values.add(Items.BLAZE_ROD, 70);
      values.add(Items.BLAZE_POWDER, 35);
      values.add(Items.SLIME_BALL, 5);
      values.add(Items.MAGMA_CREAM, 35);
   }

   private record MarketValueBuilder(Builder<MarketValue, Item> builder) {

      void add(Item item, int value) {
         add(item, value, 1);
      }

      void add(Item item, int value, int per) {
         builder.add(item.builtInRegistryHolder(), MarketValue.of(value, per), false);
      }

      void add(TagKey<Item> tag, int value) {
         add(tag, value, 1);
      }

      void add(TagKey<Item> tag, int value, int per) {
         builder.add(tag, MarketValue.of(value, per), false);
      }
   }
}

//// building materials
// values.add(Items.COBBLESTONE, 1, 16);
// values.add(Items.STONE, 1, 8);
// values.add(ItemTags.LOGS, 1, 2);
// values.add(ItemTags.PLANKS, 1, 8);
// values.add(Items.STICK, 1, 16);
// values.add(Items.GLASS, 1, 4);
// values.add(Items.BRICKS, 1, 4);
// values.add(ItemTags.WOOL, 1, 2);
// values.add(Items.TORCH, 1, 8);
//
//// crops and food
// values.add(Items.WHEAT, 1, 4);
// values.add(Items.BREAD, 1);
// values.add(Items.CARROT, 1, 6);
// values.add(Items.POTATO, 1, 6);
// values.add(Items.BAKED_POTATO, 1, 2);
// values.add(Items.BEETROOT, 1, 6);
// values.add(Items.APPLE, 1, 2);
// values.add(Items.SUGAR_CANE, 1, 8);
// values.add(Items.EGG, 1, 4);
// values.add(Items.BEEF, 1);
// values.add(Items.COOKED_BEEF, 2);
// values.add(Items.PORKCHOP, 1);
// values.add(Items.COOKED_PORKCHOP, 2);
// values.add(Items.CHICKEN, 1, 2);
// values.add(Items.COOKED_CHICKEN, 1);
// values.add(Items.MUTTON, 1, 2);
// values.add(Items.COOKED_MUTTON, 1);
//
//// animal products
// values.add(Items.LEATHER, 2);
// values.add(Items.STRING, 1, 4);
// values.add(Items.FEATHER, 1, 4);
//
//// crafted goods
// values.add(Items.PAPER, 1, 4);
// values.add(Items.BOOK, 2);
//
//// fuel, ores and gems
// values.add(Items.COAL, 1, 2);
// values.add(Items.CHARCOAL, 1, 2);
// values.add(Items.COPPER_INGOT, 1, 2);
// values.add(Items.IRON_INGOT, 3);
// values.add(Items.GOLD_INGOT, 6);
// values.add(Items.REDSTONE, 1, 4);
// values.add(Items.LAPIS_LAZULI, 1, 2);
// values.add(Items.EMERALD, 15);
// values.add(Items.DIAMOND, 40);
