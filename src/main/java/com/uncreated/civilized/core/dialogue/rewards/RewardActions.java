package com.uncreated.civilized.core.dialogue.rewards;

import java.util.List;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.item.CurrencyItem;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class RewardActions {

   public static void rewardItems(DialogueContext context, ItemStack itemStack) {
      PacketDistributor.sendToServer(new GiveItemsToPlayer(List.of(itemStack)));
   }

   public static void rewardItems(DialogueContext context, List<ItemStack> itemStack) {
      PacketDistributor.sendToServer(new GiveItemsToPlayer(itemStack));
   }

   public static void rewardCurrency(DialogueContext context, int amount) {
      PacketDistributor.sendToServer(new GiveItemsToPlayer(CurrencyItem.credit(amount)));
   }

   // adding items to player's inventory on the client doesn't work (items disappear) so have to tell the server to do
   // it.
   public static void serverGiveItemsToPlayer(GiveItemsToPlayer packet, IPayloadContext context) {
      packet.getItemStacks().forEach(itemStack -> {
         if (itemStack.isEmpty())
            return;

         if (!context.player().getInventory().add(itemStack))
            context.player().drop(itemStack, false);
      });
   }
}
