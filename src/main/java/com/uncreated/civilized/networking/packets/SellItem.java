package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.core.trading.ItemTraded;
import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.ui.menu.trading.TradingMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SellItem(int sequence, int slot) implements CustomPacketPayload {

   public static final Type<SellItem> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "sell_item"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SellItem> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               SellItem::sequence,
               ByteBufCodecs.VAR_INT,
               SellItem::slot,
               SellItem::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveSellItem(SellItem packet, IPayloadContext context) {

      if (!(context.player().containerMenu instanceof TradingMenu tradingMenu))
         return;

      TradeItem tradeItem = tradingMenu.lookUpSlot(packet.slot);
      if (tradeItem == null)
         return;

      if (tradeItem.item().isEmpty())
         return;

      ItemStack toSell = tradingMenu.getCarried();
      // is it correct to use isSameItemSameComponents? what are the implications?
      if (toSell.isEmpty() || !ItemStack.isSameItemSameComponents(toSell, tradeItem.item()))
         return;

      Container seller = tradingMenu.getCustomer();

      ItemTraded sold = tradeItem.sellToVendor(toSell, seller, tradingMenu.getAvailableVendorCurrency());

      tradingMenu.setAvailableVendorCurrency(sold.newVendorAvailableCurrency());
      tradingMenu.setCarried(sold.itemStack());
      // todo: drop loose coins

      context.reply(
            new TradeSlotUpdated(
                  packet.sequence,
                  packet.slot,
                  tradeItem.stock(),
                  tradingMenu.getAvailableVendorCurrency()));
   }
}
