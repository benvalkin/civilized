package com.uncreated.civilized.ui.menu.villager;

import java.util.List;
import java.util.function.BooleanSupplier;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.settlement.permission.ClientSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissionStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;
import com.uncreated.civilized.ui.menu.container.TogglableSlot;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// it was decided to make the villager screen a menu because some tabs (e.g. equipment) need the player's inventory.

/**
 * Menu behind {@link VillagerScreen}.
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
   public static final int SLOT_COLUMN_WIDTH = 9 * 18;

   public static final List<CarriedEquipmentSlot> EQUIPMENT_SLOTS = Loadout.SUPPORTED_SLOTS;
   public static final int EQUIPMENT_Y = 40;

   // the inventory sits at the bottom of the content panel
   private static final int INVENTORY_Y = CONTENT_HEIGHT - 8 - 76;
   private static final int INVENTORY_SLOT_COUNT = 36;

   @Setter
   private boolean playerInventoryVisible;
   @Setter
   private boolean equipmentVisible;

   private final CivilizedVillager villager;
   private final InteractionHand interactedHand;

   // client constructor
   public VillagerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraDataFromServer) {
      this(
            containerId,
            playerInventory,
            (CivilizedVillager) playerInventory.player.level().getEntity(extraDataFromServer.readVarInt()),
            extraDataFromServer.readEnum(InteractionHand.class),
            // client slots are filled via the serverside menu syncing them
            new SimpleContainer(EQUIPMENT_SLOTS.size()),
            ClientSettlementPermissionStore.INSTANCE);
   }

   public VillagerMenu(
         int containerId,
         Inventory playerInventory,
         CivilizedVillager villager,
         InteractionHand interactedHand,
         Container villagerEquipment,
         SettlementPermissionStore permissionStore) {
      super(GuiRegistry.VILLAGER_MENU.get(), containerId);
      this.villager = villager;
      this.interactedHand = interactedHand;

      addPlayerInventorySlots(playerInventory);
      Player player = playerInventory.player;
      addEquipmentSlots(
            villagerEquipment,
            () -> shouldShowSoldierEquipmentTab(permissionStore, player, villager.getInfo()));
   }

   public static boolean shouldShowSoldierEquipmentTab(
         SettlementPermissionStore permissionStore,
         Player player,
         VillagerInfo villager) {
      if (!villager.getOccupation().is(VillagerOccupations.SOLDIER) || villager.getSettlementId() == null)
         return false;

      return permissionStore.find(villager.getSettlementId())
            .map(permissions -> permissions.hasEditEquipmentPermission(player.getUUID()))
            .orElse(false);
   }

   private void addPlayerInventorySlots(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            addSlot(
                  new TogglableSlot(
                        playerInventory,
                        column + (row + 1) * 9,
                        column * 18,
                        INVENTORY_Y + row * 18,
                        this::isPlayerInventoryVisible));
         }
      }

      for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++)
         addSlot(
               new TogglableSlot(
                     playerInventory,
                     hotbarSlot,
                     hotbarSlot * 18,
                     INVENTORY_Y + 58,
                     this::isPlayerInventoryVisible));
   }

   private void addEquipmentSlots(Container equipment, BooleanSupplier canEdit) {
      for (int i = 0; i < EQUIPMENT_SLOTS.size(); i++)
         addSlot(
               new EquipmentMenuSlot(
                     equipment,
                     i,
                     i * 18,
                     EQUIPMENT_Y,
                     this::isEquipmentVisible,
                     villager,
                     EQUIPMENT_SLOTS.get(i),
                     canEdit));
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

   /** Shift-clicking moves items between the player's inventory and the villager's equipment. */
   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = slots.get(index);
      if (!slot.hasItem())
         return ItemStack.EMPTY;

      ItemStack stack = slot.getItem();
      ItemStack original = stack.copy();
      boolean isInventorySlot = index < INVENTORY_SLOT_COUNT;
      if (isInventorySlot) {
         if (!moveItemStackTo(stack, INVENTORY_SLOT_COUNT, slots.size(), false))
            return ItemStack.EMPTY;
      } else if (!moveItemStackTo(stack, 0, INVENTORY_SLOT_COUNT, true))
         return ItemStack.EMPTY;

      if (stack.isEmpty())
         slot.setByPlayer(ItemStack.EMPTY);
      else
         slot.setChanged();

      return original;
   }
}
