package com.uncreated.civilized.core.trading;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record MerchantType(int weight, IntProvider tradeCount, IntProvider startingCurrency,
      List<MerchantType.Offer> offers) {

   private static final Logger LOGGER = LogUtils.getLogger();

   public static final Codec<MerchantType> CODEC =
         RecordCodecBuilder.create(
               instance -> instance
                     .group(
                           ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(MerchantType::weight),
                           IntProvider.POSITIVE_CODEC.fieldOf("trade_count").forGetter(MerchantType::tradeCount),
                           IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("starting_currency", ConstantInt.ZERO)
                                 .forGetter(MerchantType::startingCurrency),
                           Offer.CODEC.listOf().fieldOf("offers").forGetter(MerchantType::offers))
                     .apply(instance, MerchantType::new));

   public record Offer(ItemSelector itemSelector, HolderSet<Item> items, int weight,
         IntProvider stock) implements WeightedEntry {

      public enum ItemSelector implements StringRepresentable {
         /**
          * Every item in the list is always offered. Useful when multiple types of items are intended to always be sold
          * together, e.g. a bow with arrows
          */
         ALL_OF("all_of"),
         /**
          * A single random item in the list is offered. Useful when a single offer can take on different items, e.g. a
          * single Wool trade that can be of a random color.
          */
         RANDOM_ONE_OF("random_one_of");

         public static final Codec<ItemSelector> CODEC = StringRepresentable.fromEnum(ItemSelector::values);

         private final String serializedName;

         ItemSelector(String serializedName) {
            this.serializedName = serializedName;
         }

         @Override
         public String getSerializedName() {
            return serializedName;
         }
      }

      public static final Codec<Offer> CODEC =
            RecordCodecBuilder.create(
                  instance -> instance
                        .group(
                              ItemSelector.CODEC.optionalFieldOf("select", ItemSelector.RANDOM_ONE_OF)
                                    .forGetter(Offer::itemSelector),
                              RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(Offer::items),
                              ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Offer::weight),
                              IntProvider.POSITIVE_CODEC.fieldOf("stock").forGetter(Offer::stock))
                        .apply(instance, Offer::new));

      @Override
      public Weight getWeight() {
         return Weight.of(weight);
      }

      public List<TradeItem> roll(RandomSource random) {
         // only items with a market value can be traded. Items in a tag that don't have one are left out.
         int numberOfItems = items.size();
         List<ItemStack> tradeable = items.stream().map(ItemStack::new).filter(MarketValues::isTradeable).toList();

         if (tradeable.size() != numberOfItems) {
            List<ItemStack> untradeable =
                  items.stream().map(ItemStack::new).filter(i -> !MarketValues.isTradeable(i)).toList();
            LOGGER.warn(
                  "Merchant offer for {} has {} items without a market value. These items will not be included in the offer. The problematic items are: {}",
                  items,
                  untradeable.size(),
                  untradeable);
         }

         if (tradeable.isEmpty()) {
            LOGGER.warn("Merchant offer for {} has no items with a market value, so it was skipped", items);
            return List.of();
         }

         if (itemSelector == ItemSelector.ALL_OF) {
            return tradeable.stream().map(i -> tradeItem(random, i)).toList();
         } else if (itemSelector == ItemSelector.RANDOM_ONE_OF) {
            ItemStack item = tradeable.get(random.nextInt(tradeable.size()));
            return List.of(tradeItem(random, item));
         }
         throw new IllegalStateException();
      }

      private TradeItem tradeItem(RandomSource random, ItemStack item) {
         MarketValue value = MarketValues.get(item).orElseThrow();
         int lots = stock.sample(random);
         return new TradeItem(item, value.value(), lots * value.per(), value.per());
      }
   }

   public List<TradeItem> rollTrades(RandomSource random) {
      int count = tradeCount.sample(random);
      List<Offer> remaining = new ArrayList<>(offers);
      List<TradeItem> trades = new ArrayList<>();

      // offers that can't be traded don't count towards the trade count, so others are picked in their place
      while (trades.size() < count && !remaining.isEmpty()) {
         Offer offer = WeightedRandom.getRandomItem(random, remaining).orElseThrow();
         remaining.remove(offer);

         List<TradeItem> roll = offer.roll(random);
         trades.addAll(roll);

         // uncomment the following if we want explicitly only allow up to the desired umber of trades.
         // without this, traders may sometimes have a few more trades than the max number of items.
         // for (TradeItem tradeItem : roll) {
         // trades.add(tradeItem);
         // if (trades.size() >= count)
         // break;
         // }
      }

      return trades;
   }
}
