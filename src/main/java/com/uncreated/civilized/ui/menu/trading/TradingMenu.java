package com.uncreated.civilized.ui.menu.trading;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.trading.CurrencyStock;
import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;

import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class TradingMenu extends AbstractContainerMenu {

   private static final int INVENTORY_X = 87;
   private static final int INVENTORY_Y = 105;

   /** The trades sent to the client when the menu opens, which it has no other way of knowing. */
   public static final StreamCodec<RegistryFriendlyByteBuf, List<TradeItem>> TRADES_STREAM_CODEC =
         TradeItem.STREAM_CODEC.apply(ByteBufCodecs.list());

   @Nullable
   private CivilizedVillager vendor;
   @Getter
   private final CurrencyStock vendorCurrency;
   @Getter
   private final List<TradeItem> tradeItems;
   @Getter
   private final Container customer;

   // client constructor
   public TradingMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraDataFromServer) {
      this(
            containerId,
            playerInventory,
            null,
            readAvailableVendorCurrency(extraDataFromServer),
            readVendorTrades(extraDataFromServer),
            playerInventory);
   }

   private static CurrencyStock readAvailableVendorCurrency(RegistryFriendlyByteBuf extraDataFromServer) {
      return new CurrencyStock(extraDataFromServer.readInt());
   }

   private static List<TradeItem> readVendorTrades(RegistryFriendlyByteBuf extraDataFromServer) {
      return TRADES_STREAM_CODEC.decode(extraDataFromServer);
   }

   public TradingMenu(
         int containerId,
         Inventory playerInventory,
         CivilizedVillager vendor,
         CurrencyStock vendorCurrency,
         List<TradeItem> tradeItems,
         Container customerStock) {
      super(GuiRegistry.TRADING_MENU.get(), containerId);
      this.vendor = vendor;
      this.vendorCurrency = vendorCurrency;
      this.tradeItems = tradeItems;

      this.customer = customerStock;

      addStandardInventorySlots(playerInventory, INVENTORY_X, INVENTORY_Y);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int i) {
      return ItemStack.EMPTY;
   }

   @Override
   public boolean stillValid(Player player) {
      return vendor == null || !vendor.isAlive() || player.distanceToSqr(vendor) <= 8 * 8;
   }

   public @Nullable TradeItem lookUpSlot(int slot) {
      if (slot >= 0 && slot < this.tradeItems.size())
         return tradeItems.get(slot);

      return null;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      if (vendor != null && !player.level().isClientSide())
         vendor.stopSpeakingToPlayer();
   }

}
