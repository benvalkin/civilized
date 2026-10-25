package com.uncreated.civilized.ui.menu.villager;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;
import com.uncreated.civilized.ui.menu.container.TogglableSlot;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// it was decided to make the villager screen a menu because some tabs (e.g. equipment) need the player's inventory.

/**
 * The menu behind the villager screen. Talking to a villager opens it, so the villager stands still for as long as it is
 * open.
 */
@Getter
public class VillagerMenu extends AbstractContainerMenu {

   /** How far the player can get from the villager before the screen closes itself, in blocks. */
   private static final double MAX_DISTANCE_FROM_VILLAGER = 8;

   /**
    * Height of the screen's content panel, to the right of the info card. It's fixed, because slot positions are set
    * when the menu is created.
    */
   public static final int CONTENT_HEIGHT = 160;

   // relative to the content panel. The inventory sits at its bottom.
   private static final int INVENTORY_X = 8;
   private static final int INVENTORY_Y = CONTENT_HEIGHT - 8 - 76;

   @Setter
   private boolean playerInventoryVisible;

   private final CivilizedVillager villager;
   /** The hand the player talked to the villager with. Dialogue can depend on what's held in it. */
   private final InteractionHand hand;

   // client constructor
   public VillagerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraDataFromServer) {
      this(
            containerId,
            playerInventory,
            (CivilizedVillager) playerInventory.player.level().getEntity(extraDataFromServer.readVarInt()),
            extraDataFromServer.readEnum(InteractionHand.class));
   }

   public VillagerMenu(int containerId, Inventory playerInventory, CivilizedVillager villager, InteractionHand hand) {
      super(GuiRegistry.VILLAGER_MENU.get(), containerId);
      this.villager = villager;
      this.hand = hand;

      addPlayerInventorySlots(playerInventory);
   }

   private void addPlayerInventorySlots(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            addSlot(
                  new TogglableSlot(
                        playerInventory,
                        column + (row + 1) * 9,
                        INVENTORY_X + column * 18,
                        INVENTORY_Y + row * 18,
                        this::isPlayerInventoryVisible));
         }
      }

      for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++)
         addSlot(
               new TogglableSlot(
                     playerInventory,
                     hotbarSlot,
                     INVENTORY_X + hotbarSlot * 18,
                     INVENTORY_Y + 58,
                     this::isPlayerInventoryVisible));
   }

   @Override
   public boolean stillValid(Player player) {
      return villager.isAlive()
            && player.distanceToSqr(villager) <= MAX_DISTANCE_FROM_VILLAGER * MAX_DISTANCE_FROM_VILLAGER;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      if (!player.level().isClientSide())
         villager.stopSpeakingToPlayer();
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      // Quick moving stacks is not supported until a tab has slots of its own to move them to.
      return ItemStack.EMPTY;
   }
}
