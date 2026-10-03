package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.core.trading.CurrencyStock;
import com.uncreated.civilized.core.trading.ItemTraded;
import com.uncreated.civilized.core.trading.TradeDirection;
import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.core.trading.TradeQuote;
import com.uncreated.civilized.ui.menu.trading.TradingMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuyItem(int sequence, int slot, int quantity) implements CustomPacketPayload {

   public static final Type<BuyItem> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "buy_item"));

   public static final StreamCodec<RegistryFriendlyByteBuf, BuyItem> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               BuyItem::sequence,
               ByteBufCodecs.VAR_INT,
               BuyItem::slot,
               ByteBufCodecs.VAR_INT,
               BuyItem::quantity,
               BuyItem::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveBuyItem(BuyItem packet, IPayloadContext context) {

      if (!(context.player().containerMenu instanceof TradingMenu tradingMenu))
         return;

      TradeItem tradeItem = tradingMenu.lookUpSlot(packet.slot);
      if (tradeItem == null)
         return;

      if (tradeItem.item().isEmpty())
         return;

      if (packet.quantity <= 0)
         return; // vibe check client

      ItemStack carried = tradingMenu.getCarried();
      // if you are already holding something, buying more of the same item is supported
      if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, tradeItem.item())) {
         return;
      }

      int remainingCarriedStackCapacity = tradeItem.item().getMaxStackSize() - carried.getCount();
      if (remainingCarriedStackCapacity <= 0)
         return; // cannot buy more - the stack you are holding is full

      // ensure that your 'carried' item stack actually has space
      int quantity = Math.min(packet.quantity, remainingCarriedStackCapacity);

      Container buyer = tradingMenu.getCustomer();

      TradeQuote tradeQuote = tradeItem.adjustIfOddOrNotAffordable(buyer, TradeDirection.BUY, quantity);
      CurrencyStock vendorCurrency = tradingMenu.getVendorCurrency();
      ItemTraded bought = tradeItem.buyFromVendor(tradeQuote.quanity(), buyer, vendorCurrency.availableCurrency());

      vendorCurrency.availableCurrency(bought.newVendorAvailableCurrency());
      if (!carried.isEmpty())
         carried.grow(bought.itemStack().getCount());
      else
         carried = bought.itemStack();

      tradingMenu.setCarried(carried);
      // todo: drop loose coins

      context.reply(
            new TradeSlotUpdated(packet.sequence, packet.slot, tradeItem.stock(), vendorCurrency.availableCurrency()));
   }
}
