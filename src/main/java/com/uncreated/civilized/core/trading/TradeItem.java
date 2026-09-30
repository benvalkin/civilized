package com.uncreated.civilized.core.trading;

import java.util.List;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
@Getter
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

   private final ItemStack item;
   private final int price;
   @Setter
   private int stock;
   private final int quantityPerTrade;

   public TradeItem(ItemStack item, int price, int stock, int quantityPerTrade) {
      this.item = item.copyWithCount(1);
      this.price = price;
      this.stock = stock;
      this.quantityPerTrade = quantityPerTrade;
   }

   public TradeItem(ItemStack item, int price, int stock) {
      this(item, price, stock, 1);
   }

   public ItemTraded buyFromVendor(int quanity, /* Container vendor, */Container buyer, int vendorAvailableCurrency) {

      if (stock <= 0)
         return new ItemTraded(ItemStack.EMPTY, vendorAvailableCurrency, 0);

      if (quanity > stock)
         quanity = stock;

      stock -= quanity;
      int valueOfActuallySold = quanity * price;
      int actualAmountDebited = CurrencyItem.debit(List.of(buyer), valueOfActuallySold);
      vendorAvailableCurrency += actualAmountDebited;

      // note: if actuallyTaken somehow contains items of different types, they will be converted to this trading item,
      // and their original stack will be lost.
      // this makes sense and shouldn't ever occur - just use with caution
      return new ItemTraded(item.copyWithCount(quanity), vendorAvailableCurrency, 0);
   }

   public ItemTraded sellToVendor(
         ItemStack itemStack,
         /* Container vendor, */Container seller,
         int vendorAvailableCurrency) {

      if (itemStack.getCount() <= 0)
         return new ItemTraded(itemStack, vendorAvailableCurrency, 0);

      int quantity = itemStack.getCount();

      TradeQuote quote = adjustIfNotAffordable(vendorAvailableCurrency, quantity);

      int quantityActuallySold = quote.quanity();
      stock += quantityActuallySold;
      int valueOfActuallySold = quantityActuallySold * price;

      vendorAvailableCurrency -= valueOfActuallySold;
      int valueOfCoinsFailedToTransfer = CurrencyItem.credit(List.of(seller), valueOfActuallySold);

      ItemStack remainderNotSold = itemStack.copyWithCount(quantity - quantityActuallySold);

      return new ItemTraded(remainderNotSold, vendorAvailableCurrency, valueOfCoinsFailedToTransfer);
   }

   public boolean canAfford(Container buyer, int quantity) {
      AggregateItemStack coins = ContainerHelper.countItems(buyer, i -> i.getItem() instanceof CurrencyItem);
      int requiredTotalCurrency = quantity * price;
      int availableCurrency = 0;
      for (ItemStack coinStack : coins.getItemStacks()) {
         availableCurrency += coinStack.getCount() * ((CurrencyItem) coinStack.getItem()).getUnitValue();
      }
      return availableCurrency >= requiredTotalCurrency;
   }

   public TradeQuote adjustIfNotAffordable(Container buyer, int quantity) {
      AggregateItemStack coins = ContainerHelper.countItems(buyer, i -> i.getItem() instanceof CurrencyItem);
      int availableCurrency = 0;
      for (ItemStack coinStack : coins.getItemStacks()) {
         availableCurrency += coinStack.getCount() * ((CurrencyItem) coinStack.getItem()).getUnitValue();
      }
      return adjustIfNotAffordable(availableCurrency, quantity);
   }

   public TradeQuote adjustIfNotAffordable(int availableCurrency, int quantity) {
      if (quantity <= 0 || availableCurrency <= 0)
         return new TradeQuote(0, 0, true);

      int requiredTotalCurrency = quantity * price;
      if (availableCurrency >= requiredTotalCurrency)
         return new TradeQuote(quantity, requiredTotalCurrency, false);

      int adjustedQuantity = availableCurrency / price;
      int adjustedRequiredTotalCurrency = adjustedQuantity * price;
      return new TradeQuote(adjustedQuantity, adjustedRequiredTotalCurrency, true);
   }
}
