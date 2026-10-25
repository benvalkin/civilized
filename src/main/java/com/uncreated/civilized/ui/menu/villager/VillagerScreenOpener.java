package com.uncreated.civilized.ui.menu.villager;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class VillagerScreenOpener {

   public static void open(ServerPlayer player, CivilizedVillager villager, InteractionHand hand) {

      // Opening the menu done next tick.
      // The reason for this is that closing a menu replaces the player's open menu with their
      // inventory afterwards, which would throw this one away if it was opened while a menu was being closed
      player.server.execute(() -> openVillagerMenu(player, villager, hand));
   }

   private static void openVillagerMenu(ServerPlayer player, CivilizedVillager villager, InteractionHand hand) {
      player.openMenu(new MenuProvider() {

         @Override
         public Component getDisplayName() {
            return Component.literal(villager.getInfo().getFullName());
         }

         @Override
         public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new VillagerMenu(containerId, inventory, villager, hand);
         }
      }, buffer -> writeMenuData(buffer, villager, hand)).ifPresent(containerId -> villager.goSpeakToPlayer(player));
   }

   private static void writeMenuData(RegistryFriendlyByteBuf buffer, CivilizedVillager villager, InteractionHand hand) {
      buffer.writeVarInt(villager.getId());
      buffer.writeEnum(hand);
   }
}
