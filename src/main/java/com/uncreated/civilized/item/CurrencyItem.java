package com.uncreated.civilized.item;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.neoforge.registration.ItemRegistry;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
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

      tooltipComponents.add(amountTranslation(stack.getCount() * unitValue).withColor(Colors.TEXT_LIGHT_MUTED));
   }

   /** An amount of currency, e.g. "1 coin" or "24 coins". */
   public static MutableComponent amountTranslation(int currencyValue) {
      if (currencyValue == 1)
         return Component.translatable("item.civilized.coin.count.singular", currencyValue);
      return Component.translatable("item.civilized.coin.count", currencyValue);
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

   public static int credit(List<Container> creditorContainers, int amount) {
      List<ItemStack> coins = CurrencyItem.credit(amount);

      List<ItemStack> coinsFailedToTransfer = new LinkedList<>();
      for (ItemStack coin : coins) {
         // todo: need alternative add method for player inventory, otherwise it can insert into armor slots
         ItemStack remainder = ContainerHelper.addItemNicely(creditorContainers, coin);
         if (!remainder.isEmpty())
            coinsFailedToTransfer.add(remainder);
      }

      return CurrencyItem.countCoins(coinsFailedToTransfer);
   }

   public static List<ItemStack> credit(int amount) {
      List<ItemStack> payout = new ArrayList<>();
      int remainingAmount = amount;
      CurrencyItem currentDenomination = CurrencyItem.maxDenomination();
      do {
         int unitValue = currentDenomination.getUnitValue();
         int necessaryStackSize = remainingAmount / unitValue;
         necessaryStackSize = Math.clamp(necessaryStackSize, 0, currentDenomination.getDefaultMaxStackSize());
         if (necessaryStackSize > 0) {
            payout.add(new ItemStack(currentDenomination, necessaryStackSize));
            remainingAmount -= necessaryStackSize * unitValue;
         }
         if (remainingAmount < unitValue)
            currentDenomination = CurrencyItem.getNextSmallerDenomination(currentDenomination).orElse(null);
      } while (remainingAmount > 0 && currentDenomination != null);

      return payout;
   }

   public static int debit(List<Container> containers, int quotaCurrencyValue) {
      if (quotaCurrencyValue <= 0)
         return 0;

      CurrencyItem denomination = minDenomination();
      int taken = 0;
      takeCoins: do {
         for (Container container : containers) {
            for (int i = 0; i < container.getContainerSize(); i++) {

               ItemStack item = container.getItem(i);

               if (!(item.getItem() instanceof CurrencyItem d && d.unitValue == denomination.unitValue))
                  continue;

               int valueStillMissing = quotaCurrencyValue - taken;

               // the number of coins of this denomination we'd need to take in order satisfy the remaining debit
               // quota.

               // Why the funny math? We have to ceil the division result (i.e. add 1 to the result if there's a
               // remainder) because any
               // 'remainder' means that there was a gap that smaller coins couldn't cover that we need to now cover
               // by adding 1 larger coin + paying out the excess in change.
               int requiredCoinCount = Math.ceilDiv(valueStillMissing, denomination.unitValue);

               // we can't take more than what is in the stack already
               int ableToTakeCount = Math.min(requiredCoinCount, item.getCount());

               // finally, take from this stack
               item.shrink(ableToTakeCount);
               int takenStackValue = ableToTakeCount * denomination.unitValue;
               taken += takenStackValue;

               if (taken >= quotaCurrencyValue)
                  break takeCoins;
            }
         }

         denomination = CurrencyItem.getNextLargerDenomination(denomination).orElse(null);
      } while (denomination != null);

      int change = taken - quotaCurrencyValue;
      if (change > 0) {
         credit(containers, change);
         return taken - change;
      }

      return taken;
   }

   public static int countCoins(List<ItemStack> coinStacks) {
      return coinStacks.stream().mapToInt(i -> {
         if (i.getItem() instanceof CurrencyItem currencyItem)
            return i.getCount() * currencyItem.unitValue;

         return 0;
      }).sum();
   }

   public static int countCurrency(Container container) {
      AggregateItemStack coins = ContainerHelper.countItems(container, i -> i.getItem() instanceof CurrencyItem);
      return CurrencyItem.countCoins(coins.getItemStacks());
   }

   public static int countCurrency(List<Container> containers) {
      int sum = 0;
      for (Container container : containers) {
         sum += countCurrency(container);
      }
      return sum;
   }
}
