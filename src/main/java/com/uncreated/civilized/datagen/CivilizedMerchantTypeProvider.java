package com.uncreated.civilized.datagen;

import java.util.List;

import com.uncreated.civilized.core.trading.MerchantType;
import com.uncreated.civilized.core.trading.MerchantTypes;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class CivilizedMerchantTypeProvider {

   public static void bootstrap(BootstrapContext<MerchantType> context) {
      HolderGetter<Item> itemTags = context.lookup(Registries.ITEM);

      context.register(
            MerchantTypes.key("farmer_market"),
            new MerchantType(
                  10,
                  UniformInt.of(4, 8),
                  UniformInt.of(60, 200),
                  List.of(
                        offerOneOf(items(Items.WHEAT), 5, UniformInt.of(16, 48)),
                        offerOneOf(items(Items.BREAD), 4, UniformInt.of(8, 24)),
                        offerOneOf(items(Items.CARROT, Items.POTATO, Items.BEETROOT), 5, UniformInt.of(8, 16)),
                        offerOneOf(items(Items.BAKED_POTATO), 2, UniformInt.of(4, 12)),
                        offerOneOf(items(Items.APPLE), 3, UniformInt.of(4, 16)),
                        offerOneOf(items(Items.SUGAR_CANE), 2, UniformInt.of(4, 16)),
                        offerOneOf(items(Items.EGG), 2, UniformInt.of(8, 16)))));

      context.register(
            MerchantTypes.key("trapper"),
            new MerchantType(
                  8,
                  UniformInt.of(4, 8),
                  UniformInt.of(60, 200),
                  List.of(
                        offerAllOf(items(Items.BEEF, Items.LEATHER), 4, UniformInt.of(4, 16)),
                        offerAllOf(items(Items.CHICKEN, Items.FEATHER), 4, UniformInt.of(4, 16)),
                        offerAllOf(items(Items.RABBIT, Items.RABBIT_HIDE), 4, UniformInt.of(4, 16)),
                        offerOneOf(
                              items(
                                    Items.COOKED_BEEF,
                                    Items.COOKED_PORKCHOP,
                                    Items.COOKED_CHICKEN,
                                    Items.COOKED_MUTTON),
                              3,
                              UniformInt.of(4, 12)))));

      context.register(
            MerchantTypes.key("lumber_merchant"),
            new MerchantType(
                  8,
                  UniformInt.of(4, 8),
                  UniformInt.of(80, 300),
                  List.of(
                        offerOneOf(itemTags.getOrThrow(ItemTags.LOGS), 5, UniformInt.of(16, 64)),
                        offerOneOf(itemTags.getOrThrow(ItemTags.PLANKS), 3, UniformInt.of(16, 32)),
                        offerOneOf(items(Items.STICK), 2, UniformInt.of(8, 16)),
                        offerOneOf(items(Items.CHARCOAL), 2, UniformInt.of(8, 16)))));

      context.register(
            MerchantTypes.key("mason"),
            new MerchantType(
                  6,
                  UniformInt.of(4, 8),
                  UniformInt.of(80, 300),
                  List.of(
                        offerOneOf(items(Items.COBBLESTONE), 5, UniformInt.of(16, 32)),
                        offerOneOf(items(Items.STONE), 3, UniformInt.of(16, 32)),
                        offerOneOf(items(Items.BRICKS), 2, UniformInt.of(8, 32)),
                        offerOneOf(items(Items.GLASS), 2, UniformInt.of(8, 32)))));

      context.register(
            MerchantTypes.key("bulk_goods"),
            new MerchantType(
                  6,
                  UniformInt.of(4, 10),
                  UniformInt.of(100, 350),
                  List.of(
                        offerOneOf(itemTags.getOrThrow(ItemTags.WOOL), 4, UniformInt.of(8, 32)),
                        offerOneOf(items(Items.STRING), 3, UniformInt.of(8, 32)),
                        offerOneOf(items(Items.LEATHER), 3, UniformInt.of(4, 24)),
                        offerOneOf(items(Items.SUGAR), 3, UniformInt.of(4, 32)),
                        offerOneOf(items(Items.EGG), 3, UniformInt.of(4, 20)),
                        offerOneOf(items(Items.IRON_INGOT), 3, UniformInt.of(4, 24)),
                        offerOneOf(items(Items.COAL), 3, UniformInt.of(4, 32)),
                        offerOneOf(itemTags.getOrThrow(Tags.Items.DYES), 3, UniformInt.of(4, 32)),
                        offerOneOf(items(Items.PAPER), 3, UniformInt.of(8, 24)),
                        offerOneOf(items(Items.GOLD_INGOT), 2, UniformInt.of(4, 32)),
                        offerOneOf(items(Items.BOOK), 2, UniformInt.of(1, 4)))));

      context.register(
            MerchantTypes.key("jewel_trader"),
            new MerchantType(
                  3,
                  UniformInt.of(3, 5),
                  UniformInt.of(150, 400),
                  List.of(
                        offerOneOf(items(Items.GOLD_INGOT), 2, UniformInt.of(2, 8)),
                        offerOneOf(items(Items.REDSTONE), 2, UniformInt.of(8, 24)),
                        offerOneOf(items(Items.LAPIS_LAZULI), 2, UniformInt.of(8, 24)),
                        offerOneOf(items(Items.EMERALD), 2, UniformInt.of(1, 8)),
                        offerOneOf(items(Items.GLOWSTONE), 1, UniformInt.of(1, 24)),
                        offerOneOf(items(Items.DIAMOND), 1, ConstantInt.of(1)))));
   }

   private static MerchantType.Offer offerOneOf(HolderSet<Item> items, int weight, IntProvider stock) {
      return new MerchantType.Offer(MerchantType.Offer.ItemSelector.RANDOM_ONE_OF, items, weight, stock);
   }

   private static MerchantType.Offer offerAllOf(HolderSet<Item> items, int weight, IntProvider stock) {
      return new MerchantType.Offer(MerchantType.Offer.ItemSelector.ALL_OF, items, weight, stock);
   }

   private static HolderSet<Item> items(Item... items) {
      return HolderSet.direct(Item::builtInRegistryHolder, items);
   }
}
