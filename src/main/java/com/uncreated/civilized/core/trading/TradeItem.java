package com.uncreated.civilized.core.trading;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
public class TradeItem {

   public static final StreamCodec<RegistryFriendlyByteBuf, TradeItem> STREAM_CODEC =
         StreamCodec.composite(
               ItemStack.STREAM_CODEC,
               TradeItem::item,
               ByteBufCodecs.VAR_INT,
               TradeItem::price,
               ByteBufCodecs.VAR_INT,
               TradeItem::stock,
               ByteBufCodecs.VAR_INT,
               TradeItem::quantityPerTrade,
               TradeItem::new);

   public static final Codec<TradeItem> CODEC =
         RecordCodecBuilder.create(
               instance -> instance
                     .group(
                           ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(TradeItem::item),
                           Codec.INT.fieldOf("price").forGetter(TradeItem::price),
                           Codec.INT.fieldOf("stock").forGetter(TradeItem::stock),
                           ExtraCodecs.POSITIVE_INT.fieldOf("quantity_per_trade")
                                 .forGetter(TradeItem::quantityPerTrade))
                     .apply(instance, TradeItem::new));

   @Getter
   private final ItemStack item;
   @Getter(AccessLevel.PRIVATE)
   private final int price;

   public int sellPrice() {
      return price;
   }

   public int buyPrice() {
      return Math.round(price * MarketValue.BUY_PRICE_MULTIPLIER);
   }

   @Getter
   @Setter
   private int stock;
   @Getter
   private final int quantityPerTrade;

   /**
    * @param price
    *           what one trade costs
    * @param stock
    *           how many of the item the vendor has, in items rather than trades
    * @param quantityPerTrade
    *           how many items change hands for the price. Trades only ever happen in whole lots of this many
    */
   public TradeItem(ItemStack item, int price, int stock, int quantityPerTrade) {
      if (quantityPerTrade < 1)
         throw new IllegalArgumentException("A trade has to be for at least one item, not " + quantityPerTrade);

      this.item = item.copyWithCount(1);
      this.price = price;
      this.stock = stock;
      this.quantityPerTrade = quantityPerTrade;
   }

   public TradeItem(ItemStack item, int price, int stock) {
      this(item, price, stock, 1);
   }

   /** How many whole trades this many items make up. Whatever is left over isn't part of any trade. */
   public int wholeTrades(int quantity) {
      return Math.max(0, quantity) / quantityPerTrade;
   }

   /**
    * @param maxDesiredQuantity
    *           the most items the buyer wants, which is rounded down to whole trades
    */
   public ItemTraded buyFromVendor(int maxDesiredQuantity, Container buyer, int vendorAvailableCurrency) {
      // the vendor can only sell whole trades of what it has in stock
      int trades = wholeTrades(Math.min(maxDesiredQuantity, stock));
      if (trades <= 0)
         return new ItemTraded(ItemStack.EMPTY, vendorAvailableCurrency, 0);

      int quantity = trades * quantityPerTrade;
      int valueOfTrades = trades * buyPrice();
      stock -= quantity;
      vendorAvailableCurrency += CurrencyItem.debit(List.of(buyer), valueOfTrades);

      return new ItemTraded(item.copyWithCount(quantity), vendorAvailableCurrency, 0);
   }

   /**
    * Sells as many whole trades of the stack as the vendor can afford. Whatever isn't sold, including any items that
    * don't make up a whole trade, is handed back.
    */
   public ItemTraded sellToVendor(ItemStack itemStack, Container seller, int vendorAvailableCurrency) {

      TradeQuote quote = adjustIfOddOrNotAffordable(vendorAvailableCurrency, TradeDirection.SELL, itemStack.getCount());
      int quantitySold = quote.quanity();
      if (quantitySold <= 0)
         return new ItemTraded(itemStack, vendorAvailableCurrency, 0);

      stock += quantitySold;
      vendorAvailableCurrency -= quote.totalCurrencyCost();
      int valueOfCoinsFailedToTransfer = CurrencyItem.credit(List.of(seller), quote.totalCurrencyCost());

      ItemStack remainderNotSold = itemStack.copyWithCount(itemStack.getCount() - quantitySold);
      return new ItemTraded(remainderNotSold, vendorAvailableCurrency, valueOfCoinsFailedToTransfer);
   }

   public TradeQuote adjustIfOddOrNotAffordable(Container buyer, TradeDirection direction, int quantity) {
      return adjustIfOddOrNotAffordable(countCurrency(buyer), direction, quantity);
   }

   public TradeQuote adjustIfOddOrNotAffordable(int availableCurrency, TradeDirection direction, int quantity) {
      int requestedTrades = wholeTrades(quantity);
      if (requestedTrades <= 0)
         return new TradeQuote(0, 0, true);

      int price = direction == TradeDirection.BUY ? buyPrice() : sellPrice();

      // free trades are always affordable (would otherwise divide by zero)
      int affordableTrades;
      if (price <= 0)
         affordableTrades = requestedTrades;
      else
         affordableTrades = Math.clamp(availableCurrency / price, 0, requestedTrades);

      return new TradeQuote(
            affordableTrades * quantityPerTrade,
            affordableTrades * price,
            affordableTrades < requestedTrades);
   }

   private static int countCurrency(Container container) {
      AggregateItemStack coins = ContainerHelper.countItems(container, i -> i.getItem() instanceof CurrencyItem);
      return CurrencyItem.countCoins(coins.getItemStacks());
   }
}
