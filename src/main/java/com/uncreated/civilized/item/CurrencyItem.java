package com.uncreated.civilized.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.neoforge.registration.ItemRegistry;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CurrencyItem extends Item {

   @Getter
   private final int unitValue;

   public CurrencyItem(Properties properties, int unitValue) {
      super(properties);
      this.unitValue = unitValue;
   }

   @Override
   public Component getName(ItemStack stack) {
      return ((MutableComponent) stack.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY))
            .withColor(Colors.COIN);
   }

   @Override
   public void appendHoverText(
         ItemStack stack,
         TooltipContext context,
         List<Component> tooltipComponents,
         TooltipFlag tooltipFlag) {

      int currencyValue = stack.getCount() * unitValue;
      MutableComponent component;
      if (currencyValue == 1)
         component = Component.translatable("item.civilized.coin.count.singular", currencyValue);
      else
         component = Component.translatable("item.civilized.coin.count", currencyValue);

      tooltipComponents.add(component.withColor(Colors.TEXT_LIGHT_MUTED));
   }

   @Override
   public boolean overrideStackedOnOther(ItemStack payer, Slot payeeSlot, ClickAction clickAction, Player player) {

      if (clickAction != ClickAction.PRIMARY)
         return super.overrideStackedOnOther(payer, payeeSlot, clickAction, player);

      if (!(payer.getItem() instanceof CurrencyItem payerDenomination))
         return super.overrideStackedOnOther(payer, payeeSlot, clickAction, player);

      ItemStack payee = payeeSlot.getItem();
      if (!(payee.getItem() instanceof CurrencyItem payeeDenomination))
         return super.overrideStackedOnOther(payer, payeeSlot, clickAction, player);

      if (payerDenomination.unitValue == payeeDenomination.unitValue)
         return super.overrideStackedOnOther(payer, payeeSlot, clickAction, player);

      if (payerDenomination.unitValue < payeeDenomination.unitValue) {
         while (payer.getCount() * payerDenomination.unitValue >= payeeDenomination.unitValue
               && payee.getCount() < payee.getMaxStackSize()) {
            payer.shrink(payeeDenomination.unitValue);
            payee.grow(1);
         }
      } else {
         while (payee.getCount() + payerDenomination.unitValue <= payee.getMaxStackSize() && !payer.isEmpty()) {
            payer.shrink(1);
            payee.grow(payerDenomination.unitValue);
         }
      }

      return true;
   }

   @Override
   public boolean overrideOtherStackedOnMe(
         ItemStack small,
         ItemStack other,
         Slot otherSlot,
         ClickAction action,
         Player player,
         SlotAccess hoveringAccess) {

      if (action != ClickAction.SECONDARY)
         return super.overrideOtherStackedOnMe(small, other, otherSlot, action, player, hoveringAccess);

      if (!(other.getItem() instanceof AirItem))
         return super.overrideOtherStackedOnMe(small, other, otherSlot, action, player, hoveringAccess);

      if (!(small.getItem() instanceof CurrencyItem smallDenomination))
         return super.overrideOtherStackedOnMe(small, other, otherSlot, action, player, hoveringAccess);

      Optional<CurrencyItem> largerDenomination = getNextLargerDenomination(smallDenomination);
      if (largerDenomination.isEmpty())
         return super.overrideOtherStackedOnMe(small, other, otherSlot, action, player, hoveringAccess);

      ItemStack large = new ItemStack(ItemRegistry.COIN_STACK.get(), 0);
      while (small.getCount() >= largerDenomination.get().unitValue) {
         large.grow(1);
         small.shrink(largerDenomination.get().unitValue);
      }

      if (!large.isEmpty()) {
         otherSlot.set(small);
         hoveringAccess.set(large);
         return true;
      }

      return super.overrideOtherStackedOnMe(small, other, otherSlot, action, player, hoveringAccess);
   }

   public static Optional<CurrencyItem> getNextLargerDenomination(CurrencyItem item) {
      if (item.equals(ItemRegistry.COIN.get()))
         return Optional.of((CurrencyItem) ItemRegistry.COIN_STACK.get());

      return Optional.empty();
   }

   public static Optional<CurrencyItem> getNextSmallerDenomination(CurrencyItem item) {
      if (item.equals(ItemRegistry.COIN_STACK.get()))
         return Optional.of((CurrencyItem) ItemRegistry.COIN.get());

      return Optional.empty();
   }

   public static CurrencyItem getLargestFittingDenominationForAmount(int amount) {
      CurrencyItem currentDenomination = maxDenomination();
      while (amount < currentDenomination.unitValue) {
         Optional<CurrencyItem> nextSmallerDenomination = getNextSmallerDenomination(currentDenomination);
         if (nextSmallerDenomination.isEmpty())
            break;

         currentDenomination = nextSmallerDenomination.get();
      }
      return currentDenomination;
   }

   public static CurrencyItem maxDenomination() {
      return (CurrencyItem) ItemRegistry.COIN_STACK.get();
   }

   public static CurrencyItem minDenomination() {
      return (CurrencyItem) ItemRegistry.COIN.get();
   }

   public static List<ItemStack> payoutIntoItemStacks(int amount) {

      List<ItemStack> payoutItems = new ArrayList<>();

      CurrencyItem currentDenomination = CurrencyItem.getLargestFittingDenominationForAmount(amount);
      ItemStack currentStack = new ItemStack(currentDenomination, 0);

      while (amount >= currentDenomination.getUnitValue()) {

         amount -= currentDenomination.getUnitValue();
         currentStack.grow(1);

         if (amount < currentDenomination.getUnitValue()) {
            payoutItems.add(currentStack);

            Optional<CurrencyItem> nextSmallerDenomination =
                  CurrencyItem.getNextSmallerDenomination(currentDenomination);

            if (nextSmallerDenomination.isEmpty())
               break;

            currentDenomination = nextSmallerDenomination.get();
            currentStack = new ItemStack(currentDenomination, 0);
         }
      }
      return payoutItems;
   }
}
