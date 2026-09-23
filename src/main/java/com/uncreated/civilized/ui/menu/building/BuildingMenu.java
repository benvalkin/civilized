package com.uncreated.civilized.ui.menu.building;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;
import com.uncreated.civilized.ui.context.BuildingScreenContext;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// it was decided to make building screens a menu because some tabs (specifically editing recipes) require access to the player's inventory so they can drag items.

/**
 * The menu behind every building screen. It does not hold any slots of its own.
 */
@Getter
public class BuildingMenu extends AbstractContainerMenu {

   /** How far the player can get from a building before its screen closes itself, in blocks. */
   private static final double MAX_DISTANCE_FROM_BUILDING = 64;

   private static final int INVENTORY_X = 87;
   private static final int INVENTORY_Y = 105;

   @Setter
   private boolean playerInventoryVisible;

   private final Building building;
   private final Settlement settlement;
   private final BuildingScreenContext context;

   // client constructor
   public BuildingMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraDataFromServer) {
      this(
            containerId,
            playerInventory,
            ClientBuildingStore.INSTANCE.get(extraDataFromServer.readUUID()),
            ClientSettlementsStore.INSTANCE.get(extraDataFromServer.readUUID()),
            extraDataFromServer.readNbt());
   }

   public BuildingMenu(
         int containerId,
         Inventory playerInventory,
         Building building,
         Settlement settlement,
         CompoundTag additionalData) {
      super(GuiRegistry.BUILDING_MENU.get(), containerId);

      this.building = building;
      this.settlement = settlement;
      this.context =
            new BuildingScreenContext(building, settlement, additionalData, playerInventory.player.registryAccess());

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
                        INVENTORY_Y + row * 18));
         }
      }

      for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++)
         addSlot(new TogglableSlot(playerInventory, hotbarSlot, INVENTORY_X + hotbarSlot * 18, INVENTORY_Y + 58));
   }

   private class TogglableSlot extends Slot {

      public TogglableSlot(Inventory playerInventory, int slotIndex, int x, int y) {
         super(playerInventory, slotIndex, x, y);
      }

      @Override
      public boolean isActive() {
         return playerInventoryVisible;
      }
   }

   @Override
   public boolean stillValid(Player player) {
      return player.level().dimension().equals(building.getDimension()) && building.getBlockPos()
            .distToCenterSqr(player.position()) <= MAX_DISTANCE_FROM_BUILDING * MAX_DISTANCE_FROM_BUILDING;
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      // Quick moving stacks is not supported in the Building Menu. To transfer to whatever upstream slots are
      // available, you must click on them manually.
      // This implementation should only change if there is a need to support actually moving items to some container
      // via the Building Menu.
      return ItemStack.EMPTY;
   }
}
